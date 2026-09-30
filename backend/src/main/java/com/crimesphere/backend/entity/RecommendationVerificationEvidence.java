package com.crimesphere.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "recommendation_verification_evidence")
public class RecommendationVerificationEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_id", nullable = false)
    private RecommendationVerification verification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evidence_id", nullable = false)
    private Evidence evidence;

    @Column(name = "relationship", nullable = false, length = 50)
    private String relationship; // SUPPORTS, PARTIALLY_SUPPORTS, DOES_NOT_SUPPORT, INVALID_REFERENCE

    @Column(name = "support_note", columnDefinition = "TEXT")
    private String supportNote;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }

    public RecommendationVerificationEvidence() {
    }

    public RecommendationVerificationEvidence(UUID id, RecommendationVerification verification,
                                             Evidence evidence, String relationship, String supportNote) {
        this.id = id;
        this.verification = verification;
        this.evidence = evidence;
        this.relationship = relationship;
        this.supportNote = supportNote;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RecommendationVerification getVerification() {
        return verification;
    }

    public void setVerification(RecommendationVerification verification) {
        this.verification = verification;
    }

    public Evidence getEvidence() {
        return evidence;
    }

    public void setEvidence(Evidence evidence) {
        this.evidence = evidence;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getSupportNote() {
        return supportNote;
    }

    public void setSupportNote(String supportNote) {
        this.supportNote = supportNote;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
