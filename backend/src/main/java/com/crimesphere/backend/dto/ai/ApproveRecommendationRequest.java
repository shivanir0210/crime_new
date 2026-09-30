package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApproveRecommendationRequest {

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("confirmUnsupported")
    private Boolean confirmUnsupported;

    public ApproveRecommendationRequest() {
    }

    public ApproveRecommendationRequest(String comment, Boolean confirmUnsupported) {
        this.comment = comment;
        this.confirmUnsupported = confirmUnsupported;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getConfirmUnsupported() {
        return confirmUnsupported;
    }

    public void setConfirmUnsupported(Boolean confirmUnsupported) {
        this.confirmUnsupported = confirmUnsupported;
    }
}
