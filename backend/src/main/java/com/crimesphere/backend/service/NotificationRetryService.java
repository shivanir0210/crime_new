package com.crimesphere.backend.service;

import com.crimesphere.backend.entity.Notification;
import com.crimesphere.backend.entity.NotificationAuditAction;
import com.crimesphere.backend.entity.NotificationAuditLog;
import com.crimesphere.backend.entity.NotificationStatus;
import com.crimesphere.backend.repository.NotificationAuditLogRepository;
import com.crimesphere.backend.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationRetryService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationRetryService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationAuditLogRepository auditLogRepository;
    private final NotificationService notificationService;

    @Value("${notification.max-retries:3}")
    private int maxRetries;

    public NotificationRetryService(NotificationRepository notificationRepository,
                                    NotificationAuditLogRepository auditLogRepository,
                                    NotificationService notificationService) {
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
    }

    /**
     * Periodically retries transient failed notifications.
     * Runs every 60 seconds.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void retryFailedNotifications() {
        List<Notification> retryCandidates = notificationRepository.findByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
                List.of(NotificationStatus.FAILED, NotificationStatus.RETRYING), maxRetries
        );

        if (retryCandidates.isEmpty()) {
            return;
        }

        logger.info("Found {} notifications eligible for retry", retryCandidates.size());

        for (Notification notification : retryCandidates) {
            // Check if failure was permanent (missing credentials or invalid recipient)
            if (isPermanentFailure(notification.getLastError())) {
                logger.debug("Skipping notification {} with permanent failure: {}",
                        notification.getId(), notification.getLastError());
                continue;
            }

            try {
                notification.setStatus(NotificationStatus.RETRYING);
                notificationRepository.save(notification);

                recordAudit(notification, NotificationAuditAction.NOTIFICATION_RETRIED,
                        "Automatic retry attempt " + (notification.getAttemptCount() + 1));

                notificationService.executeDispatch(notification, "Retry dispatch by CrimeSphere background worker.");
            } catch (Exception e) {
                logger.warn("Retry failed for notification {}: {}", notification.getId(), e.getMessage());
            }
        }
    }

    private boolean isPermanentFailure(String lastError) {
        if (lastError == null) return false;
        String lower = lastError.toLowerCase();
        return lower.contains("not configured") ||
               lower.contains("missing") ||
               lower.contains("invalid recipient") ||
               lower.contains("malformed");
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
            logger.warn("Failed to record retry audit log: {}", e.getMessage());
        }
    }
}
