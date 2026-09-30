package com.crimesphere.backend.dto.evidence;

import java.time.OffsetDateTime;

public class UpdateEvidenceRequest {

    private String title;
    private String description;
    private String evidenceType;
    private OffsetDateTime collectedAt;
    private String collectedLocation;
    private String collectedBy;
    private String notes;

    public UpdateEvidenceRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public OffsetDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(OffsetDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    public String getCollectedLocation() {
        return collectedLocation;
    }

    public void setCollectedLocation(String collectedLocation) {
        this.collectedLocation = collectedLocation;
    }

    public String getCollectedBy() {
        return collectedBy;
    }

    public void setCollectedBy(String collectedBy) {
        this.collectedBy = collectedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
