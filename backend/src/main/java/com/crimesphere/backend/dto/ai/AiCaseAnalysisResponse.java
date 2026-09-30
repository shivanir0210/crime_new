package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AiCaseAnalysisResponse {

    @JsonProperty("requestId")
    @JsonAlias({"request_id", "requestId"})
    private String requestId;

    @JsonProperty("status")
    private String status = "REVIEW_REQUIRED";

    @JsonProperty("message")
    private String message;

    @JsonProperty("caseId")
    @JsonAlias({"case_id", "caseId"})
    private UUID caseId;

    @JsonProperty("caseNumber")
    @JsonAlias({"case_number", "caseNumber"})
    private String caseNumber;

    @JsonProperty("analysisAvailable")
    @JsonAlias({"analysis_available", "analysisAvailable"})
    private Boolean analysisAvailable = false;

    @JsonProperty("analysisId")
    @JsonAlias({"analysis_id", "analysisId"})
    private String analysisId;

    @JsonProperty("analysisVersion")
    @JsonAlias({"analysis_version", "analysisVersion"})
    private String analysisVersion;

    @JsonProperty("modelName")
    @JsonAlias({"model_name", "modelName"})
    private String modelName;

    @JsonProperty("promptVersion")
    @JsonAlias({"prompt_version", "promptVersion"})
    private String promptVersion;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("keyObservations")
    @JsonAlias({"key_observations", "keyObservations"})
    private List<AiKeyObservationDto> keyObservations;

    @JsonProperty("potentialLeads")
    @JsonAlias({"potential_leads", "potentialLeads"})
    private List<AiPotentialLeadDto> potentialLeads;

    @JsonProperty("evidenceGaps")
    @JsonAlias({"evidence_gaps", "evidenceGaps"})
    private List<AiEvidenceGapAnalysisDto> evidenceGaps;

    @JsonProperty("contradictions")
    private List<AiContradictionDto> contradictions;

    @JsonProperty("recommendedActions")
    @JsonAlias({"recommended_actions", "recommendedActions"})
    private List<AiRecommendedActionDto> recommendedActions;

    @JsonProperty("uncertainties")
    private List<String> uncertainties;

    @JsonProperty("limitations")
    private List<String> limitations;

    @JsonProperty("retrievedSources")
    @JsonAlias({"retrieved_sources", "retrievedSources"})
    private List<RetrievedSourceDto> retrievedSources;

    @JsonProperty("createdAt")
    @JsonAlias({"created_at", "createdAt"})
    private OffsetDateTime createdAt;

    public AiCaseAnalysisResponse() {
    }


    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getAnalysisAvailable() {
        if (analysisAvailable != null && analysisAvailable) {
            return true;
        }
        return (summary != null && !summary.isBlank()) || (analysisId != null && !analysisId.isBlank());
    }

    public void setAnalysisAvailable(Boolean analysisAvailable) {
        this.analysisAvailable = analysisAvailable;
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

    public String getAnalysisVersion() {
        return analysisVersion;
    }

    public void setAnalysisVersion(String analysisVersion) {
        this.analysisVersion = analysisVersion;
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<AiKeyObservationDto> getKeyObservations() {
        if (keyObservations == null) keyObservations = new ArrayList<>();
        return keyObservations;
    }

    public void setKeyObservations(List<AiKeyObservationDto> keyObservations) {
        this.keyObservations = keyObservations;
    }

    public List<AiPotentialLeadDto> getPotentialLeads() {
        if (potentialLeads == null) potentialLeads = new ArrayList<>();
        return potentialLeads;
    }

    public void setPotentialLeads(List<AiPotentialLeadDto> potentialLeads) {
        this.potentialLeads = potentialLeads;
    }

    public List<AiEvidenceGapAnalysisDto> getEvidenceGaps() {
        if (evidenceGaps == null) evidenceGaps = new ArrayList<>();
        return evidenceGaps;
    }

    public void setEvidenceGaps(List<AiEvidenceGapAnalysisDto> evidenceGaps) {
        this.evidenceGaps = evidenceGaps;
    }

    public List<AiContradictionDto> getContradictions() {
        if (contradictions == null) contradictions = new ArrayList<>();
        return contradictions;
    }

    public void setContradictions(List<AiContradictionDto> contradictions) {
        this.contradictions = contradictions;
    }

    public List<AiRecommendedActionDto> getRecommendedActions() {
        if (recommendedActions == null) recommendedActions = new ArrayList<>();
        return recommendedActions;
    }

    public void setRecommendedActions(List<AiRecommendedActionDto> recommendedActions) {
        this.recommendedActions = recommendedActions;
    }

    public List<String> getUncertainties() {
        if (uncertainties == null) uncertainties = new ArrayList<>();
        return uncertainties;
    }

    public void setUncertainties(List<String> uncertainties) {
        this.uncertainties = uncertainties;
    }

    public List<String> getLimitations() {
        if (limitations == null) limitations = new ArrayList<>();
        return limitations;
    }

    public void setLimitations(List<String> limitations) {
        this.limitations = limitations;
    }

    public List<RetrievedSourceDto> getRetrievedSources() {
        if (retrievedSources == null) retrievedSources = new ArrayList<>();
        return retrievedSources;
    }

    public void setRetrievedSources(List<RetrievedSourceDto> retrievedSources) {
        this.retrievedSources = retrievedSources;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
