package com.crimesphere.backend.dto.interstation;

import com.crimesphere.backend.entity.InterStationRequest;

import java.time.OffsetDateTime;
import java.util.UUID;

public class InterStationRequestResponseDto {

    private UUID id;
    private UUID caseId;
    private String caseNumber;
    private String caseTitle;
    private String casePriority;
    private String caseStatus;

    private UUID victimId;
    private String victimName;
    private String victimStatus;
    private String victimAddress;

    private UUID sourceStationId;
    private String sourceStationCode;
    private String sourceStationName;
    private String sourceDistrict;
    private String sourceState;

    private UUID destinationStationId;
    private String destinationStationCode;
    private String destinationStationName;
    private String destinationDistrict;
    private String destinationState;
    private String destinationHeadName;
    private String destinationHeadEmail;

    private UUID requestedByOfficerId;
    private String requestedByPoliceId;
    private String requestedByOfficerName;
    private String requestedByOfficerRole;

    private String reason;
    private Boolean shareCaseReference;
    private Boolean shareVictimDetails;
    private Boolean shareIncidentSummary;
    private Boolean shareEvidence;
    private Boolean shareWitnessInfo;
    private String sharedDataSnapshot;

    private String status;
    private String emailDeliveryStatus;
    private String emailDeliveryError;
    private String providerMessageId;

    private String responseNotes;
    private UUID respondedByOfficerId;
    private String respondedByPoliceId;
    private String respondedByOfficerName;
    private OffsetDateTime respondedAt;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public InterStationRequestResponseDto() {
    }

