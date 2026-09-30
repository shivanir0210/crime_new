package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class AiRetrievalResponse {

    @JsonProperty("query")
    private String query;

    @JsonProperty("candidate_count")
    private int candidateCount;

    @JsonProperty("returned_count")
    private int returnedCount;

    @JsonProperty("top_similarity")
    private double topSimilarity;

    @JsonProperty("results")
    private List<AiRetrievalResultItem> results = new ArrayList<>();

    public AiRetrievalResponse() {
    }

    public AiRetrievalResponse(String query, int candidateCount, int returnedCount, double topSimilarity,
                              List<AiRetrievalResultItem> results) {
        this.query = query;
        this.candidateCount = candidateCount;
        this.returnedCount = returnedCount;
        this.topSimilarity = topSimilarity;
        this.results = results != null ? results : new ArrayList<>();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getCandidateCount() {
        return candidateCount;
    }

    public void setCandidateCount(int candidateCount) {
        this.candidateCount = candidateCount;
    }

    public int getReturnedCount() {
        return returnedCount;
    }

    public void setReturnedCount(int returnedCount) {
        this.returnedCount = returnedCount;
    }

    public double getTopSimilarity() {
        return topSimilarity;
    }

    public void setTopSimilarity(double topSimilarity) {
        this.topSimilarity = topSimilarity;
    }

    public List<AiRetrievalResultItem> getResults() {
        return results;
    }

    public void setResults(List<AiRetrievalResultItem> results) {
        this.results = results != null ? results : new ArrayList<>();
    }
}
