package com.crimesphere.backend.exception;

/**
 * Thrown when an evidence file with the same SHA-256 hash already exists for a case.
 * Results in 409 Conflict response.
 */
public class DuplicateEvidenceException extends RuntimeException {
    public DuplicateEvidenceException(String message) {
        super(message);
    }
}
