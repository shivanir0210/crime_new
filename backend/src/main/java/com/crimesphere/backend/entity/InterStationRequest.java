package com.crimesphere.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inter_station_requests")
public class InterStationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "victim_id")
    private Victim victim;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_station_id", nullable = false)
    private PoliceStation sourceStation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_station_id", nullable = false)
    private PoliceStation destinationStation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_officer_id", nullable = false)
    private PoliceOfficer requestedByOfficer;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "share_case_reference")
    private Boolean shareCaseReference = true;

    @Column(name = "share_victim_details")
    private Boolean shareVictimDetails = true;

    @Column(name = "share_incident_summary")
    private Boolean shareIncidentSummary = true;

    @Column(name = "share_evidence")
    private Boolean shareEvidence = false;

    @Column(name = "share_witness_info")
    private Boolean shareWitnessInfo = false;

    @Column(name = "shared_data_snapshot", columnDefinition = "TEXT")
    private String sharedDataSnapshot;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "SENT"; // SENT, DELIVERY_FAILED, ACCEPTED, REJECTED, IN_PROGRESS, COMPLETED

    @Column(name = "email_delivery_status", length = 50)
    private String emailDeliveryStatus = "PENDING"; // SENT, FAILED, PENDING

    @Column(name = "email_delivery_error", columnDefinition = "TEXT")
    private String emailDeliveryError;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "response_notes", columnDefinition = "TEXT")
    private String responseNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by_officer_id")
    private PoliceOfficer respondedByOfficer;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public InterStationRequest() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Case getCaseEntity() {
        return caseEntity;
    }

    public void setCaseEntity(Case caseEntity) {
        this.caseEntity = caseEntity;
    }

    public Victim getVictim() {
        return victim;
    }

    public void setVictim(Victim victim) {
        this.victim = victim;
    }

    public PoliceStation getSourceStation() {
        return sourceStation;
    }

    public void setSourceStation(PoliceStation sourceStation) {
        this.sourceStation = sourceStation;
    }

    public PoliceStation getDestinationStation() {
        return destinationStation;
    }

    public void setDestinationStation(PoliceStation destinationStation) {
        this.destinationStation = destinationStation;
    }

    public PoliceOfficer getRequestedByOfficer() {
        return requestedByOfficer;
    }

    public void setRequestedByOfficer(PoliceOfficer requestedByOfficer) {
        this.requestedByOfficer = requestedByOfficer;
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

    public PoliceOfficer getRespondedByOfficer() {
        return respondedByOfficer;
    }

    public void setRespondedByOfficer(PoliceOfficer respondedByOfficer) {
        this.respondedByOfficer = respondedByOfficer;
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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
