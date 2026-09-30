package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.CaseIllustration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseIllustrationRepository extends JpaRepository<CaseIllustration, UUID> {

    List<CaseIllustration> findByCaseEntityIdOrderByCreatedAtDesc(UUID caseId);

    Optional<CaseIllustration> findByIdAndCaseEntityId(UUID id, UUID caseId);

    long countByCaseEntityId(UUID caseId);
}
