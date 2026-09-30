package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public class EvidenceVerificationItemDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("verificationId")
    private UUID verificationId;

    @JsonProperty("evidenceId")
    private UUID evidenceId;

    @JsonProperty("evidenceNumber")
    private String evidenceNumber;

    @JsonProperty("evidenceTitle")
    private String evidenceTitle;

    @JsonProperty("itemType")
    private String itemType; // EVIDENCE_REFERENCE, CLAIM, MISSING_SUPPORT, CONTRADICTION, CASE_FACT

    @JsonProperty("claim")
    private String claim;

    @JsonProperty("supportingContent")
    private String supportingContent;

    @JsonProperty("result")
    private String result; // SUPPORTED, PARTIALLY_SUPPORTED, UNSUPPORTED, NOT_FOUND, CONFLICT

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("createdAt")
    private OffsetDateTime createdAt;

    public EvidenceVerificationItemDTO() {
    }

    public EvidenceVerificationItemDTO(UUID id, UUID verificationId, UUID evidenceId, String evidenceNumber,
                                   String evidenceTitle, String itemType, String claim,
                                   String supportingContent, String result, String reason, OffsetDateTime createdAt) {
        this.id = id;
        this.verificationId = verificationId;
        this.evidenceId = evidenceId;
        this.evidenceNumber = evidenceNumber;
        this.evidenceTitle = evidenceTitle;
        this.itemType = itemType;
        this.claim = claim;
        this.supportingContent = supportingContent;
        this.result = result;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(UUID verificationId) {
        this.verificationId = verificationId;
    }

    public UUID getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(UUID evidenceId) {
        this.evidenceId = evidenceId;
    }

    public String getEvidenceNumber() {
        return evidenceNumber;
    }

    public void setEvidenceNumber(String evidenceNumber) {
        this.evidenceNumber = evidenceNumber;
    }

    public String getEvidenceTitle() {
        return evidenceTitle;
    }

    public void setEvidenceTitle(String evidenceTitle) {
        this.evidenceTitle = evidenceTitle;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getClaim() {
        return claim;
    }

    public void setClaim(String claim) {
        this.claim = claim;
    }

    public String getSupportingContent() {
        return supportingContent;
    }

    public void setSupportingContent(String supportingContent) {
        this.supportingContent = supportingContent;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
