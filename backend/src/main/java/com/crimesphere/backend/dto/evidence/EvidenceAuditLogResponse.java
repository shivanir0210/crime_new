package com.crimesphere.backend.dto.evidence;

import java.time.OffsetDateTime;
import java.util.UUID;

public class EvidenceAuditLogResponse {

    private UUID id;
    private UUID evidenceId;
    private UUID caseId;
    private String officerName;
    private String officerBadge;
    private String action;
    private String details;
    private OffsetDateTime createdAt;

    public EvidenceAuditLogResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(UUID evidenceId) {
        this.evidenceId = evidenceId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public String getOfficerName() {
        return officerName;
    }

    public void setOfficerName(String officerName) {
        this.officerName = officerName;
    }

    public String getOfficerBadge() {
        return officerBadge;
    }

    public void setOfficerBadge(String officerBadge) {
        this.officerBadge = officerBadge;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
