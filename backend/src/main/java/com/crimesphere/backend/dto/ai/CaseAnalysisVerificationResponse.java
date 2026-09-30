package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CaseAnalysisVerificationResponse {

    @JsonProperty("analysis_id")
    private UUID analysisId;

    @JsonProperty("case_id")
    private UUID caseId;

    @JsonProperty("case_number")
    private String caseNumber;

    @JsonProperty("verified_at")
    private OffsetDateTime verifiedAt;

    @JsonProperty("verified_by_officer")
    private String verifiedByOfficer;

    @JsonProperty("total_items_checked")
    private int totalItemsChecked;

    @JsonProperty("supported_count")
    private int supportedCount;

    @JsonProperty("partially_supported_count")
    private int partiallySupportedCount;

    @JsonProperty("unsupported_count")
    private int unsupportedCount;

    @JsonProperty("unverifiable_count")
    private int unverifiableCount;

    @JsonProperty("reference_invalid_count")
    private int referenceInvalidCount;

    @JsonProperty("verifications")
    private List<ItemVerificationDto> verifications = new ArrayList<>();

    public CaseAnalysisVerificationResponse() {
    }

    public UUID getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(UUID analysisId) {
        this.analysisId = analysisId;
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

    public int getTotalItemsChecked() {
        return totalItemsChecked;
    }

    public void setTotalItemsChecked(int totalItemsChecked) {
        this.totalItemsChecked = totalItemsChecked;
    }

    public int getSupportedCount() {
        return supportedCount;
    }

    public void setSupportedCount(int supportedCount) {
        this.supportedCount = supportedCount;
    }

    public int getPartiallySupportedCount() {
        return partiallySupportedCount;
    }

    public void setPartiallySupportedCount(int partiallySupportedCount) {
        this.partiallySupportedCount = partiallySupportedCount;
    }

    public int getUnsupportedCount() {
        return unsupportedCount;
    }

    public void setUnsupportedCount(int unsupportedCount) {
        this.unsupportedCount = unsupportedCount;
    }

    public int getUnverifiableCount() {
        return unverifiableCount;
    }

    public void setUnverifiableCount(int unverifiableCount) {
        this.unverifiableCount = unverifiableCount;
    }

    public int getReferenceInvalidCount() {
        return referenceInvalidCount;
    }

    public void setReferenceInvalidCount(int referenceInvalidCount) {
        this.referenceInvalidCount = referenceInvalidCount;
    }

    public List<ItemVerificationDto> getVerifications() {
        return verifications;
    }

    public void setVerifications(List<ItemVerificationDto> verifications) {
        this.verifications = verifications;
    }
}
