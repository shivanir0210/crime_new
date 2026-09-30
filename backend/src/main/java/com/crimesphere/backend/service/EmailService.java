package com.crimesphere.backend.service;

import com.crimesphere.backend.entity.Notification;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.entity.PoliceStation;
import com.crimesphere.backend.repository.NotificationAuditLogRepository;
import com.crimesphere.backend.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    private final NotificationRepository notificationRepository;
    private final NotificationAuditLogRepository auditLogRepository;

    @Value("${app.mail.from:alerts@crimesphere.internal}")
    private String mailFrom;

    public EmailService(JavaMailSender mailSender,
                        NotificationRepository notificationRepository,
                        NotificationAuditLogRepository auditLogRepository) {
        this.mailSender = mailSender;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Send automatic security login notification to Station Head.
     * Email failure is safely handled and will NOT interrupt user authentication.
     */
    public void sendStationHeadLoginNotification(PoliceOfficer officer, PoliceStation station) {
        if (station == null || station.getHeadEmail() == null || station.getHeadEmail().isBlank()) {
            logger.info("No Station Head email configured for station: {}", station != null ? station.getStationCode() : "UNKNOWN");
            return;
        }

        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
        String subject = "CrimeSphere AI — Police Officer Login Alert";

        String body = String.format(
                "CrimeSphere AI Security Notification\n\n" +
                "A police officer has successfully logged into the CrimeSphere investigation system.\n\n" +
                "Officer: %s\n" +
                "Police ID: %s\n" +
                "Police Station: %s\n" +
                "Login Time: %s\n" +
                "Role: %s\n" +
                "Status: SUCCESSFUL LOGIN\n\n" +
                "Please review the system dashboard if this login requires attention.\n\n" +
                "This is an automated security notification from CrimeSphere AI.",
                officer.getFullName(),
                officer.getPoliceId(),
                station.getStationName(),
                timestamp,
                officer.getRole()
        );

        Notification notification = new Notification();
        notification.setRecipientOfficer(officer);
        notification.setChannel(com.crimesphere.backend.entity.NotificationChannel.EMAIL);
        notification.setNotificationType(com.crimesphere.backend.entity.NotificationType.LOGIN_ALERT);
        notification.setSubject(subject);
        notification.setMessage(body);
        notification.setRecipientEmail(station.getHeadEmail());
        notification.setStatus(com.crimesphere.backend.entity.NotificationStatus.SENDING);
        notification.setAttemptCount(1);

        if (notificationRepository != null) {
            try {
                notification = notificationRepository.save(notification);
            } catch (Exception e) {
                logger.warn("Could not save initial login notification record: {}", e.getMessage());
            }
        }

        if (mailSender == null) {
            logger.info("JavaMailSender not configured. Skipping email dispatch to Station Head: {}", station.getHeadEmail());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(station.getHeadEmail());
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            if (notification != null && notification.getId() != null && notificationRepository != null) {
                notification.setStatus(com.crimesphere.backend.entity.NotificationStatus.SENT);
                notification.setSentAt(java.time.OffsetDateTime.now());
                notification.setExternalMessageId("msg-login-" + UUID.randomUUID().toString().substring(0, 8));
                notificationRepository.save(notification);

                recordAudit(notification, officer, com.crimesphere.backend.entity.NotificationAuditAction.NOTIFICATION_SENT,
                        "Login alert sent to Station Head: " + station.getHeadEmail());
            }

            logger.info("Security login notification email sent successfully to Station Head: {}", station.getHeadEmail());
        } catch (Exception e) {
            // Non-blocking: Log mail failure without affecting authentication
            logger.warn("Station head login notification failed for station {}: {}",
                    station.getStationCode(), e.getMessage());

            if (notification.getId() != null) {
                notification.setStatus(com.crimesphere.backend.entity.NotificationStatus.FAILED);
                notification.setLastError(e.getMessage());
                notificationRepository.save(notification);

                recordAudit(notification, officer, com.crimesphere.backend.entity.NotificationAuditAction.NOTIFICATION_FAILED,
                        "Login alert failed: " + e.getMessage());
            }
        }
    }

    private void recordAudit(Notification notification, PoliceOfficer officer,
                             com.crimesphere.backend.entity.NotificationAuditAction action, String details) {
        try {
            com.crimesphere.backend.entity.NotificationAuditLog log =
                    new com.crimesphere.backend.entity.NotificationAuditLog(
                            notification, null, officer, action, details
                    );
            auditLogRepository.save(log);
        } catch (Exception ignored) {
        }
    }
}
