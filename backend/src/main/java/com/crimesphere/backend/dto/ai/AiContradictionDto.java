package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class AiContradictionDto {

    @JsonProperty("description")
    private String description;

    @JsonProperty("references")
    private List<String> references = new ArrayList<>();

    public AiContradictionDto() {
    }

    public AiContradictionDto(String description, List<String> references) {
        this.description = description;
        this.references = references != null ? references : new ArrayList<>();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getReferences() {
        return references;
    }

    public void setReferences(List<String> references) {
        this.references = references;
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

