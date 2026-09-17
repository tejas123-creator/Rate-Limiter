package com.behl.overseer.configuration;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.behl.overseer.filter.JwtAuthenticationFilter;
import com.behl.overseer.filter.RateLimitFilter;
import com.behl.overseer.utility.ApiEndpointSecurityInspector;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * Configuration class responsible for defining and configuring the security
 * settings for the application. It sets up the following components and
 * features:
 * <ul>
 *   <li>Configuration of non-secured public API endpoints.</li>
 *   <li>Integration of custom JWT Auth filter into the security filter chain to
 *       ensure that all requests to private API endpoints pass through the filter
 *       for authentication verification.</li>
 *   <li>Integration of custom Rate limiting filter into the security filter chain
 *       to ensure private API endpoints are invoked by an authenticated user
 *       within their corresponding {@link com.behl.overseer.entity.Plan} </li>
 * </ul>
 *
 * @see com.behl.overseer.filter.JwtAuthenticationFilter
 * @see com.behl.overseer.filter.RateLimitFilter
 * @see com.behl.overseer.utility.ApiEndpointSecurityInspector
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

	// Rejects requests that have exhausted the authenticated user's API quota.
	private final RateLimitFilter rateLimitFilter;
	// Validates JWT access tokens and identifies the user making the request.
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	// Locates endpoints marked as public so that they can skip JWT authentication.
	private final ApiEndpointSecurityInspector apiEndpointSecurityInspector;
	
	/**
	 * Builds the Spring Security filter chain used for every incoming HTTP request.
	 *
	 * @param http Spring Security's builder for declaring the application's security rules
	 * @return the completed security filter chain
	 */
	@Bean
	@SneakyThrows
	public SecurityFilterChain configure(final HttpSecurity http)  {
		http
			// Permit browser clients, including Swagger UI, to call the API across origins.
			.cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()))
			// CSRF protection is unnecessary because this is a stateless token-based API.
			.csrf(csrfConfigurer -> csrfConfigurer.disable())
			// Do not create server-side login sessions; every request brings its own JWT.
			.sessionManagement(sessionConfigurer -> sessionConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authManager -> {
					authManager
						// GET endpoints annotated with @PublicEndpoint can be called without a token.
						.requestMatchers(HttpMethod.GET, apiEndpointSecurityInspector.getPublicGetEndpoints().toArray(String[]::new)).permitAll()
						// POST endpoints annotated with @PublicEndpoint can also be called without a token.
						.requestMatchers(HttpMethod.POST, apiEndpointSecurityInspector.getPublicPostEndpoints().toArray(String[]::new)).permitAll()
						// Every remaining endpoint requires a successfully authenticated user.
						.anyRequest().authenticated();
			})
			// Validate the JWT before Spring Security's standard username/password filter.
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			// Enforce rate limits only after the JWT filter has identified the user.
			.addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);

		return http.build();
	}
	
	/**
	 * Provides the password-hashing algorithm used when passwords are saved and
	 * verified. BCrypt is one-way, so the original password cannot be recovered.
	 *
	 * @return the shared password encoder used by the user service
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	/**
	 * Declares browser cross-origin (CORS) rules for every API path.
	 *
	 * @return a source that provides the application's CORS rules
	 */
	private CorsConfigurationSource corsConfigurationSource() {
		// Holds the cross-origin browser-access rules for this application.
		final var corsConfiguration = new CorsConfiguration();
		// This demo accepts calls from any origin; a production app should restrict this list.
		corsConfiguration.setAllowedOrigins(List.of("*"));
		// Allow the HTTP operations exposed by the API.
		corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		// Allow request headers needed by API clients, including the JWT Authorization header.
		corsConfiguration.setAllowedHeaders(List.of("Authorization", "Origin", "Content-Type", "Accept"));
		// Allow browser JavaScript to read these rate-limit response headers.
		corsConfiguration.setExposedHeaders(List.of("Content-Type", "X-Rate-Limit-Retry-After-Seconds", "X-Rate-Limit-Remaining"));

		// Apply the preceding rules to every endpoint path.
		final var corsConfigurationSource = new UrlBasedCorsConfigurationSource();
		corsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
		return corsConfigurationSource;
	}

}
