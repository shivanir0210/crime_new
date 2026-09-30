package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, UUID> {
    List<AiRecommendation> findByCaseEntityIdOrderByCreatedAtDesc(UUID caseId);
    Optional<AiRecommendation> findByIdAndCaseEntityId(UUID id, UUID caseId);
}
