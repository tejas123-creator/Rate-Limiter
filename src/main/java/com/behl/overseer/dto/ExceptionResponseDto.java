package com.behl.overseer.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(title = "Error", accessMode = Schema.AccessMode.READ_ONLY)
@JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ExceptionResponseDto<T> {

	// HTTP status represented in the JSON error response.
	private String status;
	// Endpoint-specific explanation of the error; generic so it can have any JSON-compatible type.
	private T description;

}
