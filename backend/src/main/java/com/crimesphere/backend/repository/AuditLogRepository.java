package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("SELECT a FROM AuditLog a JOIN a.policeOfficer po WHERE po.station.id = :stationId ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentByStationId(@Param("stationId") UUID stationId, Pageable pageable);
}
