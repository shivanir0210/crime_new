package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.analysis.CaseStateDTO;
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

@Service
public class CaseStateService {

    private static final Logger logger = LoggerFactory.getLogger(CaseStateService.class);

    private final CaseRepository caseRepository;
    private final FIRRepository firRepository;
    private final VictimRepository victimRepository;
    private final SuspectRepository suspectRepository;
    private final EvidenceRepository evidenceRepository;
    private final PoliceOfficerRepository officerRepository;
    private final AuditLogRepository auditLogRepository;

    public CaseStateService(CaseRepository caseRepository,
                            FIRRepository firRepository,
                            VictimRepository victimRepository,
                            SuspectRepository suspectRepository,
                            EvidenceRepository evidenceRepository,
                            PoliceOfficerRepository officerRepository,
                            AuditLogRepository auditLogRepository) {
        this.caseRepository = caseRepository;
        this.firRepository = firRepository;
        this.victimRepository = victimRepository;
        this.suspectRepository = suspectRepository;
        this.evidenceRepository = evidenceRepository;
        this.officerRepository = officerRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public CaseStateDTO evaluateCaseState(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        CaseStateDTO dto = computeCaseState(parentCase);

        recordAuditLog(officer, "CASE_STATE_EVALUATED",
                String.format("Evaluated investigation state '%s' for case %s",
                        dto.getCurrentState(), parentCase.getCaseNumber()));

        return dto;
    }

    /**
     * Deterministic, dynamic evaluation of the current case database state.
     */
    public CaseStateDTO computeCaseState(Case parentCase) {
        UUID caseId = parentCase.getId();

        boolean hasFir = firRepository.existsByCaseEntityId(caseId);
        long victimCount = victimRepository.countByCaseEntityId(caseId);
        boolean hasVictim = victimCount > 0;
        long suspectCount = suspectRepository.countByCaseEntityId(caseId);
        boolean hasSuspect = suspectCount > 0;

        List<Evidence> evidenceList = evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
        boolean hasEvidence = !evidenceList.isEmpty();
        boolean hasUnderReviewEvidence = evidenceList.stream()
                .anyMatch(e -> "UNDER_REVIEW".equalsIgnoreCase(e.getStatus()));
        boolean hasVerifiedEvidence = evidenceList.stream()
                .anyMatch(e -> "VERIFIED".equalsIgnoreCase(e.getStatus()));

        List<String> completedAreas = new ArrayList<>();
        List<String> missingAreas = new ArrayList<>();

        // 1. Case registration
        completedAreas.add("Case Information");

        // 2. FIR
        if (hasFir) {
            completedAreas.add("FIR");
        } else {
            missingAreas.add("FIR Registration");
        }

        // 3. Victim
        if (hasVictim) {
            completedAreas.add("Victim Information");
        } else {
            missingAreas.add("Victim Information");
        }

        // 4. Suspect
        if (hasSuspect) {
            completedAreas.add("Suspect Information");
        } else {
            missingAreas.add("Suspect Information");
        }

        // 5. Evidence Collection
        if (hasEvidence) {
            completedAreas.add("Evidence Collection");
        } else {
            missingAreas.add("Evidence Collection");
        }

        // 6. Evidence Verification
        if (hasVerifiedEvidence) {
            completedAreas.add("Verified Evidence");
        } else {
            missingAreas.add("Verified Evidence");
        }

        // Deterministic progress calculation: completed / total relevant areas (6) * 100
        int totalAreas = 6;
        int progressPercentage = (int) Math.round(((double) completedAreas.size() / totalAreas) * 100.0);

        // Determine Investigation State based on exact rule ordering
        String currentState;
        String stateLabel;

        boolean isPendingReview = parentCase.getStatus() != null && "PENDING_REVIEW".equalsIgnoreCase(parentCase.getStatus());
        boolean minRequirementsSatisfied = hasFir && hasVictim && hasSuspect && hasEvidence && hasVerifiedEvidence;

        if (isPendingReview) {
            currentState = "PENDING_INVESTIGATOR_REVIEW";
            stateLabel = "Pending Investigator Review";
        } else if (minRequirementsSatisfied) {
            currentState = "INVESTIGATION_READY";
            stateLabel = "Ready for AI-Assisted Investigation";
        } else if (hasVerifiedEvidence) {
            currentState = "EVIDENCE_VERIFIED";
            stateLabel = "Evidence Verified";
        } else if (hasUnderReviewEvidence) {
            currentState = "EVIDENCE_UNDER_REVIEW";
            stateLabel = "Evidence Under Review";
        } else if (hasEvidence) {
            currentState = "EVIDENCE_COLLECTED";
            stateLabel = "Evidence Collected";
        } else if (hasSuspect) {
            currentState = "SUSPECT_IDENTIFIED";
            stateLabel = "Suspect Identified";
        } else if (hasVictim) {
            currentState = "VICTIM_IDENTIFIED";
            stateLabel = "Victim Identified";
        } else if (hasFir) {
            currentState = "FIR_REGISTERED";
            stateLabel = "FIR Registered";
        } else {
            currentState = "CASE_CREATED";
            stateLabel = "Case Created";
        }

        CaseStateDTO dto = new CaseStateDTO();
        dto.setCaseId(parentCase.getId());
        dto.setCaseNumber(parentCase.getCaseNumber());
        dto.setCurrentState(currentState);
        dto.setStateLabel(stateLabel);
        dto.setProgressPercentage(progressPercentage);
        dto.setCompletedAreas(completedAreas);
        dto.setMissingAreas(missingAreas);
        dto.setLastEvaluatedAt(OffsetDateTime.now());

        return dto;
    }

    public Case getAuthorizedCase(UUID caseId, PoliceOfficer officer) {
        UUID stationId = officer.getStation() != null ? officer.getStation().getId() : null;
        if (stationId == null) {
            throw new IllegalStateException("Police officer has no assigned station");
        }
        return caseRepository.findByIdAndStationId(caseId, stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found or access restricted to your station"));
    }

    public PoliceOfficer getAuthenticatedOfficer(String policeId) {
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
