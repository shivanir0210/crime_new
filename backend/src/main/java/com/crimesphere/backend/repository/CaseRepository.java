package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseRepository extends JpaRepository<Case, UUID>, JpaSpecificationExecutor<Case> {

    Optional<Case> findByCaseNumber(String caseNumber);

    Optional<Case> findByIdAndStationId(UUID id, UUID stationId);

    // Dashboard Statistics Query methods (station-scoped)
    long countByStationId(UUID stationId);

    long countByStationIdAndStatus(UUID stationId, String status);

    long countByStationIdAndStatusNot(UUID stationId, String status);

    long countByStationIdAndPriorityAndStatusNot(UUID stationId, String priority, String status);

    // Sequence generator for unique case numbers
    @Query(value = "SELECT nextval('case_number_seq')", nativeQuery = true)
    Long getNextCaseNumberSequence();

    @Query("SELECT c FROM Case c WHERE c.station.id = :stationId AND (c.priority = :priority OR c.status = :status) ORDER BY c.updatedAt DESC")
    java.util.List<Case> findByStationIdAndPriorityOrStatus(
            @org.springframework.data.repository.query.Param("stationId") UUID stationId,
            @org.springframework.data.repository.query.Param("priority") String priority,
            @org.springframework.data.repository.query.Param("status") String status,
            org.springframework.data.domain.Pageable pageable
    );
}
