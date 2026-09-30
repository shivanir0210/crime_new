package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.CaseLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseLocationRepository extends JpaRepository<CaseLocation, UUID> {

    List<CaseLocation> findByCaseEntityIdOrderBySequenceOrderAscRecordedAtAsc(UUID caseId);

    List<CaseLocation> findByCaseEntityIdOrderByRecordedAtAsc(UUID caseId);

    List<CaseLocation> findByCaseEntityId(UUID caseId);

    Optional<CaseLocation> findByIdAndCaseEntityId(UUID id, UUID caseId);

    long countByCaseEntityId(UUID caseId);

    void deleteByCaseEntityId(UUID caseId);
}
