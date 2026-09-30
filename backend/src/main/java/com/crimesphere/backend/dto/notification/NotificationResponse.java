package com.crimesphere.backend.dto.notification;

import com.crimesphere.backend.entity.Notification;
import com.crimesphere.backend.entity.NotificationChannel;
import com.crimesphere.backend.entity.NotificationStatus;
import com.crimesphere.backend.entity.NotificationType;
import com.crimesphere.backend.entity.NotificationSeverity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationResponse {

    private UUID id;
    private UUID caseId;
    private String caseNumber;
    private String caseTitle;
    private UUID recipientOfficerId;
    private String recipientName;
    private String recipientEmail;
    private String recipientPhone;
    private NotificationChannel channel;
    private NotificationType notificationType;
    private String subject;
    private String message;
    private String externalMessageId;
    private NotificationStatus status;
    private int attemptCount;
    private String lastError;
    private boolean isRead;
    private OffsetDateTime readAt;
    private String severity;
    private String referenceType;
    private UUID referenceId;
    private OffsetDateTime sentAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public NotificationResponse() {}

    public NotificationResponse(Notification n) {
        this.id = n.getId();
        if (n.getCaseEntity() != null) {
            this.caseId = n.getCaseEntity().getId();
            this.caseNumber = n.getCaseEntity().getCaseNumber();
            this.caseTitle = n.getCaseEntity().getTitle();
        }
        if (n.getRecipientOfficer() != null) {
            this.recipientOfficerId = n.getRecipientOfficer().getId();
            this.recipientName = n.getRecipientOfficer().getFullName();
        }
        this.recipientEmail = n.getRecipientEmail();
        this.recipientPhone = n.getRecipientPhone();
        this.channel = n.getChannel();
        this.notificationType = n.getNotificationType();
        this.subject = n.getSubject();
        this.message = n.getMessage();
        this.externalMessageId = n.getExternalMessageId();
        this.status = n.getStatus();
        this.attemptCount = n.getAttemptCount();
        this.lastError = n.getLastError();
        this.isRead = n.isRead();
        this.readAt = n.getReadAt();
        this.severity = n.getSeverity();
        this.referenceType = n.getReferenceType();
        this.referenceId = n.getReferenceId();
        this.sentAt = n.getSentAt();
        this.createdAt = n.getCreatedAt();
        this.updatedAt = n.getUpdatedAt();
    }

    public static NotificationResponse fromEntity(Notification n) {
        return n != null ? new NotificationResponse(n) : null;
    }

    @JsonProperty("isRead")
    public boolean isRead() {
        return isRead;
    }

    @JsonProperty("isTestEmail")
    public boolean isTestEmail() {
        return channel == NotificationChannel.EMAIL;
    }

    public boolean getIsRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(OffsetDateTime readAt) {
        this.readAt = readAt;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
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

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getCaseTitle() {
        return caseTitle;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

    public UUID getRecipientOfficerId() {
        return recipientOfficerId;
    }

    public void setRecipientOfficerId(UUID recipientOfficerId) {
        this.recipientOfficerId = recipientOfficerId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getExternalMessageId() {
        return externalMessageId;
    }

    public void setExternalMessageId(String externalMessageId) {
        this.externalMessageId = externalMessageId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
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
