package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class EditRecommendationRequest {

    @JsonProperty("content")
    private String content;

    @JsonProperty("editedSummary")
    private String editedSummary;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("evidenceReferences")
    private List<String> evidenceReferences = new ArrayList<>();

    public EditRecommendationRequest() {
    }

    public EditRecommendationRequest(String content, String comment, List<String> evidenceReferences) {
        this.content = content;
        this.comment = comment;
        this.evidenceReferences = evidenceReferences != null ? evidenceReferences : new ArrayList<>();
    }

    public String getContent() {
        if (content != null && !content.trim().isEmpty()) {
            return content;
        }
        return editedSummary;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getEvidenceReferences() {
        return evidenceReferences;
    }

    public void setEvidenceReferences(List<String> evidenceReferences) {
        this.evidenceReferences = evidenceReferences;
    }
}
