package com.crimesphere.backend.dto.notification;

import com.crimesphere.backend.entity.NotificationPreference;

import java.time.OffsetDateTime;
import java.util.UUID;

public class NotificationPreferenceResponse {

    private UUID id;
    private UUID policeOfficerId;
    private boolean emailEnabled;
    private boolean whatsappEnabled;
    private boolean caseCreatedEnabled;
    private boolean caseStatusEnabled;
    private boolean casePriorityEnabled;
    private boolean aiReviewEnabled;
    private boolean evidenceStatusEnabled;
    private OffsetDateTime updatedAt;

    public NotificationPreferenceResponse() {}

    public NotificationPreferenceResponse(NotificationPreference p) {
        this.id = p.getId();
        if (p.getPoliceOfficer() != null) {
            this.policeOfficerId = p.getPoliceOfficer().getId();
        }
        this.emailEnabled = p.isEmailEnabled();
        this.whatsappEnabled = p.isWhatsappEnabled();
        this.caseCreatedEnabled = p.isCaseCreatedEnabled();
        this.caseStatusEnabled = p.isCaseStatusEnabled();
        this.casePriorityEnabled = p.isCasePriorityEnabled();
        this.aiReviewEnabled = p.isAiReviewEnabled();
        this.evidenceStatusEnabled = p.isEvidenceStatusEnabled();
        this.updatedAt = p.getUpdatedAt();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPoliceOfficerId() {
        return policeOfficerId;
    }

    public void setPoliceOfficerId(UUID policeOfficerId) {
        this.policeOfficerId = policeOfficerId;
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
