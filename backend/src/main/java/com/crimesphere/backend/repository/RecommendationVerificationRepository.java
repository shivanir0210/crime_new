package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.RecommendationVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecommendationVerificationRepository extends JpaRepository<RecommendationVerification, UUID> {

    List<RecommendationVerification> findByCaseEntityIdOrderByCreatedAtDesc(UUID caseId);

    List<RecommendationVerification> findByRecommendationItemId(UUID recommendationItemId);

    Optional<RecommendationVerification> findFirstByRecommendationItemIdOrderByCreatedAtDesc(UUID recommendationItemId);

    @Query("SELECT rv FROM RecommendationVerification rv " +
           "JOIN rv.recommendationItem ri " +
           "WHERE ri.recommendation.id = :recommendationId " +
           "ORDER BY rv.createdAt DESC")
    List<RecommendationVerification> findByAiRecommendationId(@Param("recommendationId") UUID recommendationId);

    void deleteByCaseEntityId(UUID caseId);
}
