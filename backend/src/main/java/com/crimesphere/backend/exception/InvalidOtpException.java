package com.crimesphere.backend.exception;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * Exception thrown when an OTP verification or challenge validation fails.
 * Subclasses BadCredentialsException for backward compatibility while allowing
 * specific HTTP 400 Bad Request error mapping.
 */
public class InvalidOtpException extends BadCredentialsException {

    public InvalidOtpException(String message) {
        super(message);
    }
}
