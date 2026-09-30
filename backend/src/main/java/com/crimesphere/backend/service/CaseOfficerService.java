package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.CaseOfficerDto;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CaseOfficerService {

    private static final Logger logger = LoggerFactory.getLogger(CaseOfficerService.class);

    private final CaseRepository caseRepository;
    private final PoliceOfficerRepository policeOfficerRepository;
    private final CaseOfficerRepository caseOfficerRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;

    public CaseOfficerService(CaseRepository caseRepository,
                              PoliceOfficerRepository policeOfficerRepository,
                              CaseOfficerRepository caseOfficerRepository,
                              AuditLogRepository auditLogRepository,
                              NotificationService notificationService) {
        this.caseRepository = caseRepository;
        this.policeOfficerRepository = policeOfficerRepository;
        this.caseOfficerRepository = caseOfficerRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
    }

    /**
     * Assigns an officer to a case with station isolation checks.
     */
    @Transactional
    public CaseOfficerDto assignOfficerToCase(UUID caseId, UUID officerId, String authenticatedPoliceId) {
        PoliceOfficer authenticatedOfficer = getAuthenticatedOfficer(authenticatedPoliceId);
        Case parentCase = getAuthorizedCase(caseId, authenticatedOfficer);

        PoliceOfficer targetOfficer = policeOfficerRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Police officer not found: " + officerId));

        // Station isolation check: target officer must belong to same station as case (unless ADMIN)
        if (!"ADMIN".equalsIgnoreCase(authenticatedOfficer.getRole())) {
            if (targetOfficer.getStation() == null || parentCase.getStation() == null ||
                !targetOfficer.getStation().getId().equals(parentCase.getStation().getId())) {
                throw new IllegalArgumentException("Cannot assign officer from a different police station");
            }
        }

        Optional<CaseOfficer> existingOpt = caseOfficerRepository.findByCaseEntityIdAndPoliceOfficerId(caseId, officerId);
        CaseOfficer caseOfficer;
        if (existingOpt.isPresent()) {
            caseOfficer = existingOpt.get();
            if (caseOfficer.isActive()) {
                throw new IllegalArgumentException("Officer " + targetOfficer.getFullName() + " is already assigned to this case");
            }
            caseOfficer.setActive(true);
            caseOfficer.setAssignedBy(authenticatedOfficer);
        } else {
            caseOfficer = new CaseOfficer(parentCase, targetOfficer, authenticatedOfficer);
        }

        CaseOfficer saved = caseOfficerRepository.save(caseOfficer);

        // Record audit log
        recordAuditLog(authenticatedOfficer, "CASE_OFFICER_ASSIGNED",
                String.format("Officer %s assigned %s (%s) to case %s",
                        authenticatedOfficer.getFullName(), targetOfficer.getFullName(),
                        targetOfficer.getPoliceId(), parentCase.getCaseNumber()));

        // Notify target officer of assignment
        try {
            if (notificationService != null && !targetOfficer.getId().equals(authenticatedOfficer.getId())) {
                notificationService.createAndSendNotification(
                        parentCase,
                        targetOfficer,
                        NotificationChannel.IN_APP,
                        NotificationType.CASE_ALERT,
                        "Assigned to Case: " + parentCase.getCaseNumber(),
                        String.format("Officer %s assigned you to Case %s (%s).",
                                authenticatedOfficer.getFullName(), parentCase.getCaseNumber(), parentCase.getTitle()),
                        "Review case dossier in CrimeSphere AI.",
                        targetOfficer.getEmail(),
                        targetOfficer.getPhone()
                );
            }
        } catch (Exception e) {
            logger.warn("Failed to notify assigned officer: {}", e.getMessage());
        }

        return toDto(saved);
    }

    /**
     * Deactivates/removes an officer from a case assignment.
     */
    @Transactional
    public void removeOfficerFromCase(UUID caseId, UUID officerId, String authenticatedPoliceId) {
        PoliceOfficer authenticatedOfficer = getAuthenticatedOfficer(authenticatedPoliceId);
        Case parentCase = getAuthorizedCase(caseId, authenticatedOfficer);

        CaseOfficer caseOfficer = caseOfficerRepository.findByCaseEntityIdAndPoliceOfficerId(caseId, officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer assignment not found for case: " + caseId));

        caseOfficer.setActive(false);
        caseOfficerRepository.save(caseOfficer);

        recordAuditLog(authenticatedOfficer, "CASE_OFFICER_REMOVED",
                String.format("Officer %s removed officer %s from case %s",
                        authenticatedOfficer.getFullName(), officerId, parentCase.getCaseNumber()));
    }

    /**
     * Lists all active officers assigned to a case.
     */
    @Transactional(readOnly = true)
    public List<CaseOfficerDto> getAssignedOfficers(UUID caseId, String authenticatedPoliceId) {
        PoliceOfficer authenticatedOfficer = getAuthenticatedOfficer(authenticatedPoliceId);
        Case parentCase = getAuthorizedCase(caseId, authenticatedOfficer);

        List<CaseOfficer> activeAssignments = caseOfficerRepository.findByCaseEntityIdAndIsActiveTrue(parentCase.getId());
        List<CaseOfficerDto> result = activeAssignments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        // Ensure case creator is included if missing
        if (parentCase.getCreatedBy() != null && result.stream().noneMatch(dto -> dto.getOfficerId().equals(parentCase.getCreatedBy().getId()))) {
            PoliceOfficer creator = parentCase.getCreatedBy();
            CaseOfficerDto creatorDto = new CaseOfficerDto(
                    null,
                    parentCase.getId(),
                    creator.getId(),
                    creator.getPoliceId(),
                    creator.getFullName(),
                    creator.getRole(),
                    creator.getRole(),
                    creator.getStation() != null ? creator.getStation().getStationName() : "N/A",
                    parentCase.getCreatedAt(),
                    "Creator",
                    true,
                    creator.getPhone()
            );
            result.add(0, creatorDto);
        }

        return result;
    }

    /**
     * Lists all officers in caller's station available for assignment to a case.
     */
    @Transactional(readOnly = true)
    public List<CaseOfficerDto> getAvailableStationOfficers(UUID caseId, String authenticatedPoliceId) {
        PoliceOfficer authenticatedOfficer = getAuthenticatedOfficer(authenticatedPoliceId);
        Case parentCase = getAuthorizedCase(caseId, authenticatedOfficer);

        UUID stationId = parentCase.getStation() != null ? parentCase.getStation().getId() : null;
        if (stationId == null) {
            return List.of();
        }

        List<PoliceOfficer> stationOfficers = policeOfficerRepository.findByStationId(stationId);
        return stationOfficers.stream()
                .map(officer -> new CaseOfficerDto(
                        null,
                        parentCase.getId(),
                        officer.getId(),
                        officer.getPoliceId(),
                        officer.getFullName(),
                        officer.getRole(),
                        officer.getRole(),
                        officer.getStation() != null ? officer.getStation().getStationName() : "N/A",
                        null,
                        null,
                        true,
                        officer.getPhone()
                ))
                .collect(Collectors.toList());
    }

    private PoliceOfficer getAuthenticatedOfficer(String policeId) {
        return policeOfficerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new ResourceNotFoundException("Police officer not found: " + policeId));
    }

    private Case getAuthorizedCase(UUID caseId, PoliceOfficer officer) {
        Case c = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));

        if (!"ADMIN".equalsIgnoreCase(officer.getRole())) {
            if (officer.getStation() == null || c.getStation() == null
                    || !officer.getStation().getId().equals(c.getStation().getId())) {
                throw new ResourceNotFoundException("Case not found or unauthorized for officer station");
            }
        }
        return c;
    }

    private void recordAuditLog(PoliceOfficer officer, String action, String description) {
        try {
            AuditLog log = new AuditLog();
            log.setPoliceOfficer(officer);
            log.setAction(action);
            log.setStatus("SUCCESS");
            log.setUserAgent(description);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to write audit log for action: {}", action, e);
        }
    }

    public CaseOfficerDto toDto(CaseOfficer co) {
        PoliceOfficer officer = co.getPoliceOfficer();
        return new CaseOfficerDto(
                co.getId(),
                co.getCaseEntity().getId(),
                officer.getId(),
                officer.getPoliceId(),
                officer.getFullName(),
                officer.getRole(),
                officer.getRole(),
                officer.getStation() != null ? officer.getStation().getStationName() : "N/A",
                co.getAssignedAt(),
                co.getAssignedBy() != null ? co.getAssignedBy().getFullName() : null,
                co.isActive(),
                officer.getPhone()
        );
    }
}
