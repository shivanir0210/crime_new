package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.analysis.EvidenceGapDTO;

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
public class EvidenceGapAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(EvidenceGapAnalysisService.class);

    private final EvidenceGapRepository evidenceGapRepository;
    private final CaseRepository caseRepository;
    private final FIRRepository firRepository;
    private final VictimRepository victimRepository;
    private final SuspectRepository suspectRepository;
    private final EvidenceRepository evidenceRepository;
    private final PoliceOfficerRepository officerRepository;
    private final AuditLogRepository auditLogRepository;

    public EvidenceGapAnalysisService(EvidenceGapRepository evidenceGapRepository,
                                     CaseRepository caseRepository,
                                     FIRRepository firRepository,
                                     VictimRepository victimRepository,
                                     SuspectRepository suspectRepository,
                                     EvidenceRepository evidenceRepository,
                                     PoliceOfficerRepository officerRepository,
                                     AuditLogRepository auditLogRepository) {
        this.evidenceGapRepository = evidenceGapRepository;
        this.caseRepository = caseRepository;
        this.firRepository = firRepository;
        this.victimRepository = victimRepository;
        this.suspectRepository = suspectRepository;
        this.evidenceRepository = evidenceRepository;
        this.officerRepository = officerRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Executes deterministic, rule-based evidence gap analysis for a specific case.
     */
    @Transactional
    public List<EvidenceGapDTO> analyzeCase(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        // 1. Inspect case database state
        boolean hasFir = firRepository.existsByCaseEntityId(caseId);
        long victimCount = victimRepository.countByCaseEntityId(caseId);
        boolean hasVictim = victimCount > 0;
        long suspectCount = suspectRepository.countByCaseEntityId(caseId);
        boolean hasSuspect = suspectCount > 0;

        List<Evidence> evidenceList = evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
        boolean hasEvidence = !evidenceList.isEmpty();
        boolean hasVerifiedEvidence = evidenceList.stream()
                .anyMatch(e -> "VERIFIED".equalsIgnoreCase(e.getStatus()));

        Set<String> evidenceTypes = evidenceList.stream()
                .map(e -> e.getEvidenceType() != null ? e.getEvidenceType().toUpperCase() : "")
                .collect(Collectors.toSet());

        String caseType = parentCase.getCaseType() != null ? parentCase.getCaseType().toUpperCase() : "OTHER";

        // 2. Fetch existing gaps for duplicate prevention and status management
        List<EvidenceGap> existingGaps = evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
        Map<String, EvidenceGap> openGapMap = new HashMap<>();
        for (EvidenceGap g : existingGaps) {
            String key = g.getCategory() + "::" + g.getTitle();
            openGapMap.put(key, g);
        }

        List<GapRuleDefinition> rules = new ArrayList<>();

        // RULE 1: Missing FIR
        rules.add(new GapRuleDefinition(
                "FIR",
                "Missing FIR",
                "First Information Report has not been registered for this case.",
                "CRITICAL",
                "No FIR has been registered for this case.",
                !hasFir
        ));

        // RULE 2: Missing Victim
        rules.add(new GapRuleDefinition(
                "VICTIM",
                "Victim information missing",
                "No victim or complainant profile has been recorded in the case file.",
                "HIGH",
                "No victim information has been registered.",
                !hasVictim
        ));

        // RULE 3: Missing Suspect
        rules.add(new GapRuleDefinition(
                "SUSPECT",
                "Suspect information missing",
                "No suspect or person of interest is linked to this investigation.",
                "MEDIUM",
                "No suspect information has been recorded.",
                !hasSuspect
        ));

        // RULE 4: Evidence not collected
        rules.add(new GapRuleDefinition(
                "DOCUMENT",
                "Evidence not collected",
                "Zero evidence files or physical records are attached to this investigation.",
                "HIGH",
                "No evidence has been uploaded for this case.",
                !hasEvidence
        ));

        // RULE 5: No verified evidence (Only created when evidence exists but none is VERIFIED)
        rules.add(new GapRuleDefinition(
                "FORENSIC",
                "No verified evidence",
                "Uploaded evidence items are pending review or verification.",
                "HIGH",
                "Collected evidence items exist but none have VERIFIED status.",
                hasEvidence && !hasVerifiedEvidence
        ));

        // RULE 6: Case-type specific suggestions
        if ("CYBER_CRIME".contains(caseType) || caseType.contains("CYBER")) {
            boolean hasDigital = evidenceTypes.contains("DIGITAL_DEVICE") || evidenceTypes.contains("CALL_RECORD") || evidenceTypes.contains("DIGITAL");
            rules.add(new GapRuleDefinition(
                    "DIGITAL",
                    "Digital evidence not recorded",
                    "Suggested information to verify: Securing digital artifacts, IP logs, or system transaction trails.",
                    "MEDIUM",
                    "Digital evidence artifacts have not been filed for this cyber crime case.",
                    !hasDigital
            ));
        }

        if ("MURDER".contains(caseType) || caseType.contains("MURDER") || caseType.contains("ASSAULT")) {
            boolean hasForensic = evidenceTypes.contains("FORENSIC_REPORT") || evidenceTypes.contains("FORENSIC");
            rules.add(new GapRuleDefinition(
                    "FORENSIC",
                    "Forensic report not available",
                    "Suggested information to verify: Post-mortem, DNA, or ballistic forensic examination reports.",
                    "MEDIUM",
                    "Forensic report is missing for this violent crime case type.",
                    !hasForensic
            ));

            boolean hasCctv = evidenceTypes.contains("CCTV");
            rules.add(new GapRuleDefinition(
                    "CCTV",
                    "CCTV footage not available",
                    "Suggested information to verify: Ingress/egress route surveillance camera recordings.",
                    "MEDIUM",
                    "No CCTV footage has been cataloged for this case.",
                    !hasCctv
            ));
        }

        if ("THEFT".contains(caseType) || caseType.contains("THEFT") || caseType.contains("BURGLARY")) {
            boolean hasCctv = evidenceTypes.contains("CCTV");
            rules.add(new GapRuleDefinition(
                    "CCTV",
                    "CCTV footage not available",
                    "Suggested information to verify: Nearby surveillance camera footage of incident scene.",
                    "MEDIUM",
                    "No CCTV evidence has been collected for this theft case.",
                    !hasCctv
            ));
        }

        if ("MISSING_PERSON".contains(caseType) || caseType.contains("MISSING")) {
            boolean hasCallRecord = evidenceTypes.contains("CALL_RECORD") || evidenceTypes.contains("CDR");
            rules.add(new GapRuleDefinition(
                    "CALL_RECORD",
                    "Call detail records missing",
                    "Suggested information to verify: Call detail records (CDR) or tower dump location data.",
                    "MEDIUM",
                    "Call records have not been attached for missing person tracking.",
                    !hasCallRecord
            ));
        }

        boolean gapStateChanged = false;

        // 3. Process gap rules idempotently
        for (GapRuleDefinition rule : rules) {
            String key = rule.category + "::" + rule.title;
            EvidenceGap existing = openGapMap.get(key);

            if (rule.triggered) {
                if (existing == null) {
                    // Create new OPEN gap
                    EvidenceGap newGap = new EvidenceGap();
                    newGap.setCaseEntity(parentCase);
                    newGap.setCategory(rule.category);
                    newGap.setTitle(rule.title);
                    newGap.setDescription(rule.description);
                    newGap.setSeverity(rule.severity);
                    newGap.setStatus("OPEN");
                    newGap.setReason(rule.reason);
                    newGap.setSourceType("DETERMINISTIC_RULE");
                    evidenceGapRepository.save(newGap);

                    recordAuditLog(officer, "EVIDENCE_GAP_CREATED",
                            String.format("Created %s evidence gap '%s' for case %s",
                                    rule.severity, rule.title, parentCase.getCaseNumber()));
                    gapStateChanged = true;
                } else if ("RESOLVED".equalsIgnoreCase(existing.getStatus())) {
                    // Re-open if condition failed again
                    existing.setStatus("OPEN");
                    existing.setResolvedAt(null);
                    existing.setResolvedBy(null);
                    existing.setReason(rule.reason);
                    evidenceGapRepository.save(existing);

                    recordAuditLog(officer, "EVIDENCE_GAP_UPDATED",
                            String.format("Reopened evidence gap '%s' for case %s",
                                    rule.title, parentCase.getCaseNumber()));
                    gapStateChanged = true;
                }
            } else {
                // Condition satisfied: Auto-resolve OPEN or ACKNOWLEDGED gap if requirement fulfilled
                if (existing != null && ("OPEN".equalsIgnoreCase(existing.getStatus()) || "ACKNOWLEDGED".equalsIgnoreCase(existing.getStatus()))) {
                    existing.setStatus("RESOLVED");
                    existing.setResolvedAt(OffsetDateTime.now());
                    existing.setResolvedBy(officer);
                    existing.setReason("Requirement satisfied based on updated case records.");
                    evidenceGapRepository.save(existing);

                    recordAuditLog(officer, "EVIDENCE_GAP_RESOLVED",
                            String.format("Auto-resolved gap '%s' for case %s",
                                    rule.title, parentCase.getCaseNumber()));
                    gapStateChanged = true;
                }
            }
        }

        if (gapStateChanged) {
            parentCase.setUpdatedAt(OffsetDateTime.now());
            caseRepository.save(parentCase);
        }

        recordAuditLog(officer, "EVIDENCE_GAP_ANALYSIS_REQUESTED",
                "Executed deterministic evidence gap analysis for case " + parentCase.getCaseNumber());

        logger.info("Executed evidence gap analysis for case: {} by officer: {}", parentCase.getCaseNumber(), policeId);

        return evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
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

    public EvidenceGapDTO mapToDTO(EvidenceGap gap) {
        EvidenceGapDTO dto = new EvidenceGapDTO();
        dto.setId(gap.getId());
        dto.setCaseId(gap.getCaseEntity().getId());
        dto.setCategory(gap.getCategory());
        dto.setTitle(gap.getTitle());
        dto.setDescription(gap.getDescription());
        dto.setSeverity(gap.getSeverity());
        dto.setStatus(gap.getStatus());
        dto.setReason(gap.getReason());
        dto.setSourceType(gap.getSourceType());
        dto.setSourceReferenceId(gap.getSourceReferenceId());
        dto.setCreatedAt(gap.getCreatedAt());
        dto.setUpdatedAt(gap.getUpdatedAt());
        dto.setResolvedAt(gap.getResolvedAt());
        if (gap.getResolvedBy() != null) {
            dto.setResolvedById(gap.getResolvedBy().getId());
            dto.setResolvedByName(gap.getResolvedBy().getFullName());
        }
        return dto;
    }

    private static class GapRuleDefinition {
        final String category;
        final String title;
        final String description;
        final String severity;
        final String reason;
        final boolean triggered;

        GapRuleDefinition(String category, String title, String description, String severity, String reason, boolean triggered) {
            this.category = category;
            this.title = title;
            this.description = description;
            this.severity = severity;
            this.reason = reason;
            this.triggered = triggered;
        }
    }
}
