package com.behl.overseer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Maps an unknown account or incorrect password to the same safe HTTP 401 response. */
public class InvalidLoginCredentialsException extends ResponseStatusException {

	// Required by Java serialization for exception classes.
	private static final long serialVersionUID = 7439642984069939024L;
	// Deliberately generic so login does not reveal whether the email exists.
	private static final String DEFAULT_MESSAGE = "Invalid login credentials provided";

	public InvalidLoginCredentialsException() {
		// Spring uses this status when it formats the exception response.
		super(HttpStatus.UNAUTHORIZED, DEFAULT_MESSAGE);
	}

}
