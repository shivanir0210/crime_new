package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public class VerificationHistoryDTO {

    @JsonProperty("verificationId")
    private UUID verificationId;

    @JsonProperty("caseId")
    private UUID caseId;

    @JsonProperty("recommendationId")
    private UUID recommendationId;

    @JsonProperty("verificationStatus")
    private String verificationStatus;

    @JsonProperty("supportLevel")
    private String supportLevel;

    @JsonProperty("verificationSummary")
    private String verificationSummary;

    @JsonProperty("verifiedEvidenceCount")
    private int verifiedEvidenceCount;

    @JsonProperty("missingEvidenceCount")
    private int missingEvidenceCount;

    @JsonProperty("conflictCount")
    private int conflictCount;

    @JsonProperty("verifiedBy")
    private String verifiedBy;

    @JsonProperty("createdAt")
    private OffsetDateTime createdAt;

    public VerificationHistoryDTO() {
    }

    public VerificationHistoryDTO(UUID verificationId, UUID caseId, UUID recommendationId, String verificationStatus,
                                  String supportLevel, String verificationSummary, int verifiedEvidenceCount,
                                  int missingEvidenceCount, int conflictCount, String verifiedBy, OffsetDateTime createdAt) {
        this.verificationId = verificationId;
        this.caseId = caseId;
        this.recommendationId = recommendationId;
        this.verificationStatus = verificationStatus;
        this.supportLevel = supportLevel;
        this.verificationSummary = verificationSummary;
        this.verifiedEvidenceCount = verifiedEvidenceCount;
        this.missingEvidenceCount = missingEvidenceCount;
        this.conflictCount = conflictCount;
        this.verifiedBy = verifiedBy;
        this.createdAt = createdAt;
    }

    public UUID getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(UUID verificationId) {
        this.verificationId = verificationId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(UUID recommendationId) {
        this.recommendationId = recommendationId;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getSupportLevel() {
        return supportLevel;
    }

    public void setSupportLevel(String supportLevel) {
        this.supportLevel = supportLevel;
    }

    public String getVerificationSummary() {
        return verificationSummary;
    }

    public void setVerificationSummary(String verificationSummary) {
        this.verificationSummary = verificationSummary;
    }

    public int getVerifiedEvidenceCount() {
        return verifiedEvidenceCount;
    }

    public void setVerifiedEvidenceCount(int verifiedEvidenceCount) {
        this.verifiedEvidenceCount = verifiedEvidenceCount;
    }

    public int getMissingEvidenceCount() {
        return missingEvidenceCount;
    }

    public void setMissingEvidenceCount(int missingEvidenceCount) {
        this.missingEvidenceCount = missingEvidenceCount;
    }

    public int getConflictCount() {
        return conflictCount;
    }

    public void setConflictCount(int conflictCount) {
        this.conflictCount = conflictCount;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
