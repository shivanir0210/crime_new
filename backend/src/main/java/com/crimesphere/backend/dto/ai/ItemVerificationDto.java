package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemVerificationDto {

    @JsonProperty("verification_id")
    private UUID verificationId;

    @JsonProperty("recommendation_item_id")
    private UUID recommendationItemId;

    @JsonProperty("item_type")
    private String itemType;

    @JsonProperty("item_content")
    private String itemContent;

    @JsonProperty("status")
    private String status; // SUPPORTED, PARTIALLY_SUPPORTED, UNSUPPORTED, UNVERIFIABLE, REFERENCE_INVALID

    @JsonProperty("support_level")
    private String supportLevel; // STRONG, MODERATE, WEAK, NONE

    @JsonProperty("verification_method")
    private String verificationMethod;

    @JsonProperty("verification_summary")
    private String verificationSummary;

    @JsonProperty("verified_at")
    private OffsetDateTime verifiedAt;

    @JsonProperty("verified_by_officer")
    private String verifiedByOfficer;

    @JsonProperty("evidence")
    private List<VerificationEvidenceDetailDto> evidence = new ArrayList<>();

    public ItemVerificationDto() {
    }

    public ItemVerificationDto(UUID verificationId, UUID recommendationItemId, String itemType, String itemContent,
                               String status, String supportLevel, String verificationMethod, String verificationSummary,
                               OffsetDateTime verifiedAt, String verifiedByOfficer, List<VerificationEvidenceDetailDto> evidence) {
        this.verificationId = verificationId;
        this.recommendationItemId = recommendationItemId;
        this.itemType = itemType;
        this.itemContent = itemContent;
        this.status = status;
        this.supportLevel = supportLevel;
        this.verificationMethod = verificationMethod;
        this.verificationSummary = verificationSummary;
        this.verifiedAt = verifiedAt;
        this.verifiedByOfficer = verifiedByOfficer;
        this.evidence = evidence != null ? evidence : new ArrayList<>();
    }

    public UUID getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(UUID verificationId) {
        this.verificationId = verificationId;
    }

    public UUID getRecommendationItemId() {
        return recommendationItemId;
    }

    public void setRecommendationItemId(UUID recommendationItemId) {
        this.recommendationItemId = recommendationItemId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getItemContent() {
        return itemContent;
    }

    public void setItemContent(String itemContent) {
        this.itemContent = itemContent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSupportLevel() {
        return supportLevel;
    }

    public void setSupportLevel(String supportLevel) {
        this.supportLevel = supportLevel;
    }

    public String getVerificationMethod() {
        return verificationMethod;
    }

    public void setVerificationMethod(String verificationMethod) {
        this.verificationMethod = verificationMethod;
    }

    public String getVerificationSummary() {
        return verificationSummary;
    }

    public void setVerificationSummary(String verificationSummary) {
        this.verificationSummary = verificationSummary;
    }

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(OffsetDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerifiedByOfficer() {
        return verifiedByOfficer;
    }

    public void setVerifiedByOfficer(String verifiedByOfficer) {
        this.verifiedByOfficer = verifiedByOfficer;
    }

    public List<VerificationEvidenceDetailDto> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<VerificationEvidenceDetailDto> evidence) {
        this.evidence = evidence;
    }
}
