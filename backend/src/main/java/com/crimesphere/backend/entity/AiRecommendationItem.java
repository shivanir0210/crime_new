package com.crimesphere.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ai_recommendation_items")
public class AiRecommendationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_recommendation_id", nullable = false)
    private AiRecommendation recommendation;

    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType; // OBSERVATION, POTENTIAL_LEAD, EVIDENCE_GAP, CONTRADICTION, RECOMMENDED_ACTION, UNCERTAINTY, LIMITATION

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "reasoning", columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "priority", length = 50)
    private String priority; // LOW, MEDIUM, HIGH

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "recommendationItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AiRecommendationEvidence> evidenceReferences = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }

    public AiRecommendationItem() {
    }

    public AiRecommendationItem(UUID id, AiRecommendation recommendation, String itemType,
                                String content, String reasoning, String priority) {
        this.id = id;
        this.recommendation = recommendation;
        this.itemType = itemType;
        this.content = content;
        this.reasoning = reasoning;
        this.priority = priority;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AiRecommendation getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(AiRecommendation recommendation) {
        this.recommendation = recommendation;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<AiRecommendationEvidence> getEvidenceReferences() {
        return evidenceReferences;
    }

    public void setEvidenceReferences(List<AiRecommendationEvidence> evidenceReferences) {
        this.evidenceReferences = evidenceReferences;
    }

    public void addEvidenceReference(AiRecommendationEvidence ref) {
        evidenceReferences.add(ref);
        ref.setRecommendationItem(this);
    }
}
