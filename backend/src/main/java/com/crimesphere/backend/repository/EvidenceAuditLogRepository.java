package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.EvidenceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceAuditLogRepository extends JpaRepository<EvidenceAuditLog, UUID> {

    List<EvidenceAuditLog> findByEvidenceIdOrderByCreatedAtDesc(UUID evidenceId);

    List<EvidenceAuditLog> findByCaseEntityIdOrderByCreatedAtDesc(UUID caseId);
}
