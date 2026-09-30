package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AiFirDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("fir_number")
    private String firNumber;

    @JsonProperty("police_station")
    private String policeStation;

    @JsonProperty("registration_date")
    private OffsetDateTime registrationDate;

    @JsonProperty("incident_date")
    private OffsetDateTime incidentDate;

    @JsonProperty("incident_location")
    private String incidentLocation;

    @JsonProperty("offence_type")
    private String offenceType;

    @JsonProperty("sections_of_law")
    private String sectionsOfLaw;

    @JsonProperty("description")
    private String description;

    public AiFirDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
