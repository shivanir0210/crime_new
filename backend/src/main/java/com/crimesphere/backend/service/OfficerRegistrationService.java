package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.PendingOfficerRegistrationDto;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.NotificationRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OfficerRegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(OfficerRegistrationService.class);

    private final PoliceOfficerRepository officerRepository;
    private final StorageService storageService;
    private final ResendEmailService resendEmailService;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;

    public OfficerRegistrationService(PoliceOfficerRepository officerRepository,
                                    StorageService storageService,
                                    ResendEmailService resendEmailService,
                                    AuditLogRepository auditLogRepository) {
        this(officerRepository, storageService, resendEmailService, auditLogRepository, null);
    }

    @Autowired
    public OfficerRegistrationService(PoliceOfficerRepository officerRepository,
                                    StorageService storageService,
                                    ResendEmailService resendEmailService,
                                    AuditLogRepository auditLogRepository,
                                    NotificationRepository notificationRepository) {
        this.officerRepository = officerRepository;
        this.storageService = storageService;
        this.resendEmailService = resendEmailService;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Retrieves all pending officer registrations for the Station Head's station.
     */
    @Transactional(readOnly = true)
    public List<PendingOfficerRegistrationDto> getPendingRegistrationsForStation(PoliceOfficer currentOfficer) {
        if (currentOfficer == null || currentOfficer.getStation() == null) {
            throw new AccessDeniedException("You are not associated with any police station.");
        }

        UUID stationId = currentOfficer.getStation().getId();
        List<PoliceOfficer> stationOfficers = officerRepository.findByStationId(stationId);

        return stationOfficers.stream()
                .filter(o -> "PENDING_STATION_APPROVAL".equalsIgnoreCase(o.getStatus()) ||
                             "PENDING_VERIFICATION".equalsIgnoreCase(o.getStatus()) ||
                             "REJECTED".equalsIgnoreCase(o.getStatus()))
                .map(PendingOfficerRegistrationDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves details for a specific registration, enforcing station isolation.
     */
    @Transactional(readOnly = true)
    public PendingOfficerRegistrationDto getRegistrationDetail(UUID officerId, PoliceOfficer currentOfficer) {
        return getRegistrationDetail(officerId, currentOfficer, null);
    }

    /**
     * Retrieves details for a specific registration, enforcing station isolation and auditing.
     */
    @Transactional(readOnly = true)
    public PendingOfficerRegistrationDto getRegistrationDetail(UUID officerId, PoliceOfficer currentOfficer, HttpServletRequest httpRequest) {
        PoliceOfficer targetOfficer = officerRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer registration record not found with ID: " + officerId));

        validateStationAccess(targetOfficer, currentOfficer);

        recordAuditLog(targetOfficer, "REGISTRATION_VIEWED", "SUCCESS",
                "Registration viewed by " + currentOfficer.getFullName() + " (" + currentOfficer.getPoliceId() +
                ") [Station ID: " + currentOfficer.getStation().getId() + "]",
                httpRequest);

        return new PendingOfficerRegistrationDto(targetOfficer);
    }

    /**
     * Approves an officer registration, activates the account, and notifies the officer.
     */
    @Transactional
    public PendingOfficerRegistrationDto approveRegistration(UUID officerId, PoliceOfficer reviewer, HttpServletRequest httpRequest) {
        PoliceOfficer targetOfficer = officerRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer registration record not found with ID: " + officerId));

        validateStationAccess(targetOfficer, reviewer);

        if (!"PENDING_STATION_APPROVAL".equalsIgnoreCase(targetOfficer.getStatus()) &&
            !"PENDING_VERIFICATION".equalsIgnoreCase(targetOfficer.getStatus())) {
            throw new IllegalStateException("Registration is not in a pending approval status (Current status: " + targetOfficer.getStatus() + ").");
        }

        targetOfficer.setStatus("ACTIVE");
        targetOfficer.setIsActive(true);
        targetOfficer.setApprovedByOfficer(reviewer);
        targetOfficer.setApprovedAt(OffsetDateTime.now());
        targetOfficer.setRejectionReason(null);

        PoliceOfficer savedOfficer = officerRepository.save(targetOfficer);

        // Record audit log
        recordAuditLog(savedOfficer, "REGISTRATION_APPROVED", "SUCCESS",
                "Registration approved by " + reviewer.getFullName() + " (" + reviewer.getPoliceId() +
                ") [Registration ID: " + savedOfficer.getId() + ", Station ID: " + (savedOfficer.getStation() != null ? savedOfficer.getStation().getId() : "N/A") + "]",
                httpRequest);

        // Create applicant in-app notification
        try {
            if (notificationRepository != null) {
                Notification notif = new Notification();
                notif.setRecipientOfficer(savedOfficer);
                notif.setChannel(NotificationChannel.IN_APP);
                notif.setNotificationType(NotificationType.OFFICER_ACCOUNT_APPROVED);
                notif.setSubject("YOUR OFFICER ACCOUNT HAS BEEN APPROVED");
                notif.setMessage("Your CrimeSphere AI officer account (Police ID: " + savedOfficer.getPoliceId() +
                        ") has been approved and activated by Station Head " + reviewer.getFullName() + ". You may now log in to the command center.");
                notif.setStatus(NotificationStatus.SENT);
                notif.setSeverity("INFO");
                notif.setReferenceType("OFFICER_REGISTRATION");
                notif.setReferenceId(savedOfficer.getId());
                notif.setSentAt(OffsetDateTime.now());
                notificationRepository.save(notif);
            }
        } catch (Exception e) {
            logger.warn("Failed to create in-app notification for approved officer {}: {}", savedOfficer.getPoliceId(), e.getMessage());
        }

        // Notify applicant via Resend email
        if (savedOfficer.getEmail() != null && !savedOfficer.getEmail().isBlank()) {
            try {
                String subject = "CrimeSphere AI — Registration Approved";
                String body = resendEmailService.buildOfficerRegistrationDecisionBody(
                        savedOfficer.getFullName(), savedOfficer.getPoliceId(), true, null);
                resendEmailService.sendEmail(savedOfficer.getEmail(), subject, body);
            } catch (Exception e) {
                logger.warn("Could not dispatch registration approval email to {}: {}", savedOfficer.getEmail(), e.getMessage());
            }
        }

        return new PendingOfficerRegistrationDto(savedOfficer);
    }

    /**
     * Rejects an officer registration with a required reason.
     */
    @Transactional
    public PendingOfficerRegistrationDto rejectRegistration(UUID officerId, String rejectionReason,
                                                            PoliceOfficer reviewer, HttpServletRequest httpRequest) {
        if (rejectionReason == null || rejectionReason.trim().isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required.");
        }

        PoliceOfficer targetOfficer = officerRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer registration record not found with ID: " + officerId));

        validateStationAccess(targetOfficer, reviewer);

        targetOfficer.setStatus("REJECTED");
        targetOfficer.setIsActive(false);
        targetOfficer.setRejectionReason(rejectionReason.trim());
        targetOfficer.setApprovedByOfficer(reviewer);
        targetOfficer.setApprovedAt(OffsetDateTime.now());

        PoliceOfficer savedOfficer = officerRepository.save(targetOfficer);

        // Record audit log
        recordAuditLog(savedOfficer, "REGISTRATION_REJECTED", "SUCCESS",
                "Registration rejected by " + reviewer.getFullName() + " (" + reviewer.getPoliceId() +
                ") [Registration ID: " + savedOfficer.getId() + ", Station ID: " + (savedOfficer.getStation() != null ? savedOfficer.getStation().getId() : "N/A") +
                ", Reason: " + rejectionReason.trim() + "]",
                httpRequest);

        // Create applicant in-app notification
        try {
            if (notificationRepository != null) {
                Notification notif = new Notification();
                notif.setRecipientOfficer(savedOfficer);
                notif.setChannel(NotificationChannel.IN_APP);
                notif.setNotificationType(NotificationType.OFFICER_REGISTRATION_REJECTED);
                notif.setSubject("YOUR OFFICER REGISTRATION WAS REJECTED");
                notif.setMessage("Your CrimeSphere AI officer registration (Police ID: " + savedOfficer.getPoliceId() +
                        ") was rejected by the Station Head. Reason: " + rejectionReason.trim());
                notif.setStatus(NotificationStatus.SENT);
                notif.setSeverity("WARNING");
                notif.setReferenceType("OFFICER_REGISTRATION");
                notif.setReferenceId(savedOfficer.getId());
                notif.setSentAt(OffsetDateTime.now());
                notificationRepository.save(notif);
            }
        } catch (Exception e) {
            logger.warn("Failed to create in-app notification for rejected officer {}: {}", savedOfficer.getPoliceId(), e.getMessage());
        }

        // Notify applicant via Resend email
        if (savedOfficer.getEmail() != null && !savedOfficer.getEmail().isBlank()) {
            try {
                String subject = "CrimeSphere AI — Registration Update";
                String body = resendEmailService.buildOfficerRegistrationDecisionBody(
                        savedOfficer.getFullName(), savedOfficer.getPoliceId(), false, rejectionReason.trim());
                resendEmailService.sendEmail(savedOfficer.getEmail(), subject, body);
            } catch (Exception e) {
                logger.warn("Could not dispatch registration rejection email to {}: {}", savedOfficer.getEmail(), e.getMessage());
            }
        }

        return new PendingOfficerRegistrationDto(savedOfficer);
    }

    /**
     * Loads the sensitive ID document resource with strict station authorization.
     */
    @Transactional(readOnly = true)
    public Resource getOfficerIdDocumentResource(UUID officerId, PoliceOfficer currentOfficer, HttpServletRequest httpRequest) {
        PoliceOfficer targetOfficer = officerRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer registration record not found with ID: " + officerId));

        validateStationAccess(targetOfficer, currentOfficer);

        if (targetOfficer.getIdDocumentPath() == null || targetOfficer.getIdDocumentPath().isBlank()) {
            throw new ResourceNotFoundException("No ID document on file for this registration.");
        }

        recordAuditLog(targetOfficer, "REGISTRATION_VIEWED", "SUCCESS",
                "ID document accessed by " + currentOfficer.getFullName() + " (" + currentOfficer.getPoliceId() + ")",
                httpRequest);

        return storageService.loadOfficerIdDocumentResource(targetOfficer.getIdDocumentPath());
    }

    /**
     * Strictly enforces that the reviewer's station matches the registration's station.
     */
    private void validateStationAccess(PoliceOfficer targetOfficer, PoliceOfficer currentOfficer) {
        if (currentOfficer == null || currentOfficer.getStation() == null) {
            throw new AccessDeniedException("Access denied: You are not assigned to a police station.");
        }

        if (targetOfficer.getStation() == null) {
            throw new AccessDeniedException("Access denied: Target officer does not have an assigned station.");
        }

        // Allow super admin or same station
        boolean isSameStation = targetOfficer.getStation().getId().equals(currentOfficer.getStation().getId());
        boolean isSuperAdmin = "SUPER_ADMIN".equalsIgnoreCase(currentOfficer.getRole());

        if (!isSameStation && !isSuperAdmin) {
            logger.warn("Station isolation violation attempt! Officer {} (Station {}) attempted to access Registration {} (Station {})",
                    currentOfficer.getPoliceId(), currentOfficer.getStation().getStationCode(),
                    targetOfficer.getPoliceId(), targetOfficer.getStation().getStationCode());
            throw new AccessDeniedException("Access denied: You cannot view or modify registrations for another police station.");
        }
    }

    private void recordAuditLog(PoliceOfficer officer, String action, String status, String details, HttpServletRequest request) {
        try {
            String clientIp = extractClientIp(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : "Internal";
            if (details != null && !details.isBlank()) {
                userAgent = userAgent + " | " + details;
            }
            AuditLog log = new AuditLog(null, officer, action, status, clientIp, userAgent);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to write audit log for action {}: {}", action, e.getMessage());
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
