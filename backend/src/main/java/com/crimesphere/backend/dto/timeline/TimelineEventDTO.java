package com.crimesphere.backend.dto.timeline;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class TimelineEventDTO {

    private String id;
    private UUID caseId;
    private String title;
    private String description;
    private OffsetDateTime timestamp;
    private String eventType;
    private String badgeColor;
    private String actor;
    private Map<String, String> metadata;

    public TimelineEventDTO() {
    }

    public TimelineEventDTO(String id, UUID caseId, String title, String description,
                            OffsetDateTime timestamp, String eventType, String badgeColor,
                            String actor, Map<String, String> metadata) {
        this.id = id;
        this.caseId = caseId;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.badgeColor = badgeColor;
        this.actor = actor;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
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

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public void setBadgeColor(String badgeColor) {
        this.badgeColor = badgeColor;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
