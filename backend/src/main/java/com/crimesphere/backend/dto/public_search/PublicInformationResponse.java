package com.crimesphere.backend.dto.public_search;

import com.crimesphere.backend.entity.PublicInformationItem;
import com.crimesphere.backend.entity.PublicInformationVerificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PublicInformationResponse {

    private UUID id;
    private UUID caseId;
    private String caseNumber;
    private String title;
    private String description;
    private String sourceName;
    private String sourceUrl;
    private OffsetDateTime publishedAt;
    private String author;
    private String imageUrl;
    private String originalQuery;
    private PublicInformationVerificationStatus verificationStatus;
    private String notes;
    private UUID savedById;
    private String savedByOfficerName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String warningDisclaimer = "Public Information — Requires Verification. External public information is informational only and is not automatically considered evidence.";

    public PublicInformationResponse() {
    }

    public static PublicInformationResponse fromEntity(PublicInformationItem item) {
        PublicInformationResponse resp = new PublicInformationResponse();
        resp.setId(item.getId());
        if (item.getCrimeCase() != null) {
            resp.setCaseId(item.getCrimeCase().getId());
            resp.setCaseNumber(item.getCrimeCase().getCaseNumber());
        }
        resp.setTitle(item.getTitle());
        resp.setDescription(item.getDescription());
        resp.setSourceName(item.getSourceName());
        resp.setSourceUrl(item.getSourceUrl());
        resp.setPublishedAt(item.getPublishedAt());
        resp.setAuthor(item.getAuthor());
        resp.setImageUrl(item.getImageUrl());
        resp.setOriginalQuery(item.getOriginalQuery());
        resp.setVerificationStatus(item.getVerificationStatus());
        resp.setNotes(item.getNotes());
        if (item.getSavedBy() != null) {
            resp.setSavedById(item.getSavedBy().getId());
            resp.setSavedByOfficerName(item.getSavedBy().getFullName());
        }
        resp.setCreatedAt(item.getCreatedAt());
        resp.setUpdatedAt(item.getUpdatedAt());
        return resp;
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

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getOriginalQuery() {
        return originalQuery;
    }

    public void setOriginalQuery(String originalQuery) {
        this.originalQuery = originalQuery;
    }

    public PublicInformationVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(PublicInformationVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getSavedById() {
        return savedById;
    }

    public void setSavedById(UUID savedById) {
        this.savedById = savedById;
    }

    public String getSavedByOfficerName() {
        return savedByOfficerName;
    }

    public void setSavedByOfficerName(String savedByOfficerName) {
        this.savedByOfficerName = savedByOfficerName;
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

    public String getWarningDisclaimer() {
        return warningDisclaimer;
    }

    public void setWarningDisclaimer(String warningDisclaimer) {
        this.warningDisclaimer = warningDisclaimer;
    }
}
