package com.crimesphere.backend.dto;

import java.util.UUID;

public class ResendOtpRequest {
    private UUID challengeId;

    public ResendOtpRequest() {
    }

    public ResendOtpRequest(UUID challengeId) {
        this.challengeId = challengeId;
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(UUID challengeId) {
        this.challengeId = challengeId;
    }
}
