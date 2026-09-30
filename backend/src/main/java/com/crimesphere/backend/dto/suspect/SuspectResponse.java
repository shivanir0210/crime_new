package com.crimesphere.backend.dto.suspect;

import com.crimesphere.backend.dto.OfficerDto;
import com.crimesphere.backend.entity.Suspect;

import java.time.OffsetDateTime;
import java.util.UUID;

public class SuspectResponse {

    private UUID id;
    private UUID caseId;
    private String fullName;
    private String alias;
    private Integer age;
    private String gender;
    private String phone;
    private String address;
    private String occupation;
    private String description;
    private String investigationStatus;
    private String photoUrl;
    private String defaultImageUrl;
    private String imageSource;
    private Boolean hasRealPhoto;
    private OfficerDto createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public SuspectResponse() {
    }

    public SuspectResponse(Suspect s) {
        if (s != null) {
            this.id = s.getId();
            if (s.getCaseEntity() != null) {
                this.caseId = s.getCaseEntity().getId();
            }
            this.fullName = s.getFullName();
            this.alias = s.getAlias();
            this.age = s.getAge();
            this.gender = s.getGender();
            this.phone = s.getPhone();
            this.address = s.getAddress();
            this.occupation = s.getOccupation();
            this.description = s.getDescription();
            this.investigationStatus = s.getInvestigationStatus();
            this.imageSource = s.getImageSource() != null ? s.getImageSource() : (s.getPhotoStoragePath() != null ? "UPLOADED" : "SYNTHETIC_DEMO");
            this.hasRealPhoto = "UPLOADED".equalsIgnoreCase(this.imageSource);
            this.photoUrl = s.getPhotoUrl();
            if (this.caseId != null && this.id != null) {
                this.defaultImageUrl = "/api/cases/" + this.caseId + "/suspects/" + this.id + "/default-image";
                if (this.photoUrl == null || this.photoUrl.isBlank()) {
                    this.photoUrl = "/api/cases/" + this.caseId + "/suspects/" + this.id + "/photo";
                }
            }
            if (s.getCreatedBy() != null) {
                this.createdBy = new OfficerDto(s.getCreatedBy());
            }
            this.createdAt = s.getCreatedAt();
            this.updatedAt = s.getUpdatedAt();
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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInvestigationStatus() {
        return investigationStatus;
    }

    public void setInvestigationStatus(String investigationStatus) {
        this.investigationStatus = investigationStatus;
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
