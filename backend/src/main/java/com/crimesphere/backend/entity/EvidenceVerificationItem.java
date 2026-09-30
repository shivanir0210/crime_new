package com.crimesphere.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "evidence_verification_items")
public class EvidenceVerificationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_id", nullable = false)
    private EvidenceVerification verification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_id")
    private Evidence evidence;

    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType; // EVIDENCE_REFERENCE, CLAIM, MISSING_SUPPORT, CONTRADICTION, CASE_FACT

    @Column(name = "claim", nullable = false, columnDefinition = "TEXT")
    private String claim;

    @Column(name = "supporting_content", columnDefinition = "TEXT")
    private String supportingContent;

    @Column(name = "result", nullable = false, length = 50)
    private String result; // SUPPORTED, PARTIALLY_SUPPORTED, UNSUPPORTED, NOT_FOUND, CONFLICT

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }

    public EvidenceVerificationItem() {
    }

    public EvidenceVerificationItem(UUID id, EvidenceVerification verification, Evidence evidence,
                                  String itemType, String claim, String supportingContent,
                                  String result, String reason) {
        this.id = id;
        this.verification = verification;
        this.evidence = evidence;
        this.itemType = itemType;
        this.claim = claim;
        this.supportingContent = supportingContent;
        this.result = result;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EvidenceVerification getVerification() {
        return verification;
    }

    public void setVerification(EvidenceVerification verification) {
        this.verification = verification;
    }

    public Evidence getEvidence() {
        return evidence;
    }

    public void setEvidence(Evidence evidence) {
        this.evidence = evidence;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getClaim() {
        return claim;
    }

    public void setClaim(String claim) {
        this.claim = claim;
    }

    public String getSupportingContent() {
        return supportingContent;
    }

    public void setSupportingContent(String supportingContent) {
        this.supportingContent = supportingContent;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
