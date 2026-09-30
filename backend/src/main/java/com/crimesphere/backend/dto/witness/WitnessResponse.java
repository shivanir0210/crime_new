package com.crimesphere.backend.dto.witness;

import com.crimesphere.backend.dto.OfficerDto;
import com.crimesphere.backend.entity.Witness;

import java.time.OffsetDateTime;
import java.util.UUID;

public class WitnessResponse {

    private UUID id;
    private UUID caseId;
    private String fullName;
    private Integer age;
    private String gender;
    private String phone;
    private String address;
    private String occupation;
    private String statementSummary;
    private String witnessType;
    private String reliabilityScore;
    private OffsetDateTime interviewDate;
    private OfficerDto createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public WitnessResponse() {
    }

    public WitnessResponse(Witness w) {
        if (w != null) {
            this.id = w.getId();
            if (w.getCaseEntity() != null) {
                this.caseId = w.getCaseEntity().getId();
            }
            this.fullName = w.getFullName();
            this.age = w.getAge();
            this.gender = w.getGender();
            this.phone = w.getPhone();
            this.address = w.getAddress();
            this.occupation = w.getOccupation();
            this.statementSummary = w.getStatementSummary();
            this.witnessType = w.getWitnessType();
            this.reliabilityScore = w.getReliabilityScore();
            this.interviewDate = w.getInterviewDate();
            if (w.getCreatedBy() != null) {
                this.createdBy = new OfficerDto(w.getCreatedBy());
            }
            this.createdAt = w.getCreatedAt();
            this.updatedAt = w.getUpdatedAt();
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
