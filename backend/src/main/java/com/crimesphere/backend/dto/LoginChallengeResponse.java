package com.crimesphere.backend.dto;

import java.util.UUID;

public class LoginChallengeResponse {
    private boolean requiresOtp;
    private UUID challengeId;
    private String maskedEmail;
    private String message;

    public LoginChallengeResponse() {
    }

    public LoginChallengeResponse(boolean requiresOtp, UUID challengeId, String maskedEmail, String message) {
        this.requiresOtp = requiresOtp;
        this.challengeId = challengeId;
        this.maskedEmail = maskedEmail;
        this.message = message;
    }

    public boolean isRequiresOtp() {
        return requiresOtp;
    }

    public void setRequiresOtp(boolean requiresOtp) {
        this.requiresOtp = requiresOtp;
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
