package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class AiEvidenceGapAnalysisDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("gap")
    private String gap;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("severity")
    private String severity = "MEDIUM"; // LOW, MEDIUM, HIGH

    @JsonProperty("verification")
    private ItemVerificationDto verification;

    @JsonProperty("review")
    private InvestigatorReviewResponse review;

    public AiEvidenceGapAnalysisDto() {
    }

    public AiEvidenceGapAnalysisDto(String gap, String reason, String severity) {
        this.gap = gap;
        this.reason = reason;
        this.severity = severity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getGap() {
        return gap;
    }

    public void setGap(String gap) {
        this.gap = gap;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
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
