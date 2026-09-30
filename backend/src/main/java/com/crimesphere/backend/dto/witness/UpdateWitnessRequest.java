package com.crimesphere.backend.dto.witness;

import java.time.OffsetDateTime;

public class UpdateWitnessRequest {

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

    public UpdateWitnessRequest() {
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
}
