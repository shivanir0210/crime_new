package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.RecommendationVerificationEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecommendationVerificationEvidenceRepository extends JpaRepository<RecommendationVerificationEvidence, UUID> {

    List<RecommendationVerificationEvidence> findByVerificationId(UUID verificationId);

    List<RecommendationVerificationEvidence> findByEvidenceId(UUID evidenceId);
}
