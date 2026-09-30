package com.crimesphere.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CaseOfficerDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("caseId")
    private UUID caseId;

    @JsonProperty("officerId")
    private UUID officerId;

    @JsonProperty("policeId")
    private String policeId;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("role")
    private String role;

    @JsonProperty("rankDesignation")
    private String rankDesignation;

    @JsonProperty("stationName")
    private String stationName;

    @JsonProperty("assignedAt")
    private OffsetDateTime assignedAt;

    @JsonProperty("assignedBy")
    private String assignedBy;

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("phone")
    private String phone;

    public CaseOfficerDto() {}

    public CaseOfficerDto(UUID id, UUID caseId, UUID officerId, String policeId, String fullName,
                          String role, String rankDesignation, String stationName,
                          OffsetDateTime assignedAt, String assignedBy, boolean active) {
        this(id, caseId, officerId, policeId, fullName, role, rankDesignation, stationName, assignedAt, assignedBy, active, null);
    }

    public CaseOfficerDto(UUID id, UUID caseId, UUID officerId, String policeId, String fullName,
                          String role, String rankDesignation, String stationName,
                          OffsetDateTime assignedAt, String assignedBy, boolean active, String phone) {
        this.id = id;
        this.caseId = caseId;
        this.officerId = officerId;
        this.policeId = policeId;
        this.fullName = fullName;
        this.role = role;
        this.rankDesignation = rankDesignation;
        this.stationName = stationName;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
        this.active = active;
        this.phone = phone;
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

    public UUID getOfficerId() {
        return officerId;
    }

    public void setOfficerId(UUID officerId) {
        this.officerId = officerId;
    }

    public String getPoliceId() {
        return policeId;
    }

    public void setPoliceId(String policeId) {
        this.policeId = policeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRankDesignation() {
        return rankDesignation;
    }

    public void setRankDesignation(String rankDesignation) {
        this.rankDesignation = rankDesignation;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(OffsetDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
