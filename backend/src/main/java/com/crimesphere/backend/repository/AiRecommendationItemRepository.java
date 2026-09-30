package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.AiRecommendationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiRecommendationItemRepository extends JpaRepository<AiRecommendationItem, UUID> {
    List<AiRecommendationItem> findByRecommendationId(UUID recommendationId);
    List<AiRecommendationItem> findByRecommendationIdOrderByCreatedAtAsc(UUID recommendationId);
}
