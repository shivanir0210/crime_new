package com.crimesphere.backend.dto.analysis;

import java.time.OffsetDateTime;
import java.util.UUID;

public class EvidenceGapDTO {

    private UUID id;
    private UUID caseId;
    private String category;
    private String title;
    private String description;
    private String severity;
    private String status;
    private String reason;
    private String sourceType;
    private UUID sourceReferenceId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime resolvedAt;
    private UUID resolvedById;
    private String resolvedByName;

    public EvidenceGapDTO() {
    }

    public EvidenceGapDTO(UUID id, UUID caseId, String category, String title, String description,
                          String severity, String status, String reason, String sourceType,
                          UUID sourceReferenceId, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                          OffsetDateTime resolvedAt, UUID resolvedById, String resolvedByName) {
        this.id = id;
        this.caseId = caseId;
        this.category = category;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.status = status;
        this.reason = reason;
        this.sourceType = sourceType;
        this.sourceReferenceId = sourceReferenceId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.resolvedAt = resolvedAt;
        this.resolvedById = resolvedById;
        this.resolvedByName = resolvedByName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getSourceReferenceId() {
        return sourceReferenceId;
    }

    public void setSourceReferenceId(UUID sourceReferenceId) {
        this.sourceReferenceId = sourceReferenceId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public UUID getResolvedById() {
        return resolvedById;
    }

    public void setResolvedById(UUID resolvedById) {
        this.resolvedById = resolvedById;
    }

    public String getResolvedByName() {
        return resolvedByName;
    }

    public void setResolvedByName(String resolvedByName) {
        this.resolvedByName = resolvedByName;
    }
}
