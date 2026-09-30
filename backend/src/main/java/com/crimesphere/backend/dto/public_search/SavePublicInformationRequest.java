package com.crimesphere.backend.dto.public_search;

import java.time.OffsetDateTime;

public class SavePublicInformationRequest {

    private String title;
    private String description;
    private String sourceName;
    private String sourceUrl;
    private OffsetDateTime publishedAt;
    private String author;
    private String imageUrl;
    private String originalQuery;
    private String notes;

    public SavePublicInformationRequest() {
    }

    public SavePublicInformationRequest(String title, String description, String sourceName, String sourceUrl,
                                        OffsetDateTime publishedAt, String author, String imageUrl,
                                        String originalQuery, String notes) {
        this.title = title;
        this.description = description;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.publishedAt = publishedAt;
        this.author = author;
        this.imageUrl = imageUrl;
        this.originalQuery = originalQuery;
        this.notes = notes;
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

    @com.fasterxml.jackson.annotation.JsonSetter("publishedAt")
    public void setPublishedAtFromObject(Object rawPublishedAt) {
        if (rawPublishedAt == null) {
            this.publishedAt = null;
            return;
        }
        if (rawPublishedAt instanceof OffsetDateTime) {
            this.publishedAt = (OffsetDateTime) rawPublishedAt;
            return;
        }
        String str = rawPublishedAt.toString().trim();
        if (str.isEmpty() || "null".equalsIgnoreCase(str)) {
            this.publishedAt = null;
            return;
        }
        try {
            this.publishedAt = OffsetDateTime.parse(str);
        } catch (Exception e1) {
            try {
                this.publishedAt = java.time.ZonedDateTime.parse(str).toOffsetDateTime();
            } catch (Exception e2) {
                try {
                    this.publishedAt = java.time.LocalDateTime.parse(str).atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime();
                } catch (Exception e3) {
                    try {
                        this.publishedAt = java.time.LocalDate.parse(str).atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime();
                    } catch (Exception e4) {
                        this.publishedAt = null;
                    }
                }
            }
        }
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
