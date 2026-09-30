package com.crimesphere.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "firs")
public class FIR {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", unique = true, nullable = false)
    private Case caseEntity;

    @Column(name = "fir_number", unique = true, nullable = false, length = 100)
    private String firNumber;

    @Column(name = "police_station")
    private String policeStation;

    @Column(name = "registration_date")
    private OffsetDateTime registrationDate;

    @Column(name = "complainant_name")
    private String complainantName;

    @Column(name = "complainant_contact", length = 100)
    private String complainantContact;

    @Column(name = "incident_date")
    private OffsetDateTime incidentDate;

    @Column(name = "incident_location")
    private String incidentLocation;

    @Column(name = "offence_type", length = 100)
    private String offenceType;

    @Column(name = "sections_of_law", columnDefinition = "TEXT")
    private String sectionsOfLaw;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private PoliceOfficer createdBy;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = OffsetDateTime.now();
        }
        if (this.registrationDate == null) {
            this.registrationDate = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public FIR() {
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

    public PoliceOfficer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(PoliceOfficer createdBy) {
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
