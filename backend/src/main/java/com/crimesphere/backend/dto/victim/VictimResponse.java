package com.crimesphere.backend.dto.victim;

import com.crimesphere.backend.dto.OfficerDto;
import com.crimesphere.backend.entity.Victim;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class VictimResponse {

    private UUID id;
    private UUID caseId;
    private String fullName;
    private Integer age;
    private String gender;
    private LocalDate dateOfBirth;
    private String phone;
    private String address;
    private String occupation;
    private String relationshipToCase;
    private String description;
    private String status;
    private String photoUrl;
    private String defaultImageUrl;
    private String imageSource;
    private Boolean hasRealPhoto;
    private OfficerDto createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public VictimResponse() {
    }

    public VictimResponse(Victim v) {
        if (v != null) {
            this.id = v.getId();
            if (v.getCaseEntity() != null) {
                this.caseId = v.getCaseEntity().getId();
            }
            this.fullName = v.getFullName();
            this.age = v.getAge();
            this.gender = v.getGender();
            this.dateOfBirth = v.getDateOfBirth();
            this.phone = v.getPhone();
            this.address = v.getAddress();
            this.occupation = v.getOccupation();
            this.relationshipToCase = v.getRelationshipToCase();
            this.description = v.getDescription();
            this.status = v.getStatus();
            this.imageSource = v.getImageSource() != null ? v.getImageSource() : (v.getPhotoStoragePath() != null ? "UPLOADED" : "SYNTHETIC_DEMO");
            this.hasRealPhoto = "UPLOADED".equalsIgnoreCase(this.imageSource);
            this.photoUrl = v.getPhotoUrl();
            if (this.caseId != null && this.id != null) {
                this.defaultImageUrl = "/api/cases/" + this.caseId + "/victims/" + this.id + "/default-image";
                if (this.photoUrl == null || this.photoUrl.isBlank()) {
                    this.photoUrl = "/api/cases/" + this.caseId + "/victims/" + this.id + "/photo";
                }
            }
            if (v.getCreatedBy() != null) {
                this.createdBy = new OfficerDto(v.getCreatedBy());
            }
            this.createdAt = v.getCreatedAt();
            this.updatedAt = v.getUpdatedAt();
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
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

    public String getRelationshipToCase() {
        return relationshipToCase;
    }

    public void setRelationshipToCase(String relationshipToCase) {
        this.relationshipToCase = relationshipToCase;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getDefaultImageUrl() {
        return defaultImageUrl;
    }

    public void setDefaultImageUrl(String defaultImageUrl) {
        this.defaultImageUrl = defaultImageUrl;
    }

    public String getImageSource() {
        return imageSource;
    }

    public void setImageSource(String imageSource) {
        this.imageSource = imageSource;
    }

    public Boolean getHasRealPhoto() {
        return hasRealPhoto;
    }

    public void setHasRealPhoto(Boolean hasRealPhoto) {
        this.hasRealPhoto = hasRealPhoto;
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
