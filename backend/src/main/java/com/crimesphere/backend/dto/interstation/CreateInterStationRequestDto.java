package com.crimesphere.backend.dto.interstation;

import java.util.UUID;

public class CreateInterStationRequestDto {

    private UUID caseId;
    private UUID victimId;
    private UUID destinationStationId;
    private String reason;
    private Boolean shareCaseReference = true;
    private Boolean shareVictimDetails = true;
    private Boolean shareIncidentSummary = true;
    private Boolean shareEvidence = false;
    private Boolean shareWitnessInfo = false;

    public CreateInterStationRequestDto() {
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getVictimId() {
        return victimId;
    }

    public void setVictimId(UUID victimId) {
        this.victimId = victimId;
    }

    public UUID getDestinationStationId() {
        return destinationStationId;
    }

    public void setDestinationStationId(UUID destinationStationId) {
        this.destinationStationId = destinationStationId;
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
}
