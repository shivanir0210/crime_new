package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class AiPotentialLeadDto {

    @JsonProperty("lead")
    private String lead;

    @JsonProperty("basis")
    private String basis;

    @JsonProperty("evidence_references")
    private List<String> evidenceReferences = new ArrayList<>();

    @JsonProperty("priority")
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH

    public AiPotentialLeadDto() {
    }

    public AiPotentialLeadDto(String lead, String basis, List<String> evidenceReferences, String priority) {
        this.lead = lead;
        this.basis = basis;
        this.evidenceReferences = evidenceReferences != null ? evidenceReferences : new ArrayList<>();
        this.priority = priority;
    }

    public String getLead() {
        return lead;
    }

    public void setLead(String lead) {
        this.lead = lead;
    }

    public String getBasis() {
        return basis;
    }

    public void setBasis(String basis) {
        this.basis = basis;
    }

    public List<String> getEvidenceReferences() {
        return evidenceReferences;
    }

    public void setEvidenceReferences(List<String> evidenceReferences) {
        this.evidenceReferences = evidenceReferences;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
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


