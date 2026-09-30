package com.crimesphere.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for dispatching case and investigative email notifications.
 * Uses Resend Email API via ResendEmailService.
 */
@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private final ResendEmailService resendEmailService;
    private final JavaMailSender mailSender;

    public EmailNotificationService() {
        this(new ResendEmailService(), null);
    }

    public EmailNotificationService(JavaMailSender mailSender) {
        this(new ResendEmailService(), mailSender);
    }

    @Autowired
    public EmailNotificationService(ResendEmailService resendEmailService) {
        this(resendEmailService, null);
    }

    public EmailNotificationService(ResendEmailService resendEmailService, JavaMailSender mailSender) {
        this.resendEmailService = resendEmailService != null ? resendEmailService : new ResendEmailService();
        this.mailSender = mailSender;
    }

    /**
     * Sends an email notification safely.
     * Sanitizes errors and returns dispatch result without breaking caller transactions.
     * Never leaks secrets or API keys.
     */
    public NotificationDispatchResult sendEmail(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            logger.warn("Cannot send email: recipient email address is missing");
            return NotificationDispatchResult.failure("Assigned officer email address is not configured.");
        }

        String targetEmail = toEmail.trim();

        // 1. Primary email dispatch via Resend Email Service
        if (resendEmailService != null && resendEmailService.isConfigured()) {
            return resendEmailService.sendEmail(targetEmail, subject, body);
        }

        // 2. If Resend is not configured and JavaMailSender is available, attempt SMTP fallback
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(targetEmail);
                message.setSubject(subject != null ? subject.trim() : "CrimeSphere AI — Case Alert");
                message.setText(body != null ? body : "");

                mailSender.send(message);

                String generatedMessageId = "msg-smtp-" + UUID.randomUUID().toString().substring(0, 8);
                logger.info("Email notification sent via SMTP fallback to recipient: {} (MessageId: {})", maskEmail(targetEmail), generatedMessageId);
                return NotificationDispatchResult.success(generatedMessageId);
            } catch (Exception e) {
                String sanitizedError = sanitizeErrorMessage(e.getMessage());
                logger.error("SMTP fallback email dispatch failed to recipient {}: {}", maskEmail(targetEmail), sanitizedError);
                return NotificationDispatchResult.failure("Email dispatch failure: " + sanitizedError);
            }
        }

        // 3. Neither Resend nor SMTP is configured
        if (resendEmailService != null) {
            return resendEmailService.sendEmail(targetEmail, subject, body);
        }

        logger.warn("No email provider configured. Email delivery skipped for recipient: {}", maskEmail(targetEmail));
        return NotificationDispatchResult.failure("Resend API key is not configured. Set RESEND_API_KEY environment variable.");
    }

    /**
     * Builds standardized CrimeSphere AI email content.
     */
    public String buildCaseAlertBody(String caseNumber, String caseTitle, String alertMessage, String actionRequired) {
        if (resendEmailService != null) {
            return resendEmailService.buildCaseAlertBody(caseNumber, caseTitle, alertMessage, actionRequired);
        }
        return "CrimeSphere AI Case Alert: " + caseNumber + " - " + caseTitle;
    }

    /**
     * Builds standardized AI Review Required email content.
     */
    public String buildAiReviewRequiredBody(String caseNumber, String caseTitle, String findingSummary, String verificationStatus) {
        if (resendEmailService != null) {
            return resendEmailService.buildAiReviewRequiredBody(caseNumber, caseTitle, findingSummary, verificationStatus,
                    "Please review the finding in CrimeSphere AI. AI recommendations are decision-support outputs and require investigator validation.");
        }
        return "CrimeSphere AI Review Required: " + caseNumber + " - " + caseTitle;
    }

    /**
     * Builds standardized Evidence Uploaded email content.
     */
    public String buildEvidenceUploadedBody(String caseNumber, String caseTitle, String evidenceNumber, String evidenceTitle, String uploadedBy, String actionRequired) {
        if (resendEmailService != null) {
            return resendEmailService.buildEvidenceUploadedBody(caseNumber, caseTitle, evidenceNumber, evidenceTitle, uploadedBy, actionRequired);
        }
        return "CrimeSphere AI Evidence Uploaded: " + caseNumber + " - " + evidenceNumber;
    }

    public ResendEmailService getResendEmailService() {
        return resendEmailService;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf('@');
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 2) return name + domain;
        return name.substring(0, 2) + "***" + domain;
    }

    private String sanitizeErrorMessage(String error) {
        if (error == null) return "Unknown error";
        return error.replaceAll("(?i)Bearer\\s+[A-Za-z0-9._-]+", "Bearer ***")
                .replaceAll("(?i)re_[A-Za-z0-9_-]+", "re_***")
                .replaceAll("(?i)password=[^,\\s]+", "password=***")
                .replaceAll("(?i)access_token=[^,\\s]+", "access_token=***")
                .replaceAll("(?i)secret=[^,\\s]+", "secret=***");
    }
}
