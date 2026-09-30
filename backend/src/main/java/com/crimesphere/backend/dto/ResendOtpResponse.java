package com.crimesphere.backend.dto;

import java.util.UUID;

public class ResendOtpResponse {
    private boolean success;
    private UUID challengeId;
    private String maskedEmail;
    private String message;

    public ResendOtpResponse() {
    }

    public ResendOtpResponse(boolean success, UUID challengeId, String maskedEmail, String message) {
        this.success = success;
        this.challengeId = challengeId;
        this.maskedEmail = maskedEmail;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(UUID challengeId) {
        this.challengeId = challengeId;
    }

    public String getMaskedEmail() {
        return maskedEmail;
    }

    public void setMaskedEmail(String maskedEmail) {
        this.maskedEmail = maskedEmail;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
