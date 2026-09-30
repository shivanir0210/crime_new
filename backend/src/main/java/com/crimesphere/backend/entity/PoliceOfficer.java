package com.crimesphere.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "police_officers")
public class PoliceOfficer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "police_id", unique = true, nullable = false, length = 50)
    private String policeId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private PoliceStation station;

    @Column(name = "role", nullable = false, length = 50)
    private String role = "OFFICER";

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(name = "id_document_path", length = 500)
    private String idDocumentPath;

    @Column(name = "ocr_status", length = 50)
    private String ocrStatus = "PENDING";

    @Column(name = "ocr_detected_name")
    private String ocrDetectedName;

    @Column(name = "ocr_detected_police_id", length = 50)
    private String ocrDetectedPoliceId;

    @Column(name = "ocr_detected_station")
    private String ocrDetectedStation;

    @Column(name = "ocr_detected_role", length = 50)
    private String ocrDetectedRole;

    @Column(name = "ocr_raw_text", columnDefinition = "TEXT")
    private String ocrRawText;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_officer_id")
    private PoliceOfficer approvedByOfficer;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public PoliceOfficer() {
    }

    public PoliceOfficer(UUID id, String policeId, String fullName, String email, String phone, PoliceStation station, String role, Boolean isActive) {
        this.id = id;
        this.policeId = policeId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.station = station;
        this.role = role;
        this.isActive = isActive;
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

    public PoliceStation getStation() {
        return station;
    }

    public void setStation(PoliceStation station) {
        this.station = station;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIdDocumentPath() {
        return idDocumentPath;
    }

    public void setIdDocumentPath(String idDocumentPath) {
        this.idDocumentPath = idDocumentPath;
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

    public String getOcrRawText() {
        return ocrRawText;
    }

    public void setOcrRawText(String ocrRawText) {
        this.ocrRawText = ocrRawText;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public PoliceOfficer getApprovedByOfficer() {
        return approvedByOfficer;
    }

    public void setApprovedByOfficer(PoliceOfficer approvedByOfficer) {
        this.approvedByOfficer = approvedByOfficer;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PoliceOfficer that = (PoliceOfficer) o;
        if (id != null && that.id != null) {
            return id.equals(that.id);
        }
        return policeId != null && policeId.equals(that.policeId);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : (policeId != null ? policeId.hashCode() : 0);
    }
}
