package com.crimesphere.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "ai_recommendation_evidence")
public class AiRecommendationEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_item_id", nullable = false)
    private AiRecommendationItem recommendationItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evidence_id", nullable = false)
    private Evidence evidence;

    public AiRecommendationEvidence() {
    }

    public AiRecommendationEvidence(UUID id, AiRecommendationItem recommendationItem, Evidence evidence) {
        this.id = id;
        this.recommendationItem = recommendationItem;
        this.evidence = evidence;
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

    public Evidence getEvidence() {
        return evidence;
    }

    public void setEvidence(Evidence evidence) {
        this.evidence = evidence;
    }
}
