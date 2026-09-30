package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.EvidenceVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvidenceVerificationRepository extends JpaRepository<EvidenceVerification, UUID> {

    List<EvidenceVerification> findByCaseEntityIdOrderByCreatedAtDesc(UUID caseId);

    List<EvidenceVerification> findByAiRecommendationIdOrderByCreatedAtDesc(UUID aiRecommendationId);

    Optional<EvidenceVerification> findByIdAndCaseEntityId(UUID id, UUID caseId);
}
