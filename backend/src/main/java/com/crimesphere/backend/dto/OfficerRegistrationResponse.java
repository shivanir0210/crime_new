package com.crimesphere.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class OfficerRegistrationResponse {

    private UUID id;
    private String policeId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String stationName;
    private String stationCode;
    private String status; // PENDING_STATION_APPROVAL
    private Boolean isActive;
    private String ocrStatus;
    private OcrComparisonResultDto ocrComparison;
    private String message;
    private OffsetDateTime createdAt;

    public OfficerRegistrationResponse() {
    }

    public OfficerRegistrationResponse(UUID id, String policeId, String fullName, String email,
                                     String phone, String role, String stationName, String stationCode,
                                     String status, Boolean isActive, String ocrStatus,
                                     OcrComparisonResultDto ocrComparison, String message,
                                     OffsetDateTime createdAt) {
        this.id = id;
        this.policeId = policeId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.stationName = stationName;
        this.stationCode = stationCode;
        this.status = status;
        this.isActive = isActive;
        this.ocrStatus = ocrStatus;
        this.ocrComparison = ocrComparison;
        this.message = message;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public String getOcrStatus() {
        return ocrStatus;
    }

    public void setOcrStatus(String ocrStatus) {
        this.ocrStatus = ocrStatus;
    }

    public OcrComparisonResultDto getOcrComparison() {
        return ocrComparison;
    }

    public void setOcrComparison(OcrComparisonResultDto ocrComparison) {
        this.ocrComparison = ocrComparison;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
