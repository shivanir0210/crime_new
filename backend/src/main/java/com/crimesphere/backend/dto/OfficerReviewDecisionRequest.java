package com.crimesphere.backend.dto;

public class OfficerReviewDecisionRequest {

    private String rejectionReason;

    public OfficerReviewDecisionRequest() {
    }

    public OfficerReviewDecisionRequest(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
