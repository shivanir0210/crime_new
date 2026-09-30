package com.crimesphere.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "police_officer_id", nullable = false, unique = true)
    private PoliceOfficer policeOfficer;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "whatsapp_enabled", nullable = false)
    private boolean whatsappEnabled = false;

    @Column(name = "case_created_enabled", nullable = false)
    private boolean caseCreatedEnabled = true;

    @Column(name = "case_status_enabled", nullable = false)
    private boolean caseStatusEnabled = true;

    @Column(name = "case_priority_enabled", nullable = false)
    private boolean casePriorityEnabled = true;

    @Column(name = "ai_review_enabled", nullable = false)
    private boolean aiReviewEnabled = true;

    @Column(name = "evidence_status_enabled", nullable = false)
    private boolean evidenceStatusEnabled = true;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onPersist() {
        this.updatedAt = OffsetDateTime.now();
    }

    public NotificationPreference() {}

    public NotificationPreference(PoliceOfficer policeOfficer) {
        this.policeOfficer = policeOfficer;
        this.emailEnabled = true;
        this.whatsappEnabled = false;
        this.caseCreatedEnabled = true;
        this.caseStatusEnabled = true;
        this.casePriorityEnabled = true;
        this.aiReviewEnabled = true;
        this.evidenceStatusEnabled = true;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PoliceOfficer getPoliceOfficer() {
        return policeOfficer;
    }

    public void setPoliceOfficer(PoliceOfficer policeOfficer) {
        this.policeOfficer = policeOfficer;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isWhatsappEnabled() {
        return whatsappEnabled;
    }

    public void setWhatsappEnabled(boolean whatsappEnabled) {
        this.whatsappEnabled = whatsappEnabled;
    }

    public boolean isCaseCreatedEnabled() {
        return caseCreatedEnabled;
    }

    public void setCaseCreatedEnabled(boolean caseCreatedEnabled) {
        this.caseCreatedEnabled = caseCreatedEnabled;
    }

    public boolean isCaseStatusEnabled() {
        return caseStatusEnabled;
    }

    public void setCaseStatusEnabled(boolean caseStatusEnabled) {
        this.caseStatusEnabled = caseStatusEnabled;
    }

    public boolean isCasePriorityEnabled() {
        return casePriorityEnabled;
    }

    public void setCasePriorityEnabled(boolean casePriorityEnabled) {
        this.casePriorityEnabled = casePriorityEnabled;
    }

    public boolean isAiReviewEnabled() {
        return aiReviewEnabled;
    }

    public void setAiReviewEnabled(boolean aiReviewEnabled) {
        this.aiReviewEnabled = aiReviewEnabled;
    }

    public boolean isEvidenceStatusEnabled() {
        return evidenceStatusEnabled;
    }

    public void setEvidenceStatusEnabled(boolean evidenceStatusEnabled) {
        this.evidenceStatusEnabled = evidenceStatusEnabled;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
