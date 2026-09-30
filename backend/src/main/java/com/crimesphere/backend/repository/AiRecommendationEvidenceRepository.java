package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.AiRecommendationEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiRecommendationEvidenceRepository extends JpaRepository<AiRecommendationEvidence, UUID> {
    List<AiRecommendationEvidence> findByRecommendationItemId(UUID recommendationItemId);
}
