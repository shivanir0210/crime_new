package com.crimesphere.backend.dto.fir;

import com.crimesphere.backend.dto.OfficerDto;
import com.crimesphere.backend.entity.FIR;

import java.time.OffsetDateTime;
import java.util.UUID;

public class FIRResponse {

    private UUID id;
    private UUID caseId;
    private String firNumber;
    private String policeStation;
    private OffsetDateTime registrationDate;
    private String complainantName;
    private String complainantContact;
    private OffsetDateTime incidentDate;
    private String incidentLocation;
    private String offenceType;
    private String sectionsOfLaw;
    private String description;
    private OfficerDto createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public FIRResponse() {
    }

    public FIRResponse(FIR fir) {
        if (fir != null) {
            this.id = fir.getId();
            if (fir.getCaseEntity() != null) {
                this.caseId = fir.getCaseEntity().getId();
            }
            this.firNumber = fir.getFirNumber();
            this.policeStation = fir.getPoliceStation();
            this.registrationDate = fir.getRegistrationDate();
            this.complainantName = fir.getComplainantName();
            this.complainantContact = fir.getComplainantContact();
            this.incidentDate = fir.getIncidentDate();
            this.incidentLocation = fir.getIncidentLocation();
            this.offenceType = fir.getOffenceType();
            this.sectionsOfLaw = fir.getSectionsOfLaw();
            this.description = fir.getDescription();
            if (fir.getCreatedBy() != null) {
                this.createdBy = new OfficerDto(fir.getCreatedBy());
            }
            this.createdAt = fir.getCreatedAt();
            this.updatedAt = fir.getUpdatedAt();
        }
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

    public String getFirNumber() {
        return firNumber;
    }

    public void setFirNumber(String firNumber) {
        this.firNumber = firNumber;
    }

    public String getPoliceStation() {
        return policeStation;
    }

    public void setPoliceStation(String policeStation) {
        this.policeStation = policeStation;
    }

    public OffsetDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(OffsetDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getComplainantName() {
        return complainantName;
    }

    public void setComplainantName(String complainantName) {
        this.complainantName = complainantName;
    }

    public String getComplainantContact() {
        return complainantContact;
    }

    public void setComplainantContact(String complainantContact) {
        this.complainantContact = complainantContact;
    }

    public OffsetDateTime getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(OffsetDateTime incidentDate) {
        this.incidentDate = incidentDate;
    }

    public String getIncidentLocation() {
        return incidentLocation;
    }

    public void setIncidentLocation(String incidentLocation) {
        this.incidentLocation = incidentLocation;
    }

    public String getOffenceType() {
        return offenceType;
    }

    public void setOffenceType(String offenceType) {
        this.offenceType = offenceType;
    }

    public String getSectionsOfLaw() {
        return sectionsOfLaw;
    }

    public void setSectionsOfLaw(String sectionsOfLaw) {
        this.sectionsOfLaw = sectionsOfLaw;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OfficerDto getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(OfficerDto createdBy) {
        this.createdBy = createdBy;
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