    public InterStationRequestResponseDto(InterStationRequest entity) {
        if (entity == null) return;
        this.id = entity.getId();

        if (entity.getCaseEntity() != null) {
            this.caseId = entity.getCaseEntity().getId();
            this.caseNumber = entity.getCaseEntity().getCaseNumber();
            this.caseTitle = entity.getCaseEntity().getTitle();
            this.casePriority = entity.getCaseEntity().getPriority();
            this.caseStatus = entity.getCaseEntity().getStatus();
        }

        if (entity.getVictim() != null) {
            this.victimId = entity.getVictim().getId();
            this.victimName = entity.getVictim().getFullName();
            this.victimStatus = entity.getVictim().getStatus();
            this.victimAddress = entity.getVictim().getAddress();
        }

        if (entity.getSourceStation() != null) {
            this.sourceStationId = entity.getSourceStation().getId();
            this.sourceStationCode = entity.getSourceStation().getStationCode();
            this.sourceStationName = entity.getSourceStation().getStationName();
            this.sourceDistrict = entity.getSourceStation().getDistrict();
            this.sourceState = entity.getSourceStation().getState();
        }

        if (entity.getDestinationStation() != null) {
            this.destinationStationId = entity.getDestinationStation().getId();
            this.destinationStationCode = entity.getDestinationStation().getStationCode();
            this.destinationStationName = entity.getDestinationStation().getStationName();
            this.destinationDistrict = entity.getDestinationStation().getDistrict();
            this.destinationState = entity.getDestinationStation().getState();
            this.destinationHeadName = entity.getDestinationStation().getHeadName();
            this.destinationHeadEmail = entity.getDestinationStation().getHeadEmail();
        }

        if (entity.getRequestedByOfficer() != null) {
            this.requestedByOfficerId = entity.getRequestedByOfficer().getId();
            this.requestedByPoliceId = entity.getRequestedByOfficer().getPoliceId();
            this.requestedByOfficerName = entity.getRequestedByOfficer().getFullName();
            this.requestedByOfficerRole = entity.getRequestedByOfficer().getRole();
        }

        this.reason = entity.getReason();
        this.shareCaseReference = entity.getShareCaseReference();
        this.shareVictimDetails = entity.getShareVictimDetails();
        this.shareIncidentSummary = entity.getShareIncidentSummary();
        this.shareEvidence = entity.getShareEvidence();
        this.shareWitnessInfo = entity.getShareWitnessInfo();
        this.sharedDataSnapshot = entity.getSharedDataSnapshot();

        this.status = entity.getStatus();
        this.emailDeliveryStatus = entity.getEmailDeliveryStatus();
        this.emailDeliveryError = entity.getEmailDeliveryError();
        this.providerMessageId = entity.getProviderMessageId();

        this.responseNotes = entity.getResponseNotes();
        if (entity.getRespondedByOfficer() != null) {
            this.respondedByOfficerId = entity.getRespondedByOfficer().getId();
            this.respondedByPoliceId = entity.getRespondedByOfficer().getPoliceId();
            this.respondedByOfficerName = entity.getRespondedByOfficer().getFullName();
        }
        this.respondedAt = entity.getRespondedAt();

        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
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

    public String getCasePriority() {
        return casePriority;
    }

    public void setCasePriority(String casePriority) {
        this.casePriority = casePriority;
    }

    public String getCaseStatus() {
        return caseStatus;
    }

    public void setCaseStatus(String caseStatus) {
        this.caseStatus = caseStatus;
    }

    public UUID getVictimId() {
        return victimId;
    }

    public void setVictimId(UUID victimId) {
        this.victimId = victimId;
    }

    public String getVictimName() {
        return victimName;
    }

    public void setVictimName(String victimName) {
        this.victimName = victimName;
    }

    public String getVictimStatus() {
        return victimStatus;
    }

    public void setVictimStatus(String victimStatus) {
        this.victimStatus = victimStatus;
    }

    public String getVictimAddress() {
        return victimAddress;
    }

    public void setVictimAddress(String victimAddress) {
        this.victimAddress = victimAddress;
    }

    public UUID getSourceStationId() {
        return sourceStationId;
    }

    public void setSourceStationId(UUID sourceStationId) {
        this.sourceStationId = sourceStationId;
    }

    public String getSourceStationCode() {
        return sourceStationCode;
    }

    public void setSourceStationCode(String sourceStationCode) {
        this.sourceStationCode = sourceStationCode;
    }

    public String getSourceStationName() {
        return sourceStationName;
    }

    public void setSourceStationName(String sourceStationName) {
        this.sourceStationName = sourceStationName;
    }

    public String getSourceDistrict() {
        return sourceDistrict;
    }

    public void setSourceDistrict(String sourceDistrict) {
        this.sourceDistrict = sourceDistrict;
    }

    public String getSourceState() {
        return sourceState;
    }

    public void setSourceState(String sourceState) {
        this.sourceState = sourceState;
    }

    public UUID getDestinationStationId() {
        return destinationStationId;
    }

    public void setDestinationStationId(UUID destinationStationId) {
        this.destinationStationId = destinationStationId;
    }

    public String getDestinationStationCode() {
        return destinationStationCode;
    }

    public void setDestinationStationCode(String destinationStationCode) {
        this.destinationStationCode = destinationStationCode;
    }

    public String getDestinationStationName() {
        return destinationStationName;
    }

    public void setDestinationStationName(String destinationStationName) {
        this.destinationStationName = destinationStationName;
    }

    public String getDestinationDistrict() {
        return destinationDistrict;
    }

    public void setDestinationDistrict(String destinationDistrict) {
        this.destinationDistrict = destinationDistrict;
    }

    public String getDestinationState() {
        return destinationState;
    }

    public void setDestinationState(String destinationState) {
        this.destinationState = destinationState;
    }

    public String getDestinationHeadName() {
        return destinationHeadName;
    }

    public void setDestinationHeadName(String destinationHeadName) {
        this.destinationHeadName = destinationHeadName;
    }

    public String getDestinationHeadEmail() {
        return destinationHeadEmail;
    }

    public void setDestinationHeadEmail(String destinationHeadEmail) {
        this.destinationHeadEmail = destinationHeadEmail;
    }

    public UUID getRequestedByOfficerId() {
        return requestedByOfficerId;
    }

    public void setRequestedByOfficerId(UUID requestedByOfficerId) {
        this.requestedByOfficerId = requestedByOfficerId;
    }

    public String getRequestedByPoliceId() {
        return requestedByPoliceId;
    }

    public void setRequestedByPoliceId(String requestedByPoliceId) {
        this.requestedByPoliceId = requestedByPoliceId;
    }

    public String getRequestedByOfficerName() {
        return requestedByOfficerName;
    }

    public void setRequestedByOfficerName(String requestedByOfficerName) {
        this.requestedByOfficerName = requestedByOfficerName;
    }

    public String getRequestedByOfficerRole() {
        return requestedByOfficerRole;
    }

    public void setRequestedByOfficerRole(String requestedByOfficerRole) {
        this.requestedByOfficerRole = requestedByOfficerRole;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean getShareCaseReference() {
        return shareCaseReference;
    }

    public void setShareCaseReference(Boolean shareCaseReference) {
        this.shareCaseReference = shareCaseReference;
    }

    public Boolean getShareVictimDetails() {
        return shareVictimDetails;
    }

    public void setShareVictimDetails(Boolean shareVictimDetails) {
        this.shareVictimDetails = shareVictimDetails;
    }

    public Boolean getShareIncidentSummary() {
        return shareIncidentSummary;
    }

    public void setShareIncidentSummary(Boolean shareIncidentSummary) {
        this.shareIncidentSummary = shareIncidentSummary;
    }

    public Boolean getShareEvidence() {
        return shareEvidence;
    }

    public void setShareEvidence(Boolean shareEvidence) {
        this.shareEvidence = shareEvidence;
    }

    public Boolean getShareWitnessInfo() {
        return shareWitnessInfo;
    }

    public void setShareWitnessInfo(Boolean shareWitnessInfo) {
        this.shareWitnessInfo = shareWitnessInfo;
    }

    public String getSharedDataSnapshot() {
        return sharedDataSnapshot;
    }

    public void setSharedDataSnapshot(String sharedDataSnapshot) {
        this.sharedDataSnapshot = sharedDataSnapshot;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmailDeliveryStatus() {
        return emailDeliveryStatus;
    }

    public void setEmailDeliveryStatus(String emailDeliveryStatus) {
        this.emailDeliveryStatus = emailDeliveryStatus;
    }

    public String getEmailDeliveryError() {
        return emailDeliveryError;
    }

    public void setEmailDeliveryError(String emailDeliveryError) {
        this.emailDeliveryError = emailDeliveryError;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getResponseNotes() {
        return responseNotes;
    }

    public void setResponseNotes(String responseNotes) {
        this.responseNotes = responseNotes;
    }

    public UUID getRespondedByOfficerId() {
        return respondedByOfficerId;
    }

    public void setRespondedByOfficerId(UUID respondedByOfficerId) {
        this.respondedByOfficerId = respondedByOfficerId;
    }

    public String getRespondedByPoliceId() {
        return respondedByPoliceId;
    }

    public void setRespondedByPoliceId(String respondedByPoliceId) {
        this.respondedByPoliceId = respondedByPoliceId;
    }

    public String getRespondedByOfficerName() {
        return respondedByOfficerName;
    }

    public void setRespondedByOfficerName(String respondedByOfficerName) {
        this.respondedByOfficerName = respondedByOfficerName;
    }

    public OffsetDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(OffsetDateTime respondedAt) {
        this.respondedAt = respondedAt;
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
