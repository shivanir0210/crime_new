package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RejectRecommendationRequest {

    @JsonProperty("comment")
    private String comment;

    public RejectRecommendationRequest() {
    }

    public RejectRecommendationRequest(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
