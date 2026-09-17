package com.behl.overseer.dto;

import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@Schema(title = "UserCreationRequest", accessMode = Schema.AccessMode.WRITE_ONLY)
public class UserCreationRequestDto {

	@NotBlank(message = "email-id must not be empty")
	@Email(message = "email-id must be of valid format")
	@Schema(requiredMode = RequiredMode.REQUIRED, description = "email-id of user", example = "tejas.zunjepatil@example.com")
	// Unique email address that identifies the new account.
	private String emailId;
	
	@NotBlank(message = "password must not be empty")
	@Schema(requiredMode = RequiredMode.REQUIRED, description = "secure password to enable user login", example = "somethingSecure")
	// Plain-text password supplied only during creation; the service hashes it before storage.
	private String password;
	
	@NotNull
	@Schema(requiredMode = RequiredMode.REQUIRED, description = "plan to be attached with new user record")
	// Plan selected for the new user, which determines their request quota.
	private UUID planId;

}
