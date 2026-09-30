package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class EvidenceVerificationRequest {

    @JsonProperty("recommendationItemId")
    private UUID recommendationItemId;

    public EvidenceVerificationRequest() {
    }

    public EvidenceVerificationRequest(UUID recommendationItemId) {
        this.recommendationItemId = recommendationItemId;
    }

    public UUID getRecommendationItemId() {
        return recommendationItemId;
    }

    public void setRecommendationItemId(UUID recommendationItemId) {
        this.recommendationItemId = recommendationItemId;
    }
}
