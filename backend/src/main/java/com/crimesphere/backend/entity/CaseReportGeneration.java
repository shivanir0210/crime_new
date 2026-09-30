package com.crimesphere.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "case_report_generations")
public class CaseReportGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case crimeCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by", nullable = false)
    private PoliceOfficer generatedBy;

    @Column(name = "report_version", nullable = false, length = 50)
    private String reportVersion;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public CaseReportGeneration() {
    }

    public CaseReportGeneration(UUID id, Case crimeCase, PoliceOfficer generatedBy, String reportVersion, String fileName) {
        this.id = id;
        this.crimeCase = crimeCase;
        this.generatedBy = generatedBy;
        this.reportVersion = reportVersion;
        this.fileName = fileName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Case getCrimeCase() {
        return crimeCase;
    }

    public void setCrimeCase(Case crimeCase) {
        this.crimeCase = crimeCase;
    }

    public PoliceOfficer getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(PoliceOfficer generatedBy) {
        this.generatedBy = generatedBy;
    }

    public String getReportVersion() {
        return reportVersion;
    }

    public void setReportVersion(String reportVersion) {
        this.reportVersion = reportVersion;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
