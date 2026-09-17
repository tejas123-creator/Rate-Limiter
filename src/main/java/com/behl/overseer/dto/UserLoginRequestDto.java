package com.behl.overseer.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@Schema(title = "UserLoginRequest", accessMode = Schema.AccessMode.WRITE_ONLY)
public class UserLoginRequestDto {

	@NotBlank(message = "email-id must not be empty")
	@Email(message = "email-id must be of valid format")
	@Schema(requiredMode = RequiredMode.REQUIRED, example = "tejas.zunjepatil@example.com", description = "email-id associated with user account already created in the system")
	// Account email used to find the user during login.
	private String emailId;

	@NotBlank(message = "password must not be empty")
	@Schema(requiredMode = RequiredMode.REQUIRED, example = "somethingSecure", description = "password corresponding to provided email-id")
	// Password compared with the stored BCrypt hash during login.
	private String password;

}
