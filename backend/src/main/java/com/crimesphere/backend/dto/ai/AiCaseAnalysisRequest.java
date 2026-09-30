package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AiCaseAnalysisRequest {

    @JsonProperty("caseId")
    @com.fasterxml.jackson.annotation.JsonAlias({"case_id", "caseId"})
    private UUID caseId;

    @JsonProperty("caseNumber")
    @com.fasterxml.jackson.annotation.JsonAlias({"case_number", "caseNumber"})
    private String caseNumber;

    @JsonProperty("title")
    @com.fasterxml.jackson.annotation.JsonAlias({"case_title", "caseTitle", "title"})
    private String title;

    @JsonProperty("caseTitle")
    @com.fasterxml.jackson.annotation.JsonAlias({"case_title", "caseTitle", "title"})
    private String caseTitle;

    @JsonProperty("description")
    private String description;

    @JsonProperty("caseType")
    @com.fasterxml.jackson.annotation.JsonAlias({"case_type", "caseType"})
    private String caseType = "OTHER";

    @JsonProperty("priority")
    private String priority = "MEDIUM";

    @JsonProperty("status")
    @com.fasterxml.jackson.annotation.JsonAlias({"case_status", "caseStatus", "status"})
    private String status = "OPEN";

    @JsonProperty("caseStatus")
    @com.fasterxml.jackson.annotation.JsonAlias({"case_status", "caseStatus", "status"})
    private String caseStatus = "OPEN";

    @JsonProperty("investigationState")
    @com.fasterxml.jackson.annotation.JsonAlias({"investigation_state", "investigationState"})
    private String investigationState = "CASE_CREATED";

    @JsonProperty("investigationCompleteness")
    @com.fasterxml.jackson.annotation.JsonAlias({"investigation_completeness", "investigationCompleteness"})
    private Integer investigationCompleteness = 0;

    @JsonProperty("fir")
    @com.fasterxml.jackson.annotation.JsonAlias({"FIR", "fir"})
    private AiFirDto fir;

    @JsonProperty("victims")
    private List<AiVictimDto> victims = new ArrayList<>();

    @JsonProperty("suspects")
    private List<AiSuspectDto> suspects = new ArrayList<>();

    @JsonProperty("evidence")
    private List<AiEvidenceDto> evidence = new ArrayList<>();

    @JsonProperty("evidenceGaps")
    @com.fasterxml.jackson.annotation.JsonAlias({"evidence_gaps", "evidenceGaps"})
    private List<AiEvidenceGapDto> evidenceGaps = new ArrayList<>();

    @JsonProperty("retrievedSources")
    @com.fasterxml.jackson.annotation.JsonAlias({"retrieved_sources", "retrievedSources", "retrieved_context", "retrievedContext"})
    private List<AiRetrievalResultItem> retrievedContext = new ArrayList<>();

    public AiCaseAnalysisRequest() {
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

    public String getCaseTitle() {
        return caseTitle != null ? caseTitle : title;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
        this.title = caseTitle;
    }

    public String getTitle() {
        return title != null ? title : caseTitle;
    }

    public void setTitle(String title) {
        this.title = title;
        this.caseTitle = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getCaseStatus() {
        return caseStatus != null ? caseStatus : status;
    }

    public void setCaseStatus(String caseStatus) {
        this.caseStatus = caseStatus;
        this.status = caseStatus;
    }

    public String getStatus() {
        return status != null ? status : caseStatus;
    }

    public void setStatus(String status) {
        this.status = status;
        this.caseStatus = status;
    }

    public String getInvestigationState() {
        return investigationState;
    }

    public void setInvestigationState(String investigationState) {
        this.investigationState = investigationState;
    }

    public Integer getInvestigationCompleteness() {
        return investigationCompleteness;
    }

    public void setInvestigationCompleteness(Integer investigationCompleteness) {
        this.investigationCompleteness = investigationCompleteness;
    }

    public AiFirDto getFir() {
        return fir;
    }

    public void setFir(AiFirDto fir) {
        this.fir = fir;
    }

    public List<AiVictimDto> getVictims() {
        return victims;
    }

    public void setVictims(List<AiVictimDto> victims) {
        this.victims = victims;
    }

    public List<AiSuspectDto> getSuspects() {
        return suspects;
    }

    public void setSuspects(List<AiSuspectDto> suspects) {
        this.suspects = suspects;
    }

    public List<AiEvidenceDto> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<AiEvidenceDto> evidence) {
        this.evidence = evidence;
    }

    public List<AiEvidenceGapDto> getEvidenceGaps() {
        return evidenceGaps;
    }

    public void setEvidenceGaps(List<AiEvidenceGapDto> evidenceGaps) {
        this.evidenceGaps = evidenceGaps;
    }

    public List<AiRetrievalResultItem> getRetrievedContext() {
        return retrievedContext;
    }

    public void setRetrievedContext(List<AiRetrievalResultItem> retrievedContext) {
        this.retrievedContext = retrievedContext != null ? retrievedContext : new ArrayList<>();
    }
}
