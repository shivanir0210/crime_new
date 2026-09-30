package com.crimesphere.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "investigator_review_history")
public class InvestigatorReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investigator_review_id", nullable = false)
    private InvestigatorReview investigatorReview;

    @Column(name = "previous_decision", length = 50)
    private String previousDecision;

    @Column(name = "new_decision", length = 50)
    private String newDecision;

    @Column(name = "previous_content", columnDefinition = "TEXT")
    private String previousContent;

    @Column(name = "new_content", columnDefinition = "TEXT")
    private String newContent;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private PoliceOfficer changedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }

    public InvestigatorReviewHistory() {
    }

    public InvestigatorReviewHistory(UUID id, InvestigatorReview investigatorReview,
                                   String previousDecision, String newDecision,
                                   String previousContent, String newContent,
                                   String comment, PoliceOfficer changedBy) {
        this.id = id;
        this.investigatorReview = investigatorReview;
        this.previousDecision = previousDecision;
        this.newDecision = newDecision;
        this.previousContent = previousContent;
        this.newContent = newContent;
        this.comment = comment;
        this.changedBy = changedBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public InvestigatorReview getInvestigatorReview() {
        return investigatorReview;
    }

    public void setInvestigatorReview(InvestigatorReview investigatorReview) {
        this.investigatorReview = investigatorReview;
    }

    public String getPreviousDecision() {
        return previousDecision;
    }

    public void setPreviousDecision(String previousDecision) {
        this.previousDecision = previousDecision;
    }

    public String getNewDecision() {
        return newDecision;
    }

    public void setNewDecision(String newDecision) {
        this.newDecision = newDecision;
    }

    public String getPreviousContent() {
        return previousContent;
    }

    public void setPreviousContent(String previousContent) {
        this.previousContent = previousContent;
    }

    public String getNewContent() {
        return newContent;
    }

    public void setNewContent(String newContent) {
        this.newContent = newContent;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public PoliceOfficer getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(PoliceOfficer changedBy) {
        this.changedBy = changedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
