package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class RetrievedSourceDto {

    @JsonProperty("source_type")
    private String sourceType; // HISTORICAL_CASE, HISTORICAL_EVIDENCE, CURRENT_EVIDENCE, etc.

    @JsonProperty("source_id")
    private UUID sourceId;

    @JsonProperty("case_number")
    private String caseNumber;

    @JsonProperty("similarity")
    private double similarity;

    @JsonProperty("content_excerpt")
    private String contentExcerpt;

    public RetrievedSourceDto() {
    }

    public RetrievedSourceDto(String sourceType, UUID sourceId, String caseNumber, double similarity, String contentExcerpt) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.caseNumber = caseNumber;
        this.similarity = similarity;
        this.contentExcerpt = contentExcerpt;
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

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public String getContentExcerpt() {
        return contentExcerpt;
    }

    public void setContentExcerpt(String contentExcerpt) {
        this.contentExcerpt = contentExcerpt;
    }
}
