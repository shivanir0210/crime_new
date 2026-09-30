package com.crimesphere.backend.dto.fir;

import java.time.OffsetDateTime;

public class CreateFIRRequest {

    private String complainantName;
    private String complainantContact;
    private OffsetDateTime registrationDate;
    private OffsetDateTime incidentDate;
    private String incidentLocation;
    private String offenceType;
    private String sectionsOfLaw;
    private String description;

    public CreateFIRRequest() {
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

    public OffsetDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(OffsetDateTime registrationDate) {
        this.registrationDate = registrationDate;
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
}
