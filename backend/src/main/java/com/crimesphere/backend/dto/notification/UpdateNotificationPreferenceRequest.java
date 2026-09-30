package com.crimesphere.backend.dto.notification;

public class UpdateNotificationPreferenceRequest {

    private Boolean emailEnabled;
    private Boolean whatsappEnabled;
    private Boolean caseCreatedEnabled;
    private Boolean caseStatusEnabled;
    private Boolean casePriorityEnabled;
    private Boolean aiReviewEnabled;
    private Boolean evidenceStatusEnabled;

    public UpdateNotificationPreferenceRequest() {}

    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public Boolean getWhatsappEnabled() {
        return whatsappEnabled;
    }

    public void setWhatsappEnabled(Boolean whatsappEnabled) {
        this.whatsappEnabled = whatsappEnabled;
    }

    public Boolean getCaseCreatedEnabled() {
        return caseCreatedEnabled;
    }

    public void setCaseCreatedEnabled(Boolean caseCreatedEnabled) {
        this.caseCreatedEnabled = caseCreatedEnabled;
    }

    public Boolean getCaseStatusEnabled() {
        return caseStatusEnabled;
    }

    public void setCaseStatusEnabled(Boolean caseStatusEnabled) {
        this.caseStatusEnabled = caseStatusEnabled;
    }

    public Boolean getCasePriorityEnabled() {
        return casePriorityEnabled;
    }

    public void setCasePriorityEnabled(Boolean casePriorityEnabled) {
        this.casePriorityEnabled = casePriorityEnabled;
    }

    public Boolean getAiReviewEnabled() {
        return aiReviewEnabled;
    }

    public void setAiReviewEnabled(Boolean aiReviewEnabled) {
        this.aiReviewEnabled = aiReviewEnabled;
    }

    public Boolean getEvidenceStatusEnabled() {
        return evidenceStatusEnabled;
    }

    public void setEvidenceStatusEnabled(Boolean evidenceStatusEnabled) {
        this.evidenceStatusEnabled = evidenceStatusEnabled;
    }
}
