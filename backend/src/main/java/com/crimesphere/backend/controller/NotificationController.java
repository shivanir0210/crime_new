package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.case_mgmt.PageResponse;
import com.crimesphere.backend.dto.notification.*;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.NotificationPreferenceRepository;
import com.crimesphere.backend.repository.NotificationRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import com.crimesphere.backend.service.NotificationService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final PoliceOfficerRepository officerRepository;
    private final CaseRepository caseRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationController(NotificationRepository notificationRepository,
                                  NotificationService notificationService,
                                  PoliceOfficerRepository officerRepository,
                                  CaseRepository caseRepository,
                                  NotificationPreferenceRepository preferenceRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.officerRepository = officerRepository;
        this.caseRepository = caseRepository;
        this.preferenceRepository = preferenceRepository;
    }

    /**
     * GET /api/notifications
     * Lists notifications visible to the authenticated officer (station or recipient isolated).
     */
    @GetMapping("/notifications")
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @RequestParam(required = false) UUID caseId,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) NotificationType notificationType,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        UUID stationId = officer.getStation() != null ? officer.getStation().getId() : null;

        // If caseId is provided, verify station access
        if (caseId != null && stationId != null) {
            caseRepository.findByIdAndStationId(caseId, stationId)
                    .orElseThrow(() -> new AccessDeniedException("Case does not belong to your assigned police station"));
        }

        Specification<Notification> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Recipient visibility predicate:
            // Matches if the notification is explicitly addressed to this officer OR if recipient is null and notification belongs to station
            Predicate isRecipient = cb.equal(root.get("recipientOfficer").get("id"), officer.getId());
            if (stationId != null) {
                Predicate isStationBroadcast = cb.and(
                        cb.isNull(root.get("recipientOfficer")),
                        cb.isNotNull(root.get("caseEntity")),
                        cb.equal(root.get("caseEntity").get("station").get("id"), stationId)
                );
                predicates.add(cb.or(isRecipient, isStationBroadcast));
            } else {
                predicates.add(isRecipient);
            }

            if (caseId != null) {
                predicates.add(cb.equal(root.get("caseEntity").get("id"), caseId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (channel != null) {
                predicates.add(cb.equal(root.get("channel"), channel));
            }
            if (notificationType != null) {
                predicates.add(cb.equal(root.get("notificationType"), notificationType));
            }
            if (Boolean.TRUE.equals(unreadOnly)) {
                predicates.add(cb.equal(root.get("isRead"), false));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notificationPage = notificationRepository.findAll(spec, pageable);
        Page<NotificationResponse> responsePage = notificationPage.map(NotificationResponse::new);

        return ResponseEntity.ok(new PageResponse<>(responsePage));
    }

    /**
     * GET /api/notifications/unread-count
     */
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<java.util.Map<String, Long>> getUnreadCount() {
        PoliceOfficer officer = getAuthenticatedOfficer();
        long count = notificationService.getUnreadCount(officer);
        return ResponseEntity.ok(java.util.Collections.singletonMap("unreadCount", count));
    }

    /**
     * PATCH /api/notifications/{id}/read
     */
    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID id) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        Notification updated = notificationService.markAsRead(id, officer);
        return ResponseEntity.ok(new NotificationResponse(updated));
    }

    /**
     * PATCH /api/notifications/read-all
     */
    @PatchMapping("/notifications/read-all")
    public ResponseEntity<java.util.Map<String, String>> markAllAsRead() {
        PoliceOfficer officer = getAuthenticatedOfficer();
        notificationService.markAllAsRead(officer);
        return ResponseEntity.ok(java.util.Collections.singletonMap("status", "SUCCESS"));
    }

    /**
     * GET /api/cases/{caseId}/notifications
     */
    @GetMapping("/cases/{caseId}/notifications")
    public ResponseEntity<PageResponse<NotificationResponse>> getCaseNotifications(
            @PathVariable UUID caseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return getNotifications(caseId, null, null, null, null, page, size);
    }

    /**
     * GET /api/notifications/{id}
     * Returns a single notification only if authorized.
     */
    @GetMapping("/notifications/{id}")
    public ResponseEntity<NotificationResponse> getNotificationById(@PathVariable UUID id) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        UUID stationId = officer.getStation() != null ? officer.getStation().getId() : null;

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification record not found"));

        // Station authorization check
        boolean authorized = false;
        if (notification.getRecipientOfficer() != null &&
            notification.getRecipientOfficer().getId().equals(officer.getId())) {
            authorized = true;
        } else if (stationId != null && notification.getCaseEntity() != null &&
                   notification.getCaseEntity().getStation() != null &&
                   notification.getCaseEntity().getStation().getId().equals(stationId)) {
            authorized = true;
        }

        if (!authorized) {
            throw new AccessDeniedException("You are not authorized to view this notification");
        }

        return ResponseEntity.ok(new NotificationResponse(notification));
    }

    /**
     * GET /api/notification-preferences
     * Returns notification preferences for the authenticated officer.
     */
    @GetMapping("/notification-preferences")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences() {
        PoliceOfficer officer = getAuthenticatedOfficer();
        NotificationPreference pref = notificationService.getOrCreatePreferences(officer);
        return ResponseEntity.ok(new NotificationPreferenceResponse(pref));
    }

    /**
     * PUT /api/notification-preferences
     * Updates notification preferences for the authenticated officer.
     */
    @PutMapping("/notification-preferences")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
            @RequestBody UpdateNotificationPreferenceRequest request) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        NotificationPreference pref = notificationService.getOrCreatePreferences(officer);

        if (request.getEmailEnabled() != null) pref.setEmailEnabled(request.getEmailEnabled());
        if (request.getWhatsappEnabled() != null) pref.setWhatsappEnabled(request.getWhatsappEnabled());
        if (request.getCaseCreatedEnabled() != null) pref.setCaseCreatedEnabled(request.getCaseCreatedEnabled());
        if (request.getCaseStatusEnabled() != null) pref.setCaseStatusEnabled(request.getCaseStatusEnabled());
        if (request.getCasePriorityEnabled() != null) pref.setCasePriorityEnabled(request.getCasePriorityEnabled());
        if (request.getAiReviewEnabled() != null) pref.setAiReviewEnabled(request.getAiReviewEnabled());
        if (request.getEvidenceStatusEnabled() != null) pref.setEvidenceStatusEnabled(request.getEvidenceStatusEnabled());

        NotificationPreference saved = preferenceRepository.save(pref);
        return ResponseEntity.ok(new NotificationPreferenceResponse(saved));
    }

    /**
     * POST /api/cases/{caseId}/notifications/test
     * Development test endpoint for sending a test EMAIL or WHATSAPP alert.
     */
    @PostMapping("/cases/{caseId}/notifications/test")
    public ResponseEntity<NotificationResponse> sendTestNotification(
            @PathVariable UUID caseId,
            @RequestBody(required = false) TestNotificationRequest request) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        UUID stationId = officer.getStation() != null ? officer.getStation().getId() : null;

        Case caseEntity = caseRepository.findByIdAndStationId(caseId, stationId)
                .orElseThrow(() -> new AccessDeniedException("Case does not belong to your station or was not found"));

        NotificationChannel channel = request != null && request.getChannel() != null
                ? request.getChannel() : NotificationChannel.EMAIL;

        Notification notification = notificationService.sendTestNotification(caseEntity, officer, channel);
        return ResponseEntity.status(HttpStatus.CREATED).body(new NotificationResponse(notification));
    }

    /**
     * POST /api/notifications/{id}/retry
     * Retries email/notification dispatch for a specific notification.
     */
    @PostMapping("/notifications/{id}/retry")
    public ResponseEntity<NotificationResponse> retryNotification(@PathVariable UUID id) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        Notification notification = notificationService.retryNotification(id, officer);
        return ResponseEntity.ok(new NotificationResponse(notification));
    }

    private PoliceOfficer getAuthenticatedOfficer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Unauthenticated request");
        }
        String policeId = authentication.getName();
        return officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated officer not found: " + policeId));
    }
}
