package com.behl.overseer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import lombok.NonNull;

/** Maps an attempted duplicate account creation to HTTP 409 Conflict. */
public class AccountAlreadyExistsException extends ResponseStatusException {

	// Required by Java serialization for exception classes.
	private static final long serialVersionUID = 7439642984069939024L;

	public AccountAlreadyExistsException(@NonNull final String reason) {
		// Spring uses this status and message when ExceptionResponseHandler formats the response.
		super(HttpStatus.CONFLICT, reason);
	}

}
