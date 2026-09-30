package com.crimesphere.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "investigator_reviews")
public class InvestigatorReview {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_item_id", nullable = false)
    private AiRecommendationItem recommendationItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private PoliceOfficer reviewer;

    @Column(name = "decision", nullable = false, length = 50)
    private String decision; // PENDING_REVIEW, APPROVED, EDITED, REJECTED

    @Column(name = "original_content", nullable = false, columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "reviewed_content", columnDefinition = "TEXT")
    private String reviewedContent;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "verification_status_at_review", length = 50)
    private String verificationStatusAtReview;

    @Column(name = "support_level_at_review", length = 50)
    private String supportLevelAtReview;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "investigatorReview", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvestigatorReviewHistory> history = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public InvestigatorReview() {
    }

    public InvestigatorReview(UUID id, AiRecommendationItem recommendationItem, Case caseEntity,
                              PoliceOfficer reviewer, String decision, String originalContent,
                              String reviewedContent, String reviewComment,
                              String verificationStatusAtReview, String supportLevelAtReview) {
        this.id = id;
        this.recommendationItem = recommendationItem;
        this.caseEntity = caseEntity;
        this.reviewer = reviewer;
        this.decision = decision;
        this.originalContent = originalContent;
        this.reviewedContent = reviewedContent;
        this.reviewComment = reviewComment;
        this.verificationStatusAtReview = verificationStatusAtReview;
        this.supportLevelAtReview = supportLevelAtReview;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AiRecommendationItem getRecommendationItem() {
        return recommendationItem;
    }

    public void setRecommendationItem(AiRecommendationItem recommendationItem) {
        this.recommendationItem = recommendationItem;
    }

    public Case getCaseEntity() {
        return caseEntity;
    }

    public void setCaseEntity(Case caseEntity) {
        this.caseEntity = caseEntity;
    }

    public PoliceOfficer getReviewer() {
        return reviewer;
    }

    public void setReviewer(PoliceOfficer reviewer) {
        this.reviewer = reviewer;
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

    public String getVerificationStatusAtReview() {
        return verificationStatusAtReview;
    }

    public void setVerificationStatusAtReview(String verificationStatusAtReview) {
        this.verificationStatusAtReview = verificationStatusAtReview;
    }

    public String getSupportLevelAtReview() {
        return supportLevelAtReview;
    }

    public void setSupportLevelAtReview(String supportLevelAtReview) {
        this.supportLevelAtReview = supportLevelAtReview;
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

    public List<InvestigatorReviewHistory> getHistory() {
        return history;
    }

    public void setHistory(List<InvestigatorReviewHistory> history) {
        this.history = history;
    }

    public void addHistory(InvestigatorReviewHistory historyEntry) {
        this.history.add(historyEntry);
        historyEntry.setInvestigatorReview(this);
    }
}
