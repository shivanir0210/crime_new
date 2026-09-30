package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class AiRetrievalRequest {

    @JsonProperty("case_id")
    private UUID caseId;

    @JsonProperty("query")
    private String query;

    @JsonProperty("top_k")
    private int topK = 5;

    @JsonProperty("min_similarity")
    private double minSimilarity = 0.70;

    public AiRetrievalRequest() {
    }

    public AiRetrievalRequest(UUID caseId, String query, int topK, double minSimilarity) {
        this.caseId = caseId;
        this.query = query;
        this.topK = topK;
        this.minSimilarity = minSimilarity;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getMinSimilarity() {
        return minSimilarity;
    }

    public void setMinSimilarity(double minSimilarity) {
        this.minSimilarity = minSimilarity;
    }
}
