package com.crimesphere.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "recommendation_verifications")
public class RecommendationVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_item_id", nullable = false)
    private AiRecommendationItem recommendationItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Column(name = "verification_status", nullable = false, length = 50)
    private String verificationStatus; // SUPPORTED, PARTIALLY_SUPPORTED, UNSUPPORTED, UNVERIFIABLE, REFERENCE_INVALID

    @Column(name = "support_level", nullable = false, length = 50)
    private String supportLevel; // STRONG, MODERATE, WEAK, NONE

    @Column(name = "verification_summary", columnDefinition = "TEXT")
    private String verificationSummary;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private PoliceOfficer verifiedBy;

    @Column(name = "verification_method", nullable = false, length = 50)
    private String verificationMethod; // REFERENCE_CHECK, RULE_BASED, SEMANTIC_CHECK, INVESTIGATOR_REVIEW

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "verification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RecommendationVerificationEvidence> verificationEvidenceList = new ArrayList<>();

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
        if (this.verificationMethod == null) {
            this.verificationMethod = "RULE_BASED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public RecommendationVerification() {
    }

    public RecommendationVerification(UUID id, AiRecommendationItem recommendationItem, Case caseEntity,
                                        String verificationStatus, String supportLevel, String verificationSummary,
                                        PoliceOfficer verifiedBy, String verificationMethod) {
        this.id = id;
        this.recommendationItem = recommendationItem;
        this.caseEntity = caseEntity;
        this.verificationStatus = verificationStatus;
        this.supportLevel = supportLevel;
        this.verificationSummary = verificationSummary;
        this.verifiedBy = verifiedBy;
        this.verificationMethod = verificationMethod != null ? verificationMethod : "RULE_BASED";
        this.verifiedAt = OffsetDateTime.now();
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

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(OffsetDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public PoliceOfficer getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(PoliceOfficer verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getVerificationMethod() {
        return verificationMethod;
    }

    public void setVerificationMethod(String verificationMethod) {
        this.verificationMethod = verificationMethod;
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

    public List<RecommendationVerificationEvidence> getVerificationEvidenceList() {
        return verificationEvidenceList;
    }

    public void setVerificationEvidenceList(List<RecommendationVerificationEvidence> verificationEvidenceList) {
        this.verificationEvidenceList = verificationEvidenceList;
    }

    public void addVerificationEvidence(RecommendationVerificationEvidence ev) {
        verificationEvidenceList.add(ev);
        ev.setVerification(this);
    }
}
