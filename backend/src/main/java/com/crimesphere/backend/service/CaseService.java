package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.case_mgmt.*;
import com.crimesphere.backend.entity.AuditLog;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.entity.PoliceStation;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.entity.NotificationType;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.CaseSpecification;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CaseService {

    private static final Logger logger = LoggerFactory.getLogger(CaseService.class);

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "OPEN", "UNDER_INVESTIGATION", "ON_HOLD", "PENDING_REVIEW", "CLOSED"
    );

    private static final Set<String> ALLOWED_PRIORITIES = Set.of(
            "LOW", "MEDIUM", "HIGH", "CRITICAL"
    );

    private static final Set<String> ALLOWED_CASE_TYPES = Set.of(
            "MURDER", "THEFT", "MISSING_PERSON", "ASSAULT", "CYBER_CRIME", "OTHER"
    );

    private final CaseRepository caseRepository;
    private final PoliceOfficerRepository officerRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final com.crimesphere.backend.repository.CaseOfficerRepository caseOfficerRepository;

    public CaseService(CaseRepository caseRepository,
                       PoliceOfficerRepository officerRepository,
                       AuditLogRepository auditLogRepository) {
        this(caseRepository, officerRepository, auditLogRepository, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CaseService(CaseRepository caseRepository,
                       PoliceOfficerRepository officerRepository,
                       AuditLogRepository auditLogRepository,
                       NotificationService notificationService,
                       com.crimesphere.backend.repository.CaseOfficerRepository caseOfficerRepository) {
        this.caseRepository = caseRepository;
        this.officerRepository = officerRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
        this.caseOfficerRepository = caseOfficerRepository;
    }

    @Transactional
    public CaseResponse createCase(CreateCaseRequest request, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        PoliceStation station = officer.getStation();

        if (station == null) {
            throw new IllegalStateException("Police officer is not assigned to any police station");
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Case title is required");
        }

        String caseType = request.getCaseType() != null && !request.getCaseType().trim().isEmpty()
                ? request.getCaseType().trim().toUpperCase() : "OTHER";

        String priority = request.getPriority() != null && !request.getPriority().trim().isEmpty()
                ? request.getPriority().trim().toUpperCase() : "MEDIUM";

        if (!ALLOWED_PRIORITIES.contains(priority)) {
            throw new IllegalArgumentException("Invalid case priority: " + priority + ". Allowed: " + ALLOWED_PRIORITIES);
        }

        String caseNumber = generateCaseNumber();

        Case newCase = new Case();
        newCase.setCaseNumber(caseNumber);
        newCase.setTitle(request.getTitle().trim());
        newCase.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        newCase.setCaseType(caseType);
        newCase.setPriority(priority);
        newCase.setStatus("OPEN"); // Forced initial status
        newCase.setStation(station);
        newCase.setCreatedBy(officer);

        Case savedCase = caseRepository.save(newCase);

        // Register creator as primary officer in case_officers team table
        if (caseOfficerRepository != null) {
            try {
                com.crimesphere.backend.entity.CaseOfficer creatorOfficer =
                        new com.crimesphere.backend.entity.CaseOfficer(savedCase, officer, officer);
                caseOfficerRepository.save(creatorOfficer);
            } catch (Exception e) {
                logger.warn("Failed to register case creator in case_officers table: {}", e.getMessage());
            }
        }

        if (notificationService != null) {
            try {
                notificationService.dispatchCaseNotification(
                        savedCase,
                        NotificationType.CASE_CREATED,
                        "CrimeSphere AI — New Case Registered: " + savedCase.getCaseNumber(),
                        String.format("A new case has been created: %s (%s). Priority: %s.",
                                savedCase.getCaseNumber(), savedCase.getTitle(), savedCase.getPriority()),
                        "Please review the new dossier in the CrimeSphere command center.",
                        officer
                );
            } catch (Exception e) {
                logger.warn("CASE_CREATED notification dispatch failed: {}", e.getMessage());
            }
        }

        recordAuditLog(officer, "CASE_CREATED", "SUCCESS", httpRequest);
        logger.info("Case created successfully: {} by officer: {}", caseNumber, policeId);

        return new CaseResponse(savedCase);
    }

    @Transactional(readOnly = true)
    public PageResponse<CaseSummaryResponse> getCases(String policeId,
                                                      String search,
                                                      String status,
                                                      String priority,
                                                      String caseType,
                                                      Pageable pageable) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        UUID stationId = officer.getStation() != null ? officer.getStation().getId() : null;

        if (stationId == null) {
            throw new IllegalStateException("Officer has no assigned station");
        }

        Specification<Case> spec = CaseSpecification.withFilters(stationId, search, status, priority, caseType);
        Page<Case> casePage = caseRepository.findAll(spec, pageable);

        Page<CaseSummaryResponse> summaryPage = casePage.map(CaseSummaryResponse::new);
        return new PageResponse<>(summaryPage);
    }

    @Transactional(readOnly = true)
    public CaseResponse getCaseById(UUID id, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        UUID stationId = officer.getStation() != null ? officer.getStation().getId() : null;

        Case c = caseRepository.findByIdAndStationId(id, stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Case record not found or access restricted to your station"));

        return new CaseResponse(c);
    }

    @Transactional
    public CaseResponse updateCase(UUID id, UpdateCaseRequest request, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        UUID stationId = officer.getStation() != null ? officer.getStation().getId() : null;

        Case c = caseRepository.findByIdAndStationId(id, stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Case record not found or access restricted to your station"));

        String oldStatus = c.getStatus();
        String oldPriority = c.getPriority();

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            c.setTitle(request.getTitle().trim());
        }

        if (request.getDescription() != null) {
            c.setDescription(request.getDescription().trim());
        }

        if (request.getCaseType() != null && !request.getCaseType().trim().isEmpty()) {
            c.setCaseType(request.getCaseType().trim().toUpperCase());
        }

        if (request.getPriority() != null && !request.getPriority().trim().isEmpty()) {
            String priority = request.getPriority().trim().toUpperCase();
            if (!ALLOWED_PRIORITIES.contains(priority)) {
                throw new IllegalArgumentException("Invalid priority: " + priority);
            }
            c.setPriority(priority);
        }

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            String status = request.getStatus().trim().toUpperCase();
            if (!ALLOWED_STATUSES.contains(status)) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
            c.setStatus(status);
        }

        Case updatedCase = caseRepository.save(c);

        if (!updatedCase.getStatus().equalsIgnoreCase(oldStatus) && notificationService != null) {
            try {
                notificationService.dispatchCaseNotification(
                        updatedCase,
                        NotificationType.CASE_STATUS_CHANGED,
                        "CrimeSphere AI — Case Status Changed: " + updatedCase.getCaseNumber(),
                        String.format("Case %s status transitioned from %s to %s.",
                                updatedCase.getCaseNumber(), oldStatus, updatedCase.getStatus()),
                        "Review investigation progress.",
                        officer
                );
            } catch (Exception e) {
                logger.warn("CASE_STATUS_CHANGED notification failed: {}", e.getMessage());
            }
        }

        if (!updatedCase.getPriority().equalsIgnoreCase(oldPriority) && notificationService != null) {
            try {
                notificationService.dispatchCaseNotification(
                        updatedCase,
                        NotificationType.CASE_PRIORITY_CHANGED,
                        "CrimeSphere AI — Case Priority Updated: " + updatedCase.getCaseNumber(),
                        String.format("Case %s priority updated from %s to %s.",
                                updatedCase.getCaseNumber(), oldPriority, updatedCase.getPriority()),
                        "Review priority escalation in dossier.",
                        officer
                );
            } catch (Exception e) {
                logger.warn("CASE_PRIORITY_CHANGED notification failed: {}", e.getMessage());
            }
        }

        recordAuditLog(officer, "CASE_UPDATED", "SUCCESS", httpRequest);

        return new CaseResponse(updatedCase);
    }

    @Transactional
    public CaseResponse updateStatus(UUID id, String newStatus, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        UUID stationId = officer.getStation() != null ? officer.getStation().getId() : null;

        if (newStatus == null || !ALLOWED_STATUSES.contains(newStatus.trim().toUpperCase())) {
            throw new IllegalArgumentException("Invalid status: " + newStatus + ". Allowed: " + ALLOWED_STATUSES);
        }

        Case c = caseRepository.findByIdAndStationId(id, stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Case record not found or access restricted to your station"));

        String oldStatus = c.getStatus();
        c.setStatus(newStatus.trim().toUpperCase());
        Case updatedCase = caseRepository.save(c);

        if (!updatedCase.getStatus().equalsIgnoreCase(oldStatus) && notificationService != null) {
            try {
                notificationService.dispatchCaseNotification(
                        updatedCase,
                        NotificationType.CASE_STATUS_CHANGED,
                        "CrimeSphere AI — Case Status Changed: " + updatedCase.getCaseNumber(),
                        String.format("Case %s status transitioned from %s to %s.",
                                updatedCase.getCaseNumber(), oldStatus, updatedCase.getStatus()),
                        "Review investigation progress.",
                        officer
                );
            } catch (Exception e) {
                logger.warn("CASE_STATUS_CHANGED notification failed: {}", e.getMessage());
            }
        }

        recordAuditLog(officer, "CASE_STATUS_UPDATED", "SUCCESS", httpRequest);
        return new CaseResponse(updatedCase);
    }

    @Transactional
    public CaseResponse updatePriority(UUID id, String newPriority, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        UUID stationId = officer.getStation() != null ? officer.getStation().getId() : null;

        if (newPriority == null || !ALLOWED_PRIORITIES.contains(newPriority.trim().toUpperCase())) {
            throw new IllegalArgumentException("Invalid priority: " + newPriority + ". Allowed: " + ALLOWED_PRIORITIES);
        }

        Case c = caseRepository.findByIdAndStationId(id, stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Case record not found or access restricted to your station"));

        String oldPriority = c.getPriority();
        c.setPriority(newPriority.trim().toUpperCase());
        Case updatedCase = caseRepository.save(c);

        if (!updatedCase.getPriority().equalsIgnoreCase(oldPriority) && notificationService != null) {
            try {
                notificationService.dispatchCaseNotification(
                        updatedCase,
                        NotificationType.CASE_PRIORITY_CHANGED,
                        "CrimeSphere AI — Case Priority Updated: " + updatedCase.getCaseNumber(),
                        String.format("Case %s priority updated from %s to %s.",
                                updatedCase.getCaseNumber(), oldPriority, updatedCase.getPriority()),
                        "Review priority escalation in dossier.",
                        officer
                );
            } catch (Exception e) {
                logger.warn("CASE_PRIORITY_CHANGED notification failed: {}", e.getMessage());
            }
        }

        recordAuditLog(officer, "CASE_PRIORITY_UPDATED", "SUCCESS", httpRequest);
        return new CaseResponse(updatedCase);
    }

    private synchronized String generateCaseNumber() {
        int currentYear = Year.now().getValue();
        try {
            Long nextSeq = caseRepository.getNextCaseNumberSequence();
            return String.format("CS-%d-%04d", currentYear, nextSeq);
        } catch (Exception e) {
            logger.warn("Sequence fetch failed, using timestamp based sequence: {}", e.getMessage());
            long fallback = System.currentTimeMillis() % 10000;
            return String.format("CS-%d-%04d", currentYear, fallback);
        }
    }

    private PoliceOfficer getAuthenticatedOfficer(String policeId) {
        return officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated police officer record not found"));
    }

    private void recordAuditLog(PoliceOfficer officer, String action, String status, HttpServletRequest request) {
        try {
            String clientIp = extractClientIp(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : "Unknown";
            AuditLog log = new AuditLog(null, officer, action, status, clientIp, userAgent);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to record audit log for action {}: {}", action, e.getMessage());
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "Unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "Unknown";
    }
}
