package com.behl.overseer.filter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.behl.overseer.dto.ExceptionResponseDto;
import com.behl.overseer.utility.ApiEndpointSecurityInspector;
import com.behl.overseer.utility.JwtUtility;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * JwtAuthenticationFilter is a custom filter registered with the spring
 * security filter chain and works in conjunction with the security
 * configuration, as defined in {@link com.behl.overseer.configuration.SecurityConfiguration}. 
 * 
 * It is responsible for verifying the authenticity of incoming HTTP requests to
 * secured API endpoints by examining JWT token in the request header, verifying 
 * it's signature, expiration.
 * If authentication is successful, the filter populates the security context with
 * the user's unique identifier which can be referenced by the application later.
 * 
 * This filter is only executed when a secure API endpoint in invoked, and is skipped
 * if the incoming request is destined to a non-secured public API endpoint.
 *
 * @see com.behl.overseer.configuration.SecurityConfiguration
 * @see com.behl.overseer.utility.ApiEndpointSecurityInspector
 * @see com.behl.overseer.utility.JwtUtility
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	// Converts the structured authentication-error DTO into JSON for the HTTP response.
	private final ObjectMapper objectMapper;
	// Creates and validates JWTs, and extracts the authenticated user's ID from a token.
	private final JwtUtility jwtUtility;
	// Determines whether the current endpoint is public and therefore needs no JWT.
	private final ApiEndpointSecurityInspector apiEndpointSecurityInspector;
	
	// Name of the HTTP header that carries the client's JWT access token.
	private static final String AUTHORIZATION_HEADER = "Authorization";
	// Required prefix in the Authorization header: "Bearer <JWT>".
	private static final String BEARER_PREFIX = "Bearer ";
	// Safe, client-facing message returned when no usable token was supplied.
	private static final String MISSING_TOKEN_ERROR_MESSAGE = "Authentication failure: Token missing, invalid or expired";

	/**
	 * Runs once for each HTTP request and performs JWT authentication for private
	 * endpoints before the request reaches a controller.
	 *
	 * <p>A successful authentication stores the user's UUID in Spring Security's
	 * {@code SecurityContext}. Later code, such as the rate-limit filter and
	 * services, can retrieve this UUID without parsing the token again.</p>
	 *
	 * @param request the incoming HTTP request, including its Authorization header
	 * @param response the outgoing HTTP response; used to return a 401 error on failure
	 * @param filterChain the remaining security filters and, eventually, the controller
	 */
	@Override
	@SneakyThrows
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
		// Public endpoints, such as account creation and login, are allowed without a JWT.
		final var unsecuredApiBeingInvoked = apiEndpointSecurityInspector.isUnsecureRequest(request);
		
		if (Boolean.FALSE.equals(unsecuredApiBeingInvoked)) {
			// Read the token value supplied by the client in the Authorization HTTP header.
			final var authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
	
			if (StringUtils.isNotEmpty(authorizationHeader) && authorizationHeader.startsWith(BEARER_PREFIX) ) {
				// Remove "Bearer " so JwtUtility receives only the compact JWT string.
				final var token = authorizationHeader.replace(BEARER_PREFIX, StringUtils.EMPTY);
				
				// Verify the token and extract its user ID. Invalid or expired tokens cannot authenticate.
				final var userId = jwtUtility.getUserId(token);
				// Represent the authenticated user to Spring Security; no password or roles are needed here.
				final var authentication = new UsernamePasswordAuthenticationToken(userId, null, null);
				// Attach request metadata, such as the remote address, to the authentication record.
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				// Make the authenticated user available to the rest of this request's processing.
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} else {
				// Stop processing immediately: a private endpoint cannot be accessed without a Bearer token.
				setAuthErrorDetails(response);
				return;
			}
		}
		// Authentication succeeded, or the endpoint is public, so continue to the next filter/controller.
		filterChain.doFilter(request, response);
	}
	
	/**
	 * Builds and writes the standard 401 JSON response for a request without a
	 * usable Bearer token. This method prevents the request from reaching a
	 * protected controller.
	 * 
	 * @param response response that receives the HTTP status, content type, and JSON body
	 */
	@SneakyThrows
	private void setAuthErrorDetails(HttpServletResponse response) {
		// Inform the client that authentication is required or has failed.
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		// Tell the client that the error body is JSON.
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		// Serialize the project's standard error DTO before writing it to the response body.
		final var errorResponse = prepareErrorResponseBody();
		response.getWriter().write(errorResponse);
	}
	
	/**
	 * Creates the standard error object and serializes it to JSON so the client
	 * receives a consistent response shape.
	 *
	 * @return JSON containing the 401 status and authentication-failure description
	 */
	@SneakyThrows
	private String prepareErrorResponseBody() {
		// DTO used throughout the project to return structured error details.
		final var exceptionResponse = new ExceptionResponseDto<String>();
		// Store the HTTP status in the response body as well as on the HTTP response itself.
		exceptionResponse.setStatus(HttpStatus.UNAUTHORIZED.toString());
		// Avoid revealing whether a token was missing, malformed, expired, or forged.
		exceptionResponse.setDescription(MISSING_TOKEN_ERROR_MESSAGE);
		return objectMapper.writeValueAsString(exceptionResponse);
	}

}
