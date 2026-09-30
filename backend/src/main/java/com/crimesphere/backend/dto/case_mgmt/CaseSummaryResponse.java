package com.crimesphere.backend.dto.case_mgmt;

import com.crimesphere.backend.entity.Case;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CaseSummaryResponse {

    private UUID id;
    private String caseNumber;
    private String title;
    private String description;
    private String caseType;
    private String priority;
    private String status;
    private String stationName;
    private String stationCode;
    private String createdByName;
    private String createdByPoliceId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public CaseSummaryResponse() {
    }

    public CaseSummaryResponse(Case c) {
        if (c != null) {
            this.id = c.getId();
            this.caseNumber = c.getCaseNumber();
            this.title = c.getTitle();
            this.description = c.getDescription();
            this.caseType = c.getCaseType();
            this.priority = c.getPriority();
            this.status = c.getStatus();
            if (c.getStation() != null) {
                this.stationName = c.getStation().getStationName();
                this.stationCode = c.getStation().getStationCode();
            }
            if (c.getCreatedBy() != null) {
                this.createdByName = c.getCreatedBy().getFullName();
                this.createdByPoliceId = c.getCreatedBy().getPoliceId();
            }
            this.createdAt = c.getCreatedAt();
            this.updatedAt = c.getUpdatedAt();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
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

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCreatedByPoliceId() {
        return createdByPoliceId;
    }

    public void setCreatedByPoliceId(String createdByPoliceId) {
        this.createdByPoliceId = createdByPoliceId;
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
}
