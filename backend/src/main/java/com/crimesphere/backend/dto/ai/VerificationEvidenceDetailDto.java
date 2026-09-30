package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class VerificationEvidenceDetailDto {

    @JsonProperty("evidence_id")
    private UUID evidenceId;

    @JsonProperty("evidence_number")
    private String evidenceNumber;

    @JsonProperty("title")
    private String title;

    @JsonProperty("evidence_type")
    private String evidenceType;

    @JsonProperty("status")
    private String status;

    @JsonProperty("relationship")
    private String relationship; // SUPPORTS, PARTIALLY_SUPPORTS, DOES_NOT_SUPPORT, INVALID_REFERENCE

    @JsonProperty("support_note")
    private String supportNote;

    public VerificationEvidenceDetailDto() {
    }

    public VerificationEvidenceDetailDto(UUID evidenceId, String evidenceNumber, String title,
                                         String evidenceType, String status, String relationship, String supportNote) {
        this.evidenceId = evidenceId;
        this.evidenceNumber = evidenceNumber;
        this.title = title;
        this.evidenceType = evidenceType;
        this.status = status;
        this.relationship = relationship;
        this.supportNote = supportNote;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getSupportNote() {
        return supportNote;
    }

    public void setSupportNote(String supportNote) {
        this.supportNote = supportNote;
    }
}
