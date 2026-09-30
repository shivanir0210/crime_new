package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {

    List<Evidence> findByCaseEntityIdOrderByCreatedAtDesc(UUID caseId);

    Optional<Evidence> findByIdAndCaseEntityId(UUID id, UUID caseId);

    Optional<Evidence> findByEvidenceNumber(String evidenceNumber);

    boolean existsByCaseEntityIdAndFileHash(UUID caseId, String fileHash);

    boolean existsByFileHashAndCaseEntityIdNot(String fileHash, UUID caseId);

    List<Evidence> findByFileHashAndCaseEntityIdNot(String fileHash, UUID caseId);

    @Query(value = "SELECT nextval('evidence_number_seq')", nativeQuery = true)
    Long getNextEvidenceNumberSequence();

    @Query("SELECT COUNT(e) FROM Evidence e WHERE e.caseEntity.station.id = :stationId")
    long countByStationId(UUID stationId);

    @Query("SELECT COUNT(e) FROM Evidence e WHERE e.caseEntity.station.id = :stationId AND e.status = :status")
    long countByStationIdAndStatus(UUID stationId, String status);
}
