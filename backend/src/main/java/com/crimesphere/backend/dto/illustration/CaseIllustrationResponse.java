package com.crimesphere.backend.dto.illustration;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CaseIllustrationResponse {

    private UUID id;
    private UUID caseId;
    private String prompt;
    private String revisedPrompt;
    private String modelName;
    private String mimeType;
    private Long fileSize;
    private String fileHash;
    private String status;
    private Boolean isAiGenerated;
    private String disclaimer;
    private String viewUrl;
    private String createdByOfficerName;
    private String createdByPoliceId;
    private OffsetDateTime createdAt;

    public CaseIllustrationResponse() {
        this.disclaimer = "AI-GENERATED ILLUSTRATION — NOT EVIDENCE. Generated for hypothetical investigative scenario reconstruction.";
        this.isAiGenerated = true;
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

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getRevisedPrompt() {
        return revisedPrompt;
    }

    public void setRevisedPrompt(String revisedPrompt) {
        this.revisedPrompt = revisedPrompt;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsAiGenerated() {
        return isAiGenerated;
    }

    public void setIsAiGenerated(Boolean isAiGenerated) {
        this.isAiGenerated = isAiGenerated;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public String getViewUrl() {
        return viewUrl;
    }

    public void setViewUrl(String viewUrl) {
        this.viewUrl = viewUrl;
    }

    public String getCreatedByOfficerName() {
        return createdByOfficerName;
    }

    public void setCreatedByOfficerName(String createdByOfficerName) {
        this.createdByOfficerName = createdByOfficerName;
    }

    public String getCreatedByPoliceId() {
        return createdByPoliceId;
    }

    public void setCreatedByPoliceId(String createdByPoliceId) {
        this.createdByPoliceId = createdByPoliceId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
