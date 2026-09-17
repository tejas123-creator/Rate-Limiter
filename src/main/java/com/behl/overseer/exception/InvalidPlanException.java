package com.behl.overseer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import lombok.NonNull;

/** Maps a requested plan ID that is absent from MySQL to HTTP 404 Not Found. */
public class InvalidPlanException extends ResponseStatusException {

	// Required by Java serialization for exception classes.
	private static final long serialVersionUID = 4506094675559975006L;

	public InvalidPlanException(@NonNull final String reason) {
		// Preserve the caller-specific reason while consistently returning HTTP 404.
		super(HttpStatus.NOT_FOUND, reason);
	}

}
