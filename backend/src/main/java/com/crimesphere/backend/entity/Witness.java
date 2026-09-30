package com.crimesphere.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "witnesses")
public class Witness {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "age")
    private Integer age;

    @Column(name = "gender", length = 50)
    private String gender;

    @Column(name = "phone", length = 100)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "statement_summary", columnDefinition = "TEXT")
    private String statementSummary;

    @Column(name = "witness_type", length = 100)
    private String witnessType = "EYEWITNESS";

    @Column(name = "reliability_score", length = 50)
    private String reliabilityScore = "MEDIUM";

    @Column(name = "interview_date")
    private OffsetDateTime interviewDate;

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
        if (this.witnessType == null || this.witnessType.isBlank()) {
            this.witnessType = "EYEWITNESS";
        }
        if (this.reliabilityScore == null || this.reliabilityScore.isBlank()) {
            this.reliabilityScore = "MEDIUM";
        }
        if (this.interviewDate == null) {
            this.interviewDate = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Witness() {
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getStatementSummary() {
        return statementSummary;
    }

    public void setStatementSummary(String statementSummary) {
        this.statementSummary = statementSummary;
    }

    public String getWitnessType() {
        return witnessType;
    }

    public void setWitnessType(String witnessType) {
        this.witnessType = witnessType;
    }

    public String getReliabilityScore() {
        return reliabilityScore;
    }

    public void setReliabilityScore(String reliabilityScore) {
        this.reliabilityScore = reliabilityScore;
    }

    public OffsetDateTime getInterviewDate() {
        return interviewDate;
    }

    public void setInterviewDate(OffsetDateTime interviewDate) {
        this.interviewDate = interviewDate;
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
