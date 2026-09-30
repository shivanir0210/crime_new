package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EvidenceVerificationResponse {

    @JsonProperty("verificationId")
    private UUID verificationId;

    @JsonProperty("caseId")
    private UUID caseId;

    @JsonProperty("caseNumber")
    private String caseNumber;

    @JsonProperty("recommendationId")
    private UUID recommendationId;

    @JsonProperty("recommendationItemId")
    private UUID recommendationItemId;

    @JsonProperty("verificationStatus")
    private String verificationStatus; // PENDING, COMPLETED, REQUIRES_REVIEW

    @JsonProperty("supportLevel")
    private String supportLevel; // SUPPORTED, PARTIALLY_SUPPORTED, UNSUPPORTED, VERIFICATION_REQUIRED

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

    @JsonProperty("verifiedAt")
    private OffsetDateTime verifiedAt;

    @JsonProperty("createdAt")
    private OffsetDateTime createdAt;

    @JsonProperty("items")
    private List<EvidenceVerificationItemDTO> items = new ArrayList<>();

    public EvidenceVerificationResponse() {
    }

    public EvidenceVerificationResponse(UUID verificationId, UUID caseId, String caseNumber, UUID recommendationId,
                                        UUID recommendationItemId, String verificationStatus, String supportLevel,
                                        String verificationSummary, int verifiedEvidenceCount, int missingEvidenceCount,
                                        int conflictCount, String verifiedBy, OffsetDateTime verifiedAt,
                                        OffsetDateTime createdAt, List<EvidenceVerificationItemDTO> items) {
        this.verificationId = verificationId;
        this.caseId = caseId;
        this.caseNumber = caseNumber;
        this.recommendationId = recommendationId;
        this.recommendationItemId = recommendationItemId;
        this.verificationStatus = verificationStatus;
        this.supportLevel = supportLevel;
        this.verificationSummary = verificationSummary;
        this.verifiedEvidenceCount = verifiedEvidenceCount;
        this.missingEvidenceCount = missingEvidenceCount;
        this.conflictCount = conflictCount;
        this.verifiedBy = verifiedBy;
        this.verifiedAt = verifiedAt;
        this.createdAt = createdAt;
        this.items = items != null ? items : new ArrayList<>();
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

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public UUID getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(UUID recommendationId) {
        this.recommendationId = recommendationId;
    }

    public UUID getRecommendationItemId() {
        return recommendationItemId;
    }

    public void setRecommendationItemId(UUID recommendationItemId) {
        this.recommendationItemId = recommendationItemId;
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

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(OffsetDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<EvidenceVerificationItemDTO> getItems() {
        return items;
    }

    public void setItems(List<EvidenceVerificationItemDTO> items) {
        this.items = items;
    }
}
