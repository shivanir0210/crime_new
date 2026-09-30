package com.crimesphere.backend.service;

import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationAuditLogRepository auditLogRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final PoliceOfficerRepository officerRepository;
    private final CaseOfficerRepository caseOfficerRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final EmailNotificationService emailService;
    private final WhatsAppNotificationService whatsAppService;

    public NotificationService() {
        this(null, null, null, null, null, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public NotificationService(NotificationRepository notificationRepository,
                               NotificationAuditLogRepository auditLogRepository,
                               NotificationPreferenceRepository preferenceRepository,
                               PoliceOfficerRepository officerRepository,
                               CaseOfficerRepository caseOfficerRepository,
                               NotificationDeliveryRepository deliveryRepository,
                               EmailNotificationService emailService,
                               WhatsAppNotificationService whatsAppService) {
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.preferenceRepository = preferenceRepository;
        this.officerRepository = officerRepository;
        this.caseOfficerRepository = caseOfficerRepository;
        this.deliveryRepository = deliveryRepository;
        this.emailService = emailService;
        this.whatsAppService = whatsAppService;
    }

    /**
     * Resolves recipients for a case event and dispatches notifications.
     * Guaranteed non-blocking: catches all exceptions so calling transaction is never broken.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatchCaseNotification(Case caseEntity,
                                         NotificationType notificationType,
                                         String subject,
                                         String message,
                                         String actionRequired,
                                         PoliceOfficer triggerOfficer) {
        if (caseEntity == null) {
            logger.warn("Cannot dispatch notification: case is null");
            return;
        }

        try {
            // Idempotency check for general case events (EVIDENCE_ADDED and EVIDENCE_UPLOADED bypass coarse case-level suppression)
            if (notificationType != NotificationType.EVIDENCE_ADDED && notificationType != NotificationType.EVIDENCE_UPLOADED) {
                OffsetDateTime tenSecondsAgo = OffsetDateTime.now().minusSeconds(10);
                Optional<Notification> recent = notificationRepository
                        .findFirstByCaseEntityIdAndNotificationTypeAndCreatedAtAfterOrderByCreatedAtDesc(
                                caseEntity.getId(), notificationType, tenSecondsAgo);
                if (recent.isPresent()) {
                    logger.info("Skipping duplicate notification {} for case {}", notificationType, caseEntity.getCaseNumber());
                    return;
                }
            }

            // Resolve target officers for this case
            List<PoliceOfficer> recipients = resolveRecipients(caseEntity, notificationType, triggerOfficer);
            List<String> recipientOfficerIds = recipients.stream()
                    .map(r -> r.getPoliceId() + " (" + r.getId() + ")")
                    .toList();
            logger.info("Case {}: Resolved {} recipients for notification type {}: {}",
                    caseEntity.getCaseNumber(), recipients.size(), notificationType, recipientOfficerIds);

            for (PoliceOfficer recipient : recipients) {
                NotificationPreference prefs = getOrCreatePreferences(recipient);

                if (!isNotificationTypeEnabled(prefs, notificationType)) {
                    logger.debug("Officer {} opted out of notification type {}", recipient.getPoliceId(), notificationType);
                    continue;
                }

                // 1. Dispatch In-App Notification (Always enabled for assigned team members)
                createAndSendNotification(
                        caseEntity,
                        recipient,
                        NotificationChannel.IN_APP,
                        notificationType,
                        subject != null ? subject : "CrimeSphere AI — Case Alert",
                        message,
                        actionRequired,
                        null,
                        null
                );

                // 2. Dispatch Email if enabled (Recipient dynamically loaded from police_officers.email)
                if (prefs.isEmailEnabled()) {
                    createAndSendNotification(
                            caseEntity,
                            recipient,
                            NotificationChannel.EMAIL,
                            notificationType,
                            subject != null ? subject : "CrimeSphere AI — Case Alert",
                            message,
                            actionRequired,
                            recipient.getEmail(),
                            null
                    );
                }

                // 3. Dispatch WhatsApp if enabled
                if (prefs.isWhatsappEnabled() && recipient.getPhone() != null && !recipient.getPhone().isBlank()) {
                    createAndSendNotification(
                            caseEntity,
                            recipient,
                            NotificationChannel.WHATSAPP,
                            notificationType,
                            subject != null ? subject : "CrimeSphere AI — Case Alert",
                            message,
                            actionRequired,
                            null,
                            recipient.getPhone()
                    );
                }
            }

            // Also check Station Head email if station has head configured and head is not in recipients list
            PoliceStation station = caseEntity.getStation();
            if (station != null && station.getHeadEmail() != null && !station.getHeadEmail().isBlank()) {
                boolean headAlreadyNotified = recipients.stream()
                        .anyMatch(r -> station.getHeadEmail().equalsIgnoreCase(r.getEmail()));
                if (!headAlreadyNotified) {
                    createAndSendNotification(
                            caseEntity,
                            null,
                            NotificationChannel.EMAIL,
                            notificationType,
                            subject != null ? subject : "CrimeSphere AI — Station Alert",
                            message,
                            actionRequired,
                            station.getHeadEmail(),
                            null
                    );
                }
            }

        } catch (Exception e) {
            // Non-blocking: log error and protect caller transaction
            logger.error("Failed to dispatch case notification safely: {}", e.getMessage(), e);
        }
    }

    /**
     * Creates and dispatches a single notification record with full status tracking and audit logging.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createAndSendNotification(Case caseEntity,
                                                  PoliceOfficer recipient,
                                                  NotificationChannel channel,
                                                  NotificationType type,
                                                  String subject,
                                                  String message,
                                                  String actionRequired,
                                                  String recipientEmail,
                                                  String recipientPhone) {
        Notification notification = new Notification();
        notification.setCaseEntity(caseEntity);
        notification.setRecipientOfficer(recipient);
        notification.setChannel(channel);
        notification.setNotificationType(type);
        notification.setSubject(subject);
        notification.setMessage(message);
        notification.setRecipientEmail(recipientEmail);
        notification.setRecipientPhone(recipientPhone);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setAttemptCount(0);

        String idempotencyKey = String.format("%s_%s_%s_%s_%s",
                caseEntity != null ? caseEntity.getId() : "SYS",
                type,
                recipient != null ? recipient.getId() : "DEST",
                channel,
                System.currentTimeMillis() / 10000);
        notification.setIdempotencyKey(idempotencyKey);

        if (notificationRepository != null) {
            Optional<Notification> existing = notificationRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                logger.info("Skipping duplicate notification with idempotencyKey: {}. Creation result: DUPLICATE_SKIPPED (existing ID: {})",
                        idempotencyKey, existing.get().getId());
                return existing.get();
            }
        }

        Notification saved = notificationRepository.save(notification);
        logger.info("Notification creation result: CREATED ID={}, type={}, channel={}, recipient={}, case={}",
                saved.getId(), type, channel, recipient != null ? recipient.getPoliceId() : "N/A",
                caseEntity != null ? caseEntity.getCaseNumber() : "N/A");

        // Record creation audit log
        recordAudit(saved, NotificationAuditAction.NOTIFICATION_CREATED,
                String.format("Notification created for case %s via %s",
                        caseEntity != null ? caseEntity.getCaseNumber() : "SYSTEM", channel));

        // Attempt immediate dispatch
        return executeDispatch(saved, actionRequired);
    }

    /**
     * Executes dispatch to the appropriate provider (In-App, Email or WhatsApp).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification executeDispatch(Notification notification, String actionRequired) {
        notification.setAttemptCount(notification.getAttemptCount() + 1);

        String caseNumber = notification.getCaseEntity() != null ? notification.getCaseEntity().getCaseNumber() : "SYSTEM";
        String caseTitle = notification.getCaseEntity() != null ? notification.getCaseEntity().getTitle() : "System Alert";

        if (notification.getChannel() == NotificationChannel.IN_APP) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(OffsetDateTime.now());
            Notification saved = notificationRepository.save(notification);
            createDeliveryRecord(saved, NotificationStatus.SENT, "IN_APP_DELIVERED", null);
            return saved;
        }

        notification.setStatus(NotificationStatus.SENDING);
        notificationRepository.save(notification);

        NotificationDispatchResult result;
        if (notification.getChannel() == NotificationChannel.EMAIL) {
            if (notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) {
                logger.warn("Assigned officer email address is not configured for notification ID: {}", notification.getId());
                notification.setStatus(NotificationStatus.FAILED);
                notification.setLastError("Assigned officer email address is not configured.");
                Notification updated = notificationRepository.save(notification);
                createDeliveryRecord(updated, NotificationStatus.FAILED, null, "Assigned officer email address is not configured.");
                recordAudit(updated, NotificationAuditAction.NOTIFICATION_FAILED, "Assigned officer email address is not configured.");
                return updated;
            }

            String emailBody;
            if (notification.getNotificationType() == NotificationType.EVIDENCE_UPLOADED || notification.getNotificationType() == NotificationType.EVIDENCE_ADDED) {
                emailBody = emailService.buildEvidenceUploadedBody(
                        caseNumber,
                        caseTitle,
                        "EVD-" + (notification.getId() != null ? notification.getId().toString().substring(0, 8) : "REF"),
                        notification.getSubject(),
                        notification.getRecipientOfficer() != null ? notification.getRecipientOfficer().getFullName() : "Assigned Officer",
                        actionRequired != null ? actionRequired : "Review newly logged evidence in the case dossier."
                );
            } else if (notification.getNotificationType() == NotificationType.AI_REVIEW_REQUIRED) {
                emailBody = emailService.buildAiReviewRequiredBody(
                        caseNumber,
                        caseTitle,
                        notification.getMessage(),
                        "SUPPORTED / PARTIALLY_SUPPORTED"
                );
            } else {
                emailBody = emailService.buildCaseAlertBody(
                        caseNumber,
                        caseTitle,
                        notification.getMessage(),
                        actionRequired != null ? actionRequired : "Please review in CrimeSphere AI command center."
                );
            }
            result = emailService.sendEmail(notification.getRecipientEmail(), notification.getSubject(), emailBody);
        } else { // WHATSAPP
            if (!whatsAppService.isMockEnabled() && !whatsAppService.isConfigured()) {
                notification.setStatus(NotificationStatus.SKIPPED);
                notification.setLastError("WHATSAPP_NOT_CONFIGURED");
                Notification updated = notificationRepository.save(notification);
                createDeliveryRecord(updated, NotificationStatus.SKIPPED, null, "WHATSAPP_NOT_CONFIGURED");
                recordAudit(updated, NotificationAuditAction.NOTIFICATION_SKIPPED, "WhatsApp provider not configured");
                return updated;
            }
            result = whatsAppService.sendWhatsApp(
                    notification.getRecipientPhone(),
                    caseNumber,
                    caseTitle,
                    notification.getMessage(),
                    actionRequired != null ? actionRequired : "Please review in CrimeSphere AI command center."
            );
        }

        if (result.isSuccess()) {
            if (result.isMock()) {
                notification.setStatus(NotificationStatus.MOCK_SENT);
                notification.setSentAt(OffsetDateTime.now());
                notification.setExternalMessageId(result.getExternalMessageId());
                notification.setLastError("DEVELOPMENT MOCK — NOT ACTUALLY SENT");
                Notification updated = notificationRepository.save(notification);
                createDeliveryRecord(updated, NotificationStatus.MOCK_SENT, result.getExternalMessageId(), "DEVELOPMENT MOCK — NOT ACTUALLY SENT");
                recordAudit(updated, NotificationAuditAction.NOTIFICATION_SENT,
                        String.format("Simulated dispatch via %s (MessageId: %s) [MOCK_SENT]", notification.getChannel(), result.getExternalMessageId()));
                return updated;
            }

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(OffsetDateTime.now());
            notification.setExternalMessageId(result.getExternalMessageId());
            notification.setLastError(null);
            Notification updated = notificationRepository.save(notification);
            createDeliveryRecord(updated, NotificationStatus.SENT, result.getExternalMessageId(), null);

            recordAudit(updated, NotificationAuditAction.NOTIFICATION_SENT,
                    String.format("Dispatched via %s (MessageId: %s)",
                            notification.getChannel(), result.getExternalMessageId()));
            return updated;
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setLastError(result.getErrorMessage());
            Notification updated = notificationRepository.save(notification);
            createDeliveryRecord(updated, NotificationStatus.FAILED, null, result.getErrorMessage());

            recordAudit(updated, NotificationAuditAction.NOTIFICATION_FAILED,
                    String.format("Failed via %s: %s", notification.getChannel(), result.getErrorMessage()));
            return updated;
        }
    }

    /**
     * Sends a development-only test notification for a case to the authenticated officer.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification sendTestNotification(Case caseEntity, PoliceOfficer officer, NotificationChannel channel) {
        String testSubject = "CrimeSphere AI [DEV TEST] — Case Alert";
        String testMessage = String.format("[DEVELOPMENT TEST NOTIFICATION]\nThis is a test notification for case %s (%s).",
                caseEntity.getCaseNumber(), caseEntity.getTitle());
        String actionRequired = "No action required. This is an automated system connectivity test.";

        String email = channel == NotificationChannel.EMAIL ? officer.getEmail() : null;
        String phone = channel == NotificationChannel.WHATSAPP ? officer.getPhone() : null;

        return createAndSendNotification(
                caseEntity,
                officer,
                channel,
                NotificationType.CASE_ALERT,
                testSubject,
                testMessage,
                actionRequired,
                email,
                phone
        );
    }

    /**
     * Resolves appropriate police officer recipients for a case event.
     * Queries active case_officers and excludes triggerOfficer per Section 7 rules.
     */
    public List<PoliceOfficer> resolveRecipients(Case caseEntity, NotificationType type, PoliceOfficer triggerOfficer) {
        Set<PoliceOfficer> recipientSet = new LinkedHashSet<>();

        // Add case creator
        if (caseEntity.getCreatedBy() != null) {
            recipientSet.add(caseEntity.getCreatedBy());
        }

        // Add active assigned officers for this case
        if (caseOfficerRepository != null) {
            List<CaseOfficer> assignments = caseOfficerRepository.findByCaseEntityIdAndIsActiveTrue(caseEntity.getId());
            for (CaseOfficer co : assignments) {
                if (co.getPoliceOfficer() != null) {
                    recipientSet.add(co.getPoliceOfficer());
                }
            }
        }

        // Exclude triggerOfficer (acting officer who performed the action)
        if (triggerOfficer != null) {
            recipientSet.removeIf(officer -> officer.getId().equals(triggerOfficer.getId()));
        }

        return new ArrayList<>(recipientSet);
    }

    /**
     * Checks if officer has enabled the given event notification type.
     * Critical security alerts (LOGIN_ALERT) are always enabled.
     */
    public boolean isNotificationTypeEnabled(NotificationPreference prefs, NotificationType type) {
        if (type == NotificationType.LOGIN_ALERT) {
            return true; // Mandatory security policy
        }
        if (prefs == null) {
            return true;
        }
        return switch (type) {
            case CASE_CREATED -> prefs.isCaseCreatedEnabled();
            case CASE_STATUS_CHANGED -> prefs.isCaseStatusEnabled();
            case CASE_PRIORITY_CHANGED -> prefs.isCasePriorityEnabled();
            case AI_REVIEW_REQUIRED, AI_RECOMMENDATION_APPROVED, AI_RECOMMENDATION_EDITED, AI_RECOMMENDATION_REJECTED -> prefs.isAiReviewEnabled();
            case EVIDENCE_STATUS_CHANGED, EVIDENCE_ADDED, EVIDENCE_UPLOADED -> prefs.isEvidenceStatusEnabled();
            case CASE_ALERT -> true;
            default -> true;
        };
    }

    /**
     * Gets or creates default preferences for an officer.
     */
    @Transactional
    public NotificationPreference getOrCreatePreferences(PoliceOfficer officer) {
        if (preferenceRepository == null) {
            return new NotificationPreference(officer);
        }
        return preferenceRepository.findByPoliceOfficerId(officer.getId())
                .orElseGet(() -> {
                    NotificationPreference newPref = new NotificationPreference(officer);
                    NotificationPreference saved = preferenceRepository.save(newPref);
                    return saved != null ? saved : newPref;
                });
    }

    /**
     * Gets unread notification count for an officer.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(PoliceOfficer officer) {
        return notificationRepository.countByRecipientOfficerIdAndIsReadFalse(officer.getId());
    }

    /**
     * Fetches notifications for an officer with pagination.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Notification> getOfficerNotifications(PoliceOfficer officer, boolean unreadOnly, org.springframework.data.domain.Pageable pageable) {
        if (unreadOnly) {
            return notificationRepository.findByRecipientOfficerIdAndIsReadFalseOrderByCreatedAtDesc(officer.getId(), pageable);
        }
        return notificationRepository.findByRecipientOfficerIdOrderByCreatedAtDesc(officer.getId(), pageable);
    }

    /**
     * Marks a notification as read.
     */
    @Transactional
    public Notification markAsRead(UUID notificationId, PoliceOfficer officer) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (notification.getRecipientOfficer() != null && !notification.getRecipientOfficer().getId().equals(officer.getId())) {
            throw new ResourceNotFoundException("Notification not found for officer");
        }

        notification.setRead(true);
        notification.setReadAt(OffsetDateTime.now());
        Notification saved = notificationRepository.save(notification);

        recordAudit(saved, NotificationAuditAction.NOTIFICATION_READ, "Notification marked as read");
        return saved;
    }

    /**
     * Marks all notifications as read for an officer.
     */
    @Transactional
    public void markAllAsRead(PoliceOfficer officer) {
        List<Notification> unread = notificationRepository.findByRecipientOfficerIdAndIsReadFalseOrderByCreatedAtDesc(officer.getId());
        for (Notification n : unread) {
            n.setRead(true);
            n.setReadAt(OffsetDateTime.now());
            notificationRepository.save(n);
        }
    }

    /**
     * Fetches notification history for a case.
     */
    @Transactional(readOnly = true)
    public List<Notification> getCaseNotifications(UUID caseId) {
        return notificationRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
    }

    /**
     * Retries dispatch for a specific notification.
     * Prevents concurrent retries while dispatch is in progress.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification retryNotification(UUID notificationId, PoliceOfficer requestingOfficer) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        boolean isRecipient = notification.getRecipientOfficer() != null &&
                notification.getRecipientOfficer().getId().equals(requestingOfficer.getId());
        boolean isStationOfficer = notification.getCaseEntity() != null &&
                notification.getCaseEntity().getStation() != null &&
                requestingOfficer.getStation() != null &&
                notification.getCaseEntity().getStation().getId().equals(requestingOfficer.getStation().getId());

        if (!isRecipient && !isStationOfficer) {
            throw new AccessDeniedException("Not authorized to retry this notification");
        }

        if (notification.getStatus() == NotificationStatus.SENDING || notification.getStatus() == NotificationStatus.RETRYING) {
            throw new IllegalStateException("Notification retry is already in progress");
        }

        notification.setStatus(NotificationStatus.RETRYING);
        notificationRepository.save(notification);

        recordAudit(notification, NotificationAuditAction.NOTIFICATION_RETRIED,
                "Manual retry initiated by officer " + requestingOfficer.getPoliceId());

        return executeDispatch(notification, "Manual retry dispatch by investigator " + requestingOfficer.getPoliceId());
    }

    private void createDeliveryRecord(Notification notification, NotificationStatus status, String providerMsgId, String error) {
        try {
            if (deliveryRepository == null) return;
            NotificationDelivery delivery = new NotificationDelivery();
            delivery.setNotification(notification);
            delivery.setCaseEntity(notification.getCaseEntity());
            delivery.setRecipientOfficer(notification.getRecipientOfficer());
            delivery.setChannel(notification.getChannel());
            delivery.setEventType(notification.getNotificationType());
            delivery.setRecipientAddress(notification.getRecipientEmail() != null ? notification.getRecipientEmail() : notification.getRecipientPhone());
            delivery.setStatus(status);
            delivery.setProviderMessageId(providerMsgId);
            delivery.setLastError(error);
            delivery.setIdempotencyKey(notification.getIdempotencyKey());
            if (status == NotificationStatus.SENT) {
                delivery.setSentAt(OffsetDateTime.now());
            }
            deliveryRepository.save(delivery);
        } catch (Exception e) {
            logger.warn("Failed to create delivery record: {}", e.getMessage());
        }
    }

    private void recordAudit(Notification notification, NotificationAuditAction action, String details) {
        try {
            NotificationAuditLog log = new NotificationAuditLog(
                    notification,
                    notification.getCaseEntity(),
                    notification.getRecipientOfficer(),
                    action,
                    details
            );
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to record notification audit log: {}", e.getMessage());
        }
    }
}
