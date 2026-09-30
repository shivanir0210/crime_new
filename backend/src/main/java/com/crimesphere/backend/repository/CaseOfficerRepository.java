package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.CaseOfficer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseOfficerRepository extends JpaRepository<CaseOfficer, UUID> {

    List<CaseOfficer> findByCaseEntityIdAndIsActiveTrue(UUID caseId);

    List<CaseOfficer> findByCaseEntityId(UUID caseId);

    List<CaseOfficer> findByPoliceOfficerIdAndIsActiveTrue(UUID policeOfficerId);

    Optional<CaseOfficer> findByCaseEntityIdAndPoliceOfficerId(UUID caseId, UUID policeOfficerId);

    boolean existsByCaseEntityIdAndPoliceOfficerIdAndIsActiveTrue(UUID caseId, UUID policeOfficerId);
}
