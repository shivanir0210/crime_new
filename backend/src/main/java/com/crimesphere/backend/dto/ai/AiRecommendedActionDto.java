package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class AiRecommendedActionDto {

    @JsonProperty("action")
    private String action;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("priority")
    private String priority = "HIGH"; // LOW, MEDIUM, HIGH

    @JsonProperty("evidence_references")
    private List<String> evidenceReferences = new ArrayList<>();

    public AiRecommendedActionDto() {
    }

    public AiRecommendedActionDto(String action, String reason, String priority, List<String> evidenceReferences) {
        this.action = action;
        this.reason = reason;
        this.priority = priority;
        this.evidenceReferences = evidenceReferences != null ? evidenceReferences : new ArrayList<>();
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public List<String> getEvidenceReferences() {
        return evidenceReferences;
    }

    public void setEvidenceReferences(List<String> evidenceReferences) {
        this.evidenceReferences = evidenceReferences;
    }

    @JsonProperty("id")
    private java.util.UUID id;

    @JsonProperty("verification")
    private ItemVerificationDto verification;

    @JsonProperty("review")
    private InvestigatorReviewResponse review;

    public java.util.UUID getId() {
        return id;
    }

    public void setId(java.util.UUID id) {
        this.id = id;
    }

    public ItemVerificationDto getVerification() {
        return verification;
    }

    public void setVerification(ItemVerificationDto verification) {
        this.verification = verification;
    }

    public InvestigatorReviewResponse getReview() {
        return review;
    }

    public void setReview(InvestigatorReviewResponse review) {
        this.review = review;
    }
}

