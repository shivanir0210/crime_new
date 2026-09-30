package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.EvidenceGap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvidenceGapRepository extends JpaRepository<EvidenceGap, UUID> {

    List<EvidenceGap> findByCaseEntityIdOrderByCreatedAtDesc(UUID caseId);

    List<EvidenceGap> findByCaseEntityIdAndStatus(UUID caseId, String status);

    Optional<EvidenceGap> findByIdAndCaseEntityId(UUID id, UUID caseId);

    Optional<EvidenceGap> findByCaseEntityIdAndCategoryAndTitle(UUID caseId, String category, String title);

    boolean existsByCaseEntityIdAndCategoryAndTitle(UUID caseId, String category, String title);

    long countByCaseEntityIdAndStatus(UUID caseId, String status);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(g) FROM EvidenceGap g WHERE g.caseEntity.station.id = :stationId AND g.status = :status")
    long countByStationIdAndStatus(UUID stationId, String status);
}
