package com.crimesphere.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "evidence_verifications")
public class EvidenceVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_recommendation_id", nullable = false)
    private AiRecommendation aiRecommendation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_item_id")
    private AiRecommendationItem recommendationItem;

    @Column(name = "verification_status", nullable = false, length = 50)
    private String verificationStatus; // PENDING, COMPLETED, REQUIRES_REVIEW

    @Column(name = "support_level", nullable = false, length = 50)
    private String supportLevel; // SUPPORTED, PARTIALLY_SUPPORTED, UNSUPPORTED, VERIFICATION_REQUIRED

    @Column(name = "verification_summary", columnDefinition = "TEXT")
    private String verificationSummary;

    @Column(name = "verified_evidence_count", nullable = false)
    private int verifiedEvidenceCount = 0;

    @Column(name = "missing_evidence_count", nullable = false)
    private int missingEvidenceCount = 0;

    @Column(name = "conflict_count", nullable = false)
    private int conflictCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private PoliceOfficer verifiedBy;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "verification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EvidenceVerificationItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = OffsetDateTime.now();
        }
        if (this.verifiedAt == null) {
            this.verifiedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public EvidenceVerification() {
    }

    public EvidenceVerification(UUID id, Case caseEntity, AiRecommendation aiRecommendation,
                                AiRecommendationItem recommendationItem, String verificationStatus,
                                String supportLevel, String verificationSummary, int verifiedEvidenceCount,
                                int missingEvidenceCount, int conflictCount, PoliceOfficer verifiedBy) {
        this.id = id;
        this.caseEntity = caseEntity;
        this.aiRecommendation = aiRecommendation;
        this.recommendationItem = recommendationItem;
        this.verificationStatus = verificationStatus;
        this.supportLevel = supportLevel;
        this.verificationSummary = verificationSummary;
        this.verifiedEvidenceCount = verifiedEvidenceCount;
        this.missingEvidenceCount = missingEvidenceCount;
        this.conflictCount = conflictCount;
        this.verifiedBy = verifiedBy;
        this.verifiedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Case getCaseEntity() {
        return caseEntity;
    }

    public void setCaseEntity(Case caseEntity) {
        this.caseEntity = caseEntity;
    }

    public AiRecommendation getAiRecommendation() {
        return aiRecommendation;
    }

    public void setAiRecommendation(AiRecommendation aiRecommendation) {
        this.aiRecommendation = aiRecommendation;
    }

    public AiRecommendationItem getRecommendationItem() {
        return recommendationItem;
    }

    public void setRecommendationItem(AiRecommendationItem recommendationItem) {
        this.recommendationItem = recommendationItem;
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

    public String getVerificationSummary() {
        return verificationSummary;
    }

    public void setVerificationSummary(String verificationSummary) {
        this.verificationSummary = verificationSummary;
    }

    public int getVerifiedEvidenceCount() {
        return verifiedEvidenceCount;
    }

    public void setVerifiedEvidenceCount(int verifiedEvidenceCount) {
        this.verifiedEvidenceCount = verifiedEvidenceCount;
    }

    public int getMissingEvidenceCount() {
        return missingEvidenceCount;
    }

    public void setMissingEvidenceCount(int missingEvidenceCount) {
        this.missingEvidenceCount = missingEvidenceCount;
    }

    public int getConflictCount() {
        return conflictCount;
    }

    public void setConflictCount(int conflictCount) {
        this.conflictCount = conflictCount;
    }

    public PoliceOfficer getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(PoliceOfficer verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(OffsetDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
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

    public List<EvidenceVerificationItem> getItems() {
        return items;
    }

    public void setItems(List<EvidenceVerificationItem> items) {
        this.items = items;
    }

    public void addItem(EvidenceVerificationItem item) {
        items.add(item);
        item.setVerification(this);
    }
}
