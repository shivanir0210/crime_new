package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.CaseReportGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaseReportGenerationRepository extends JpaRepository<CaseReportGeneration, UUID> {
    List<CaseReportGeneration> findByCrimeCase_IdOrderByCreatedAtDesc(UUID caseId);
}
