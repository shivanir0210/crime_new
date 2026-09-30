package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class AiKeyObservationDto {

    @JsonProperty("observation")
    private String observation;

    @JsonProperty("evidence_references")
    private List<String> evidenceReferences = new ArrayList<>();

    @JsonProperty("reasoning")
    private String reasoning;

    public AiKeyObservationDto() {
    }

    public AiKeyObservationDto(String observation, List<String> evidenceReferences, String reasoning) {
        this.observation = observation;
        this.evidenceReferences = evidenceReferences != null ? evidenceReferences : new ArrayList<>();
        this.reasoning = reasoning;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public List<String> getEvidenceReferences() {
        return evidenceReferences;
    }

    public void setEvidenceReferences(List<String> evidenceReferences) {
        this.evidenceReferences = evidenceReferences;
    }

    public String getReasoning() {
        return reasoning;
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

