package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.InvestigatorReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvestigatorReviewRepository extends JpaRepository<InvestigatorReview, UUID> {

    List<InvestigatorReview> findByCaseEntityIdOrderByCreatedAtDesc(UUID caseId);

    List<InvestigatorReview> findByRecommendationItemId(UUID recommendationItemId);

    Optional<InvestigatorReview> findFirstByRecommendationItemIdOrderByCreatedAtDesc(UUID recommendationItemId);

    Optional<InvestigatorReview> findByRecommendationItemIdAndCaseEntityId(UUID recommendationItemId, UUID caseId);

    @Query("SELECT ir FROM InvestigatorReview ir " +
           "JOIN ir.recommendationItem ri " +
           "WHERE ri.recommendation.id = :analysisId " +
           "ORDER BY ir.createdAt DESC")
    List<InvestigatorReview> findByAiRecommendationId(@Param("analysisId") UUID analysisId);

    List<InvestigatorReview> findByCaseEntityIdAndDecision(UUID caseId, String decision);

    void deleteByCaseEntityId(UUID caseId);
}
