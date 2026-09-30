package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InvestigatorReviewResponse {

    @JsonProperty("reviewId")
    private UUID reviewId;

    @JsonProperty("caseId")
    private UUID caseId;

    @JsonProperty("analysisId")
    private UUID analysisId;

    @JsonProperty("recommendationItemId")
    private UUID recommendationItemId;

    @JsonProperty("decision")
    private String decision; // PENDING_REVIEW, APPROVED, EDITED, REJECTED

    @JsonProperty("originalContent")
    private String originalContent;

    @JsonProperty("reviewedContent")
    private String reviewedContent;

    @JsonProperty("reviewComment")
    private String reviewComment;

    @JsonProperty("verificationStatus")
    private String verificationStatus;

    @JsonProperty("supportLevel")
    private String supportLevel;

    @JsonProperty("reviewer")
    private String reviewer;

    @JsonProperty("createdAt")
    private OffsetDateTime createdAt;

    @JsonProperty("updatedAt")
    private OffsetDateTime updatedAt;

    @JsonProperty("itemType")
    private String itemType;

    @JsonProperty("reasoning")
    private String reasoning;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("evidenceReferences")
    private List<String> evidenceReferences = new ArrayList<>();

    @JsonProperty("findingIndex")
    private Integer findingIndex;

    public InvestigatorReviewResponse() {
    }

    public InvestigatorReviewResponse(UUID reviewId, UUID caseId, UUID analysisId, UUID recommendationItemId,
                                      String decision, String originalContent, String reviewedContent,
                                      String reviewComment, String verificationStatus, String supportLevel,
                                      String reviewer, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(reviewId, caseId, analysisId, recommendationItemId, decision, originalContent, reviewedContent,
                reviewComment, verificationStatus, supportLevel, reviewer, createdAt, updatedAt,
                null, null, null, null, null);
    }

    public InvestigatorReviewResponse(UUID reviewId, UUID caseId, UUID analysisId, UUID recommendationItemId,
                                      String decision, String originalContent, String reviewedContent,
                                      String reviewComment, String verificationStatus, String supportLevel,
                                      String reviewer, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                      String itemType, String reasoning, String priority,
                                      List<String> evidenceReferences, Integer findingIndex) {
        this.reviewId = reviewId;
        this.caseId = caseId;
        this.analysisId = analysisId;
        this.recommendationItemId = recommendationItemId;
        this.decision = decision;
        this.originalContent = originalContent;
        this.reviewedContent = reviewedContent;
        this.reviewComment = reviewComment;
        this.verificationStatus = verificationStatus;
        this.supportLevel = supportLevel;
        this.reviewer = reviewer;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.itemType = itemType;
        this.reasoning = reasoning;
        this.priority = priority;
        this.evidenceReferences = evidenceReferences != null ? evidenceReferences : new ArrayList<>();
        this.findingIndex = findingIndex;
    }

    public UUID getReviewId() {
        return reviewId;
    }

    public void setReviewId(UUID reviewId) {
        this.reviewId = reviewId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(UUID analysisId) {
        this.analysisId = analysisId;
    }

    public UUID getRecommendationItemId() {
        return recommendationItemId;
    }

    public void setRecommendationItemId(UUID recommendationItemId) {
        this.recommendationItemId = recommendationItemId;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getReviewedContent() {
        return reviewedContent;
    }

    public void setReviewedContent(String reviewedContent) {
        this.reviewedContent = reviewedContent;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getSupportLevel() {
        return supportLevel;
    }

    public void setSupportLevel(String supportLevel) {
        this.supportLevel = supportLevel;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public List<String> getEvidenceReferences() {
        return evidenceReferences;
    }

    public void setEvidenceReferences(List<String> evidenceReferences) {
        this.evidenceReferences = evidenceReferences != null ? evidenceReferences : new ArrayList<>();
    }

    public Integer getFindingIndex() {
        return findingIndex;
    }

    public void setFindingIndex(Integer findingIndex) {
        this.findingIndex = findingIndex;
    }

    @JsonProperty("recommendationId")
    public UUID getRecommendationId() {
        return analysisId != null ? analysisId : recommendationItemId;
    }

    @JsonProperty("status")
    public String getStatus() {
        return decision;
    }

    @JsonProperty("comment")
    public String getComment() {
        return reviewComment;
    }

    @JsonProperty("editedSummary")
    public String getEditedSummary() {
        return reviewedContent;
    }

    @JsonProperty("reviewedAt")
    public OffsetDateTime getReviewedAt() {
        return updatedAt != null ? updatedAt : createdAt;
    }
}
