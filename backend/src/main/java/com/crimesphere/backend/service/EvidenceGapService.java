package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.analysis.EvidenceGapDTO;
import com.crimesphere.backend.dto.analysis.UpdateGapStatusRequest;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EvidenceGapService {

    private static final Logger logger = LoggerFactory.getLogger(EvidenceGapService.class);

    private static final Set<String> VALID_GAP_STATUSES = Set.of(
            "OPEN", "ACKNOWLEDGED", "RESOLVED", "NOT_APPLICABLE"
    );

    private final EvidenceGapRepository evidenceGapRepository;
    private final CaseRepository caseRepository;
    private final PoliceOfficerRepository officerRepository;
    private final AuditLogRepository auditLogRepository;
    private final EvidenceGapAnalysisService evidenceGapAnalysisService;

    public EvidenceGapService(EvidenceGapRepository evidenceGapRepository,
                               CaseRepository caseRepository,
                               PoliceOfficerRepository officerRepository,
                               AuditLogRepository auditLogRepository,
                               EvidenceGapAnalysisService evidenceGapAnalysisService) {
        this.evidenceGapRepository = evidenceGapRepository;
        this.caseRepository = caseRepository;
        this.officerRepository = officerRepository;
        this.auditLogRepository = auditLogRepository;
        this.evidenceGapAnalysisService = evidenceGapAnalysisService;
    }

    /**
     * Get all detected gaps for a specific case with station-level security check.
     */
    @Transactional(readOnly = true)
    public List<EvidenceGapDTO> getGapsByCase(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        return evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(parentCase.getId())
                .stream()
                .map(evidenceGapAnalysisService::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Delegates deterministic, rule-based evidence gap analysis to EvidenceGapAnalysisService.
     */
    @Transactional
    public List<EvidenceGapDTO> runGapAnalysis(UUID caseId, String policeId) {
        return evidenceGapAnalysisService.analyzeCase(caseId, policeId);
    }

    /**
     * Update the status of an evidence gap (ACKNOWLEDGED, RESOLVED, NOT_APPLICABLE).
     * Extracts resolvedBy officer strictly from JWT token. Updates cases.updated_at.
     */
    @Transactional
    public EvidenceGapDTO updateGapStatus(UUID caseId, UUID gapId, UpdateGapStatusRequest request, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        EvidenceGap gap = evidenceGapRepository.findByIdAndCaseEntityId(gapId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Evidence gap not found or access restricted to your station"));

        String newStatus = request.getStatus() != null ? request.getStatus().trim().toUpperCase() : "";
        if (!VALID_GAP_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status: " + newStatus + ". Allowed: " + VALID_GAP_STATUSES);
        }

        String oldStatus = gap.getStatus();
        gap.setStatus(newStatus);
        if (request.getReason() != null && !request.getReason().isBlank()) {
            gap.setReason(request.getReason().trim());
        }

        if ("RESOLVED".equalsIgnoreCase(newStatus) || "NOT_APPLICABLE".equalsIgnoreCase(newStatus)) {
            gap.setResolvedAt(OffsetDateTime.now());
            gap.setResolvedBy(officer);
        } else if ("OPEN".equalsIgnoreCase(newStatus) || "ACKNOWLEDGED".equalsIgnoreCase(newStatus)) {
            gap.setResolvedAt(null);
            gap.setResolvedBy(null);
        }

        EvidenceGap updated = evidenceGapRepository.save(gap);

        // Update cases.updated_at per requirement 18
        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        // Audit Logging
        String action = "RESOLVED".equalsIgnoreCase(newStatus) ? "EVIDENCE_GAP_RESOLVED" : "EVIDENCE_GAP_UPDATED";

        recordAuditLog(officer, action,
                String.format("Updated gap '%s' status from %s to %s for case %s",
                        gap.getTitle(), oldStatus, newStatus, parentCase.getCaseNumber()));

        logger.info("Gap {} status updated: {} -> {} for case {} by officer {}",
                gap.getId(), oldStatus, newStatus, parentCase.getCaseNumber(), policeId);

        return evidenceGapAnalysisService.mapToDTO(updated);
    }

    private Case getAuthorizedCase(UUID caseId, PoliceOfficer officer) {
        UUID stationId = officer.getStation() != null ? officer.getStation().getId() : null;
        if (stationId == null) {
            throw new IllegalStateException("Police officer has no assigned station");
        }
        return caseRepository.findByIdAndStationId(caseId, stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found or access restricted to your station"));
    }

    private PoliceOfficer getAuthenticatedOfficer(String policeId) {
        return officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated police officer record not found"));
    }

    private void recordAuditLog(PoliceOfficer officer, String action, String details) {
        try {
            AuditLog log = new AuditLog(null, officer, action, "SUCCESS", null, details);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to record audit log for action {}: {}", action, e.getMessage());
        }
    }
}
