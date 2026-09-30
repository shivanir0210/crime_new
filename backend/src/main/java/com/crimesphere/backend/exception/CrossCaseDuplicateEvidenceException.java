package com.crimesphere.backend.exception;

/**
 * Thrown when an evidence file SHA-256 hash exists in a different case.
 * Prompts investigator confirmation to allow cross-case evidence association.
 */
public class CrossCaseDuplicateEvidenceException extends RuntimeException {

    private final String caseNumber;

    public CrossCaseDuplicateEvidenceException(String caseNumber, String message) {
        super(message);
        this.caseNumber = caseNumber;
    }

    public String getCaseNumber() {
        return caseNumber;
    }
}
