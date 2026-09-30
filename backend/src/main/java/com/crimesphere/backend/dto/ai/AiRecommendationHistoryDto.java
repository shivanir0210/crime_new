package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AiRecommendationHistoryDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("case_id")
    private UUID caseId;

    @JsonProperty("analysis_version")
    private String analysisVersion;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("prompt_version")
    private String promptVersion;

    @JsonProperty("status")
    private String status;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("created_by_officer")
    private String createdByOfficer;

    @JsonProperty("items_count")
    private int itemsCount;

    public AiRecommendationHistoryDto() {
    }

    public AiRecommendationHistoryDto(UUID id, UUID caseId, String analysisVersion, String summary,
                                      String modelName, String promptVersion, String status,
                                      OffsetDateTime createdAt, String createdByOfficer, int itemsCount) {
        this.id = id;
        this.caseId = caseId;
        this.analysisVersion = analysisVersion;
        this.summary = summary;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.status = status;
        this.createdAt = createdAt;
        this.createdByOfficer = createdByOfficer;
        this.itemsCount = itemsCount;
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

    public String getAnalysisVersion() {
        return analysisVersion;
    }

    public void setAnalysisVersion(String analysisVersion) {
        this.analysisVersion = analysisVersion;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedByOfficer() {
        return createdByOfficer;
    }

    public void setCreatedByOfficer(String createdByOfficer) {
        this.createdByOfficer = createdByOfficer;
    }

    public int getItemsCount() {
        return itemsCount;
    }

    public void setItemsCount(int itemsCount) {
        this.itemsCount = itemsCount;
    }
}
