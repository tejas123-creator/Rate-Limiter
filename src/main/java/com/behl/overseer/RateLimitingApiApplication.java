package com.behl.overseer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the rate-limiting API.
 *
 * <p>{@code @SpringBootApplication} tells Spring Boot to load configuration,
 * discover application components in this package and its child packages, and
 * configure the application automatically.</p>
 */
@SpringBootApplication
public class RateLimitingApiApplication {

	/**
	 * Starts the Spring Boot application.
	 *
	 * <p>Spring Boot reads {@code application.yml}, creates the configured
	 * components (controllers, services, repositories, and filters), connects to
	 * external services such as MySQL and Redis, and starts the web server.</p>
	 *
	 * @param args optional command-line arguments passed when the application starts
	 */
	public static void main(String[] args) {
		// Use this class as the root configuration class and start the application.
		SpringApplication.run(RateLimitingApiApplication.class, args);
	}

}
