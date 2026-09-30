package com.crimesphere.backend.dto.public_search;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SuggestedQueriesResponse {

    private UUID caseId;
    private String caseNumber;
    private List<String> suggestedQueries = new ArrayList<>();
    private String explanation;

    public SuggestedQueriesResponse() {
    }

    public SuggestedQueriesResponse(UUID caseId, String caseNumber, List<String> suggestedQueries, String explanation) {
        this.caseId = caseId;
        this.caseNumber = caseNumber;
        this.suggestedQueries = suggestedQueries != null ? suggestedQueries : new ArrayList<>();
        this.explanation = explanation;
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

    public List<String> getSuggestedQueries() {
        return suggestedQueries;
    }

    public void setSuggestedQueries(List<String> suggestedQueries) {
        this.suggestedQueries = suggestedQueries != null ? suggestedQueries : new ArrayList<>();
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
