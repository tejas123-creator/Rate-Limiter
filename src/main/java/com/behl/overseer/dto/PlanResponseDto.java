package com.behl.overseer.dto;

import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
@JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@Schema(title = "Plan", accessMode = Schema.AccessMode.READ_ONLY)
public class PlanResponseDto {

	// Unique ID clients send when selecting or changing a plan.
	private UUID id;
	// Display name, such as FREE, BUSINESS, or PROFESSIONAL.
	private String name;
	// Maximum protected-API requests allowed for this plan each hour.
	private Integer limitPerHour;

}
