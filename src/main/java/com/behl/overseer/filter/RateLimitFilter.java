package com.behl.overseer.filter;

import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.behl.overseer.configuration.BypassRateLimit;
import com.behl.overseer.dto.ExceptionResponseDto;
import com.behl.overseer.service.RateLimitingService;
import com.behl.overseer.utility.ApiEndpointSecurityInspector;
import com.behl.overseer.utility.AuthenticatedUserIdProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * RateLimitFilter is a custom filter registered with the spring security filter
 * chain and works in conjunction with the security configuration, as defined in
 * {@link com.behl.overseer.configuration.SecurityConfiguration}. As per
 * established configuration, this filter is executed after evaluation of
 * {@link com.behl.overseer.filter.JwtAuthenticationFilter}
 * 
 * This filter is responsible for enforcing rate limit on secured
 * API endpoint(s) corresponding to the user's current plan. It intercepts 
 * incoming HTTP  requests and evaluates whether the user has exhausted the 
 * limit enforced.
 * If the limit is exceeded, an error response indicating that the
 * request limit linked to the user's current plan has been exhausted
 * is returned back to the client.
 * 
 * This filter is only executed when a secure API endpoint in invoked, and is skipped
 * if the incoming request is destined to a non-secured public API endpoint.
 * 
 * Additionally, the rate limit enforcement can be bypassed for specific private
 * API endpoints by annotating the corresponding controller methods with
 * {@link com.behl.overseer.configuration.BypassRateLimit} annotation.
 * 
 * @see com.behl.overseer.configuration.BypassRateLimit
 * @see com.behl.overseer.service.RateLimitingService
 * @see com.behl.overseer.utility.ApiEndpointSecurityInspector
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

	// Serializes the standard rate-limit error response to JSON.
	private final ObjectMapper objectMapper;
	// Retrieves and consumes the authenticated user's Bucket4j bucket in Redis.
	private final RateLimitingService rateLimitingService;
	// Maps the request URL to its controller method to inspect @BypassRateLimit.
	private final RequestMappingHandlerMapping requestHandlerMapping;
	// Supplies the user ID authenticated earlier by JwtAuthenticationFilter.
	private final AuthenticatedUserIdProvider authenticatedUserIdProvider;
	// Identifies public endpoints, which must not consume a rate-limit token.
	private final ApiEndpointSecurityInspector apiEndpointSecurityInspector;

	// Client-facing explanation sent when no quota remains.
	private static final String RATE_LIMIT_ERROR_MESSAGE = "API request limit linked to your current plan has been exhausted.";
	// HTTP status required for a rejected rate-limited request.
	private static final HttpStatus RATE_LIMIT_ERROR_STATUS = HttpStatus.TOO_MANY_REQUESTS;

	/**
	 * Runs after JWT authentication and consumes one token for every rate-limited
	 * private request. Requests with no remaining token receive HTTP 429 and do
	 * not reach the controller.
	 *
	 * @param request incoming request whose endpoint and authenticated user are inspected
	 * @param response response used for rate-limit headers or a 429 error
	 * @param filterChain remaining filters and the controller to call when allowed
	 */
	@Override
	@SneakyThrows
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
		// Public endpoints are deliberately excluded from user quota checks.
		final var unsecuredApiBeingInvoked = apiEndpointSecurityInspector.isUnsecureRequest(request);

		if (Boolean.FALSE.equals(unsecuredApiBeingInvoked) && authenticatedUserIdProvider.isAvailable()) {
			// Certain private endpoints, such as changing plan, explicitly bypass rate limiting.
			final var isRequestBypassed = isBypassed(request);

			if (Boolean.FALSE.equals(isRequestBypassed)) {
				// Each user has an independent Redis bucket, identified by their UUID.
				final var userId = authenticatedUserIdProvider.getUserId();
				final var bucket = rateLimitingService.getBucket(userId);
				// Attempt to spend one token and also obtain remaining-token/refill information.
				final var consumptionProbe = bucket.tryConsumeAndReturnRemaining(1);
				final var isConsumptionPassed = consumptionProbe.isConsumed();

				if (Boolean.FALSE.equals(isConsumptionPassed)) {
					// Stop now so an exhausted user cannot execute the controller method.
					setRateLimitErrorDetails(response, consumptionProbe);
					return;
				}

				// Help clients display or respect the remaining quota.
				final var remainingTokens = consumptionProbe.getRemainingTokens();
				response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remainingTokens));
			}
		}
		filterChain.doFilter(request, response);
	}

	/**
	 * Checks if the controller method corresponding to current request is annotated
	 * with {@link BypassRateLimit} annotation, indicating that rate limit
	 * enforcement should be bypassed.
	 *
	 * @param request HttpServletRequest representing the incoming HTTP request
	 * @return {@code true} if the request is to be bypassed, {@code false} otherwise
	 */
	@SneakyThrows
	private boolean isBypassed(HttpServletRequest request) {
		// Resolve the request to its controller method so annotations can be inspected at runtime.
		var handlerChain = requestHandlerMapping.getHandler(request);
		if (handlerChain != null && handlerChain.getHandler() instanceof HandlerMethod handlerMethod) {
			return handlerMethod.getMethod().isAnnotationPresent(BypassRateLimit.class);
		}
		return Boolean.FALSE;
	}

	/**
	 * Sets the rate limit error details in the HTTP response. This method is
	 * invoked when the user has exceeded their configured rate limit for API
	 * requests.
	 * 
	 * @param response instance of HttpServletResponse to which the rate limit error response will be set.
	 * @param consumptionProbe ConsumptionProbe object representing the rate limit consumption information.
	 */
	@SneakyThrows
	private void setRateLimitErrorDetails(HttpServletResponse response, final ConsumptionProbe consumptionProbe) {
		// Mark the request as rejected and return JSON, matching other project errors.
		response.setStatus(RATE_LIMIT_ERROR_STATUS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		// Convert Bucket4j's nanosecond refill time into a useful response header for clients.
		final var waitPeriod = TimeUnit.NANOSECONDS.toSeconds(consumptionProbe.getNanosToWaitForRefill());
		response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitPeriod));

		final var errorResponse = prepareErrorResponseBody();
		response.getWriter().write(errorResponse);
	}

	/**
	 * Returns a JSON representation of the rate limit exhaustion error response
	 * body.
	 */
	@SneakyThrows
	private String prepareErrorResponseBody() {
		// Build the common error DTO before serializing it to a JSON string.
		final var exceptionResponse = new ExceptionResponseDto<String>();
		exceptionResponse.setStatus(RATE_LIMIT_ERROR_STATUS.toString());
		exceptionResponse.setDescription(RATE_LIMIT_ERROR_MESSAGE);
		return objectMapper.writeValueAsString(exceptionResponse);
	}

}
