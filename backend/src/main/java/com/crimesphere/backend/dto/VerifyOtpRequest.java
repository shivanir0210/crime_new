package com.crimesphere.backend.dto;

import java.util.UUID;

public class VerifyOtpRequest {
    private UUID challengeId;
    private String otp;

    public VerifyOtpRequest() {
    }

    public VerifyOtpRequest(UUID challengeId, String otp) {
        this.challengeId = challengeId;
        this.otp = otp;
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(UUID challengeId) {
        this.challengeId = challengeId;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
