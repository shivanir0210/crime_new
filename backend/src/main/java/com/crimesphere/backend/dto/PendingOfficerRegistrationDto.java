package com.crimesphere.backend.dto;

import com.crimesphere.backend.entity.PoliceOfficer;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PendingOfficerRegistrationDto {

    private UUID id;
    private String policeId;
    private String fullName;
    private String email;
    private String phone;
    private UUID stationId;
    private String stationName;
    private String stationCode;
    private String role;
    private String status;
    private Boolean isActive;

    private String ocrStatus;
    private String ocrDetectedName;
    private String ocrDetectedPoliceId;
    private String ocrDetectedStation;
    private String ocrDetectedRole;

    private boolean nameMatches;
    private boolean policeIdMatches;
    private boolean stationMatches;
    private boolean roleMatches;

    private boolean hasIdDocument;
    private String documentDownloadUrl;

    private String rejectionReason;
    private OffsetDateTime createdAt;

    public PendingOfficerRegistrationDto() {
    }

    public PendingOfficerRegistrationDto(PoliceOfficer officer) {
        if (officer != null) {
            this.id = officer.getId();
            this.policeId = officer.getPoliceId();
            this.fullName = officer.getFullName();
            this.email = officer.getEmail();
            this.phone = officer.getPhone();
            if (officer.getStation() != null) {
                this.stationId = officer.getStation().getId();
                this.stationName = officer.getStation().getStationName();
                this.stationCode = officer.getStation().getStationCode();
            }
            this.role = officer.getRole();
            this.status = officer.getStatus();
            this.isActive = officer.getIsActive();

            this.ocrStatus = officer.getOcrStatus() != null ? officer.getOcrStatus() : "PENDING";
            this.ocrDetectedName = officer.getOcrDetectedName();
            this.ocrDetectedPoliceId = officer.getOcrDetectedPoliceId();
            this.ocrDetectedStation = officer.getOcrDetectedStation();
            this.ocrDetectedRole = officer.getOcrDetectedRole();

            this.hasIdDocument = officer.getIdDocumentPath() != null && !officer.getIdDocumentPath().isBlank();
            if (this.hasIdDocument && officer.getId() != null) {
                this.documentDownloadUrl = "/api/auth/station-registrations/" + officer.getId() + "/document";
            }

            this.rejectionReason = officer.getRejectionReason();
            this.createdAt = officer.getCreatedAt();

            // Compute comparison matches
            this.nameMatches = isFieldMatching(officer.getFullName(), officer.getOcrDetectedName());
            this.policeIdMatches = isFieldMatching(officer.getPoliceId(), officer.getOcrDetectedPoliceId());
            this.stationMatches = isStationMatching(officer, officer.getOcrDetectedStation());
            this.roleMatches = isFieldMatching(officer.getRole(), officer.getOcrDetectedRole());
        }
    }

    private boolean isFieldMatching(String entered, String detected) {
        if (entered == null || detected == null) return false;
        String e = normalize(entered);
        String d = normalize(detected);
        if (e.isEmpty() || d.isEmpty()) return false;
        if (e.equals(d)) return true;
        if (d.contains(e) || e.contains(d)) return true;

        // Token match
        String[] eTokens = e.split("\\s+");
        String[] dTokens = d.split("\\s+");
        int matchCount = 0;
        for (String et : eTokens) {
            for (String dt : dTokens) {
                if (et.equals(dt) || (et.length() > 3 && dt.contains(et))) {
                    matchCount++;
                    break;
                }
            }
        }
        return matchCount >= Math.min(eTokens.length, 2);
    }

    private boolean isStationMatching(PoliceOfficer officer, String detectedStation) {
        if (officer.getStation() == null || detectedStation == null) return false;
        String detected = normalize(detectedStation);
        if (detected.isEmpty()) return false;
        String code = normalize(officer.getStation().getStationCode());
        String name = normalize(officer.getStation().getStationName());
        String district = normalize(officer.getStation().getDistrict());
        return detected.contains(code) || detected.contains(name) || name.contains(detected) || detected.contains(district);
    }

    private String normalize(String str) {
        if (str == null) return "";
        return str.toLowerCase().replaceAll("[^a-z0-9]", " ").trim().replaceAll("\\s+", " ");
    }

    // Getters and Setters
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

    public UUID getStationId() {
        return stationId;
    }

    public void setStationId(UUID stationId) {
        this.stationId = stationId;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public String getOcrDetectedName() {
        return ocrDetectedName;
    }

    public void setOcrDetectedName(String ocrDetectedName) {
        this.ocrDetectedName = ocrDetectedName;
    }

    public String getOcrDetectedPoliceId() {
        return ocrDetectedPoliceId;
    }

    public void setOcrDetectedPoliceId(String ocrDetectedPoliceId) {
        this.ocrDetectedPoliceId = ocrDetectedPoliceId;
    }

    public String getOcrDetectedStation() {
        return ocrDetectedStation;
    }

    public void setOcrDetectedStation(String ocrDetectedStation) {
        this.ocrDetectedStation = ocrDetectedStation;
    }

    public String getOcrDetectedRole() {
        return ocrDetectedRole;
    }

    public void setOcrDetectedRole(String ocrDetectedRole) {
        this.ocrDetectedRole = ocrDetectedRole;
    }

    public boolean isNameMatches() {
        return nameMatches;
    }

    public void setNameMatches(boolean nameMatches) {
        this.nameMatches = nameMatches;
    }

    public boolean isPoliceIdMatches() {
        return policeIdMatches;
    }

    public void setPoliceIdMatches(boolean policeIdMatches) {
        this.policeIdMatches = policeIdMatches;
    }

    public boolean isStationMatches() {
        return stationMatches;
    }

    public void setStationMatches(boolean stationMatches) {
        this.stationMatches = stationMatches;
    }

    public boolean isRoleMatches() {
        return roleMatches;
    }

    public void setRoleMatches(boolean roleMatches) {
        this.roleMatches = roleMatches;
    }

    public boolean isHasIdDocument() {
        return hasIdDocument;
    }

    public void setHasIdDocument(boolean hasIdDocument) {
        this.hasIdDocument = hasIdDocument;
    }

    public String getDocumentDownloadUrl() {
        return documentDownloadUrl;
    }

    public void setDocumentDownloadUrl(String documentDownloadUrl) {
        this.documentDownloadUrl = documentDownloadUrl;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
