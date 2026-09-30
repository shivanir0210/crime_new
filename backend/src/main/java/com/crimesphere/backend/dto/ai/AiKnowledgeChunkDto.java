package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.UUID;

public class AiKnowledgeChunkDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("source_type")
    private String sourceType;

    @JsonProperty("source_id")
    private UUID sourceId;

    @JsonProperty("case_id")
    private UUID caseId;

    @JsonProperty("case_number")
    private String caseNumber;

    @JsonProperty("case_type")
    private String caseType;

    @JsonProperty("content")
    private String content;

    @JsonProperty("content_hash")
    private String contentHash;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    public AiKnowledgeChunkDto() {
    }

    public AiKnowledgeChunkDto(String sourceType, UUID sourceId, UUID caseId, String caseNumber,
                              String caseType, String content, String contentHash, Map<String, Object> metadata) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.caseId = caseId;
        this.caseNumber = caseNumber;
        this.caseType = caseType;
        this.content = content;
        this.contentHash = contentHash;
        this.metadata = metadata;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
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

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
