package com.behl.overseer.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(OpenApiConfigurationProperties.class)
public class OpenApiConfiguration {

	// Supplies title, version, and description read from application.yml.
	private final OpenApiConfigurationProperties openApiConfigurationProperties;
	
	// Identifier Swagger UI uses when it displays the authorization dialog.
	private static final String BEARER_AUTH_COMPONENT_NAME = "Bearer Authentication";
	// HTTP authentication scheme that tells Swagger to send `Authorization: Bearer <token>`.
	private static final String BEARER_AUTH_SCHEME = "Bearer";

	@Bean
	public OpenAPI openApi() {
		// Read the documentation metadata rather than hard-coding it in Java.
		final var properties = openApiConfigurationProperties.getOpenApi();
		final var info = new Info()
				.version(properties.getApiVersion())
				.title(properties.getTitle())
				.description(properties.getDescription());

		// Register the bearer-token scheme and apply it to documented operations.
		return new OpenAPI()
			    .info(info)
			    .components(new Components()
			        .addSecuritySchemes(BEARER_AUTH_COMPONENT_NAME,
			            new SecurityScheme()
			                .type(SecurityScheme.Type.HTTP)
			                .scheme(BEARER_AUTH_SCHEME)))
			    .addSecurityItem(new SecurityRequirement()
			    			.addList(BEARER_AUTH_COMPONENT_NAME));
	}
	
}
