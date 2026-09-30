package com.crimesphere.backend.dto.analysis;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class CaseStateDTO {

    private UUID caseId;
    private String caseNumber;
    private String currentState;
    private String stateLabel;
    private int progressPercentage;
    private List<String> completedAreas;
    private List<String> missingAreas;
    private OffsetDateTime lastEvaluatedAt;

    public CaseStateDTO() {
    }

    public CaseStateDTO(UUID caseId, String caseNumber, String currentState, String stateLabel,
                        int progressPercentage, List<String> completedAreas, List<String> missingAreas,
                        OffsetDateTime lastEvaluatedAt) {
        this.caseId = caseId;
        this.caseNumber = caseNumber;
        this.currentState = currentState;
        this.stateLabel = stateLabel;
        this.progressPercentage = progressPercentage;
        this.completedAreas = completedAreas;
        this.missingAreas = missingAreas;
        this.lastEvaluatedAt = lastEvaluatedAt;
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

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public String getStateLabel() {
        return stateLabel;
    }

    public void setStateLabel(String stateLabel) {
        this.stateLabel = stateLabel;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public List<String> getCompletedAreas() {
        return completedAreas;
    }

    public void setCompletedAreas(List<String> completedAreas) {
        this.completedAreas = completedAreas;
    }

    public List<String> getMissingAreas() {
        return missingAreas;
    }

    public void setMissingAreas(List<String> missingAreas) {
        this.missingAreas = missingAreas;
    }

    public OffsetDateTime getLastEvaluatedAt() {
        return lastEvaluatedAt;
    }

    public void setLastEvaluatedAt(OffsetDateTime lastEvaluatedAt) {
        this.lastEvaluatedAt = lastEvaluatedAt;
    }
}
