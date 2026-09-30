package com.crimesphere.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for sending transactional emails via Resend HTTP API.
 * Securely reads RESEND_API_KEY and RESEND_FROM_EMAIL from environment.
 * Never logs or exposes API keys.
 */
@Service
public class ResendEmailService {

    private static final Logger logger = LoggerFactory.getLogger(ResendEmailService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${resend.api-key:${RESEND_API_KEY:}}")
    private String apiKey;

    @Value("${resend.from-email:${RESEND_FROM_EMAIL:onboarding@resend.dev}}")
    private String fromEmail = "onboarding@resend.dev";

    @Value("${resend.api-url:${RESEND_API_URL:https://api.resend.com}}")
    private String apiUrl = "https://api.resend.com";

    public ResendEmailService() {
        this(RestClient.builder().build(), new ObjectMapper());
    }

    public ResendEmailService(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient != null ? restClient : RestClient.builder().build();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public ResendEmailService(String apiKey, String fromEmail, String apiUrl, RestClient restClient, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail != null && !fromEmail.isBlank() ? fromEmail : "onboarding@resend.dev";
        this.apiUrl = apiUrl != null && !apiUrl.isBlank() ? apiUrl : "https://api.resend.com";
        this.restClient = restClient != null ? restClient : RestClient.builder().build();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !isPlaceholder(apiKey);
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    /**
     * Sends an email via Resend HTTP API with plain text body and auto-generated HTML.
     */
    public NotificationDispatchResult sendEmail(String toEmail, String subject, String body) {
        String htmlBody = generateHtmlFromBody(subject, body);
        return sendEmail(toEmail, subject, body, htmlBody);
    }

    /**
     * Sends an email via Resend HTTP API with custom text and HTML bodies.
     */
    public NotificationDispatchResult sendEmail(String toEmail, String subject, String textBody, String htmlBody) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            logger.warn("Cannot send email: recipient email address is missing");
            return NotificationDispatchResult.failure("Assigned officer email address is not configured.");
        }

        String targetEmail = toEmail.trim();

        if (!isConfigured()) {
            logger.warn("Resend API key is not configured. Email dispatch skipped for recipient: {}", maskEmail(targetEmail));
            return NotificationDispatchResult.failure("Resend API key is not configured. Set RESEND_API_KEY environment variable.");
        }

        String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail.trim() : "onboarding@resend.dev";
        String finalSubject = (subject != null && !subject.isBlank()) ? subject.trim() : "CrimeSphere AI — Notification";

        try {
            String endpoint = String.format("%s/emails", apiUrl.replaceAll("/+$", ""));

            Map<String, Object> payload = new HashMap<>();
            payload.put("from", sender);
            payload.put("to", Collections.singletonList(targetEmail));
            payload.put("subject", finalSubject);
            if (textBody != null && !textBody.isBlank()) {
                payload.put("text", textBody);
            }
            if (htmlBody != null && !htmlBody.isBlank()) {
                payload.put("html", htmlBody);
            }

            String requestJson = objectMapper.writeValueAsString(payload);

            String responseBody = restClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);

            String messageId = extractMessageId(responseBody);
            logger.info("Resend email notification sent successfully to recipient: {} (MessageId: {})",
                    maskEmail(targetEmail), messageId);
            return NotificationDispatchResult.success(messageId);

        } catch (RestClientResponseException e) {
            int statusCode = e.getStatusCode().value();
            String responseStr = e.getResponseBodyAsString();
            String parsedError = parseResendError(responseStr, statusCode);
            String sanitizedError = sanitizeErrorMessage(parsedError);

            logger.error("Resend email dispatch failed (HTTP {}) for recipient {}: {}",
                    statusCode, maskEmail(targetEmail), sanitizedError);
            return NotificationDispatchResult.failure("Resend dispatch failure (HTTP " + statusCode + "): " + sanitizedError);

        } catch (Exception e) {
            String rootCauseMsg = e.getCause() != null ? e.getCause().getMessage() : null;
            String rawMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            String fullErrorMsg = (rootCauseMsg != null && !rawMsg.contains(rootCauseMsg))
                    ? rawMsg + " (Cause: " + rootCauseMsg + ")"
                    : rawMsg;
            String sanitizedError = sanitizeErrorMessage(fullErrorMsg);

            logger.error("Resend email dispatch unexpected exception for recipient {}: {}",
                    maskEmail(targetEmail), sanitizedError);
            return NotificationDispatchResult.failure("Resend dispatch failure: " + sanitizedError);
        }
    }

    /**
     * Builds standardized plain-text case alert content.
     */
    public String buildCaseAlertBody(String caseNumber, String caseTitle, String alertMessage, String actionRequired) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
        return String.format(
                "CrimeSphere AI — Investigation System\n\n" +
                "Case Alert\n\n" +
                "Case Number: %s\n" +
                "Case Title: %s\n\n" +
                "Alert:\n" +
                "%s\n\n" +
                "Action Required:\n" +
                "%s\n\n" +
                "Timestamp: %s\n\n" +
                "Access the CrimeSphere AI command center to review this case.\n" +
                "This is an automated notification. Do not reply to this email.",
                caseNumber != null ? caseNumber : "N/A",
                caseTitle != null ? caseTitle : "N/A",
                alertMessage != null ? alertMessage : "Case status updated.",
                actionRequired != null ? actionRequired : "Please review the case dossier in CrimeSphere AI.",
                timestamp
        );
    }

    /**
     * Builds standardized plain-text Evidence Uploaded content.
     */
    public String buildEvidenceUploadedBody(String caseNumber, String caseTitle, String evidenceNumber,
                                            String evidenceTitle, String uploadedBy, String actionRequired) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
        return String.format(
                "CrimeSphere AI — Investigation System\n\n" +
                "Evidence Uploaded — AI Review Required\n\n" +
                "Case Number: %s\n" +
                "Case Title: %s\n\n" +
                "Evidence Number: %s\n" +
                "Evidence Title: %s\n" +
                "Uploaded By: %s\n" +
                "Timestamp: %s\n\n" +
                "Action Required:\n" +
                "%s\n\n" +
                "Access the CrimeSphere AI command center to review and validate evidence.\n" +
                "This is an automated notification.",
                caseNumber != null ? caseNumber : "N/A",
                caseTitle != null ? caseTitle : "N/A",
                evidenceNumber != null ? evidenceNumber : "N/A",
                evidenceTitle != null ? evidenceTitle : "New Evidence File",
                uploadedBy != null ? uploadedBy : "Investigating Officer",
                timestamp,
                actionRequired != null ? actionRequired : "Review newly logged evidence in the case dossier."
        );
    }

    /**
     * Builds standardized plain-text AI Review Required content.
     */
    public String buildAiReviewRequiredBody(String caseNumber, String caseTitle, String findingSummary,
                                            String verificationStatus, String actionRequired) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
        return String.format(
                "CrimeSphere AI — Investigation System\n\n" +
                "AI Review Required\n\n" +
                "Case Number: %s\n" +
                "Case Title: %s\n\n" +
                "Finding Summary:\n" +
                "%s\n\n" +
                "Verification Status:\n" +
                "%s\n\n" +
                "Action Required:\n" +
                "%s\n\n" +
                "Timestamp: %s\n\n" +
                "Please review the finding in CrimeSphere AI. AI recommendations are decision-support outputs and require investigator validation.\n" +
                "This is an automated notification.",
                caseNumber != null ? caseNumber : "N/A",
                caseTitle != null ? caseTitle : "N/A",
                findingSummary != null ? findingSummary : "New investigative observation requires review.",
                verificationStatus != null ? verificationStatus : "SUPPORTED / PARTIALLY_SUPPORTED",
                actionRequired != null ? actionRequired : "Review finding in CrimeSphere AI command center.",
                timestamp
        );
    }

    /**
     * Builds standardized plain-text Inter-Station Coordination content.
     */
    public String buildInterStationCoordinationBody(String requestingStation, String destinationStation,
                                                    String caseNumber, String victimName, String requestReason,
                                                    List<String> sharedItems, String actionRequired) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
        StringBuilder sharedListStr = new StringBuilder();
        if (sharedItems != null && !sharedItems.isEmpty()) {
            for (String item : sharedItems) {
                sharedListStr.append("- ").append(item).append("\n");
            }
        } else {
            sharedListStr.append("- Basic Coordination Notice\n");
        }

        return String.format(
                "CrimeSphere AI — Investigation System\n\n" +
                "Inter-Station Coordination Request\n\n" +
                "Requesting Station: %s\n" +
                "Destination Station: %s\n\n" +
                "Case: %s\n" +
                (victimName != null && !victimName.isBlank() ? "Victim: " + victimName + "\n" : "") +
                "\nReason:\n%s\n\n" +
                "Information Shared:\n%s\n" +
                "Action Required:\n%s\n\n" +
                "Timestamp: %s\n\n" +
                "Access the CrimeSphere AI command center to review, accept, or respond to this inter-station request.\n" +
                "This is an automated notification. Do not reply to this email.",
                requestingStation != null ? requestingStation : "N/A",
                destinationStation != null ? destinationStation : "N/A",
                caseNumber != null ? caseNumber : "N/A",
                requestReason != null ? requestReason : "Inter-station jurisdiction inquiry.",
                sharedListStr.toString().trim(),
                actionRequired != null ? actionRequired : "Please review and take action in CrimeSphere AI command center.",
                timestamp
        );
    }

    /**
     * Builds standardized plain-text Officer Registration Approval email for Station Head.
     */
    public String buildOfficerRegistrationAlertBody(String fullName, String policeId, String stationName,
                                                    String role, String ocrStatus, String reviewUrl) {
        return String.format(
                "CrimeSphere AI\n\n" +
                "A new officer has registered with your station.\n\n" +
                "Officer:\n%s\n\n" +
                "Police ID:\n%s\n\n" +
                "Station:\n%s\n\n" +
                "Role:\n%s\n\n" +
                "OCR Status:\n%s\n\n" +
                "Please log in to CrimeSphere AI to review this registration.\n" +
                (reviewUrl != null && !reviewUrl.isBlank() ? "\nReview Link:\n" + reviewUrl + "\n" : ""),
                fullName != null ? fullName : "N/A",
                policeId != null ? policeId : "N/A",
                stationName != null ? stationName : "N/A",
                role != null ? role : "OFFICER",
                ocrStatus != null ? ocrStatus : "REVIEW_REQUIRED"
        );
    }

    /**
     * Builds rich HTML Officer Registration Approval email for Station Head.
     */
    public String buildOfficerRegistrationAlertHtml(String fullName, String policeId, String stationName,
                                                    String role, String ocrStatus, String reviewUrl) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Officer", fullName != null ? fullName : "N/A");
        details.put("Police ID", policeId != null ? policeId : "N/A");
        details.put("Station", stationName != null ? stationName : "N/A");
        details.put("Role", role != null ? role : "OFFICER");
        details.put("OCR Status", ocrStatus != null ? ocrStatus : "REVIEW_REQUIRED");

        return buildHtmlEmail(
                "NEW OFFICER REGISTRATION REQUIRES APPROVAL",
                policeId != null ? policeId : "REG-PENDING",
                "New Officer Registration Approval Required",
                details,
                "Please log in to CrimeSphere AI to review this registration and verify ID credentials." +
                (reviewUrl != null && !reviewUrl.isBlank() ? " Review Link: " + reviewUrl : "")
        );
    }

    /**
     * Builds plain-text approval decision notification for the applicant.
     */
    public String buildOfficerRegistrationDecisionBody(String fullName, String policeId, boolean approved, String reason) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
        if (approved) {
            return String.format(
                    "CrimeSphere AI — Account Activation\n\n" +
                    "Hello %s,\n\n" +
                    "YOUR OFFICER ACCOUNT HAS BEEN APPROVED\n\n" +
                    "Your officer registration for Police ID %s has been approved by your Station Head.\n" +
                    "Your account is now ACTIVE and you can log in to CrimeSphere AI.\n\n" +
                    "Timestamp: %s\n\n" +
                    "Access portal: http://localhost:5173/login",
                    fullName != null ? fullName : "Officer",
                    policeId != null ? policeId : "",
                    timestamp
            );
        } else {
            return String.format(
                    "CrimeSphere AI — Account Status Update\n\n" +
                    "Hello %s,\n\n" +
                    "YOUR OFFICER REGISTRATION WAS REJECTED\n\n" +
                    "Your officer registration for Police ID %s was rejected by your Station Head.\n" +
                    (reason != null && !reason.isBlank() ? "Rejection Reason: " + reason + "\n\n" : "\n") +
                    "Please contact your police station administrator for further assistance.\n\n" +
                    "Timestamp: %s",
                    fullName != null ? fullName : "Officer",
                    policeId != null ? policeId : "",
                    timestamp
            );
        }
    }

    /**
     * Builds rich HTML Inter-Station Coordination email.
     */
    public String buildInterStationCoordinationHtml(String requestingStation, String destinationStation,
                                                    String caseNumber, String victimName, String requestReason,
                                                    List<String> sharedItems, String actionRequired) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Requesting Station", requestingStation != null ? requestingStation : "N/A");
        details.put("Destination Station", destinationStation != null ? destinationStation : "N/A");
        if (victimName != null && !victimName.isBlank()) {
            details.put("Victim Subject", victimName);
        }
        details.put("Request Reason", requestReason != null ? requestReason : "Inter-station inquiry");
        if (sharedItems != null && !sharedItems.isEmpty()) {
            details.put("Information Shared", String.join(", ", sharedItems));
        }

        return buildHtmlEmail(
                "INTER-STATION COORDINATION REQUEST",
                caseNumber,
                "Inter-Station Jurisdiction Inquiry",
                details,
                actionRequired != null ? actionRequired : "Please log into CrimeSphere AI to review, accept, or respond to this coordination request."
        );
    }

    /**
     * Generates a modern HTML notification email layout.
     */
    public String buildHtmlEmail(String headline, String caseNumber, String caseTitle,
                                 Map<String, String> details, String actionRequired) {
        StringBuilder detailRows = new StringBuilder();
        if (details != null) {
            for (Map.Entry<String, String> entry : details.entrySet()) {
                detailRows.append(String.format(
                        "<tr><td style=\"padding: 8px 12px; font-weight: 600; color: #475569; width: 35%%;\">%s</td>" +
                        "<td style=\"padding: 8px 12px; color: #1e293b;\">%s</td></tr>",
                        escapeHtml(entry.getKey()), escapeHtml(entry.getValue())
                ));
            }
        }

        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));

        return "<!DOCTYPE html>" +
                "<html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>" +
                "<body style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px;\">" +
                "<div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);\">" +
                "  <div style=\"background-color: #0f172a; padding: 20px 24px; color: #ffffff;\">" +
                "    <h2 style=\"margin: 0; font-size: 18px; font-weight: 700; letter-spacing: 0.5px;\">CRIMESPHERE AI</h2>" +
                "    <p style=\"margin: 4px 0 0 0; font-size: 13px; color: #94a3b8;\">Police Intelligence &amp; Investigation Platform</p>" +
                "  </div>" +
                "  <div style=\"padding: 24px;\">" +
                "    <div style=\"display: inline-block; background-color: #eff6ff; border-left: 4px solid #3b82f6; padding: 10px 14px; border-radius: 0 4px 4px 0; margin-bottom: 20px;\">" +
                "      <span style=\"font-weight: 700; color: #1e40af; font-size: 15px;\">" + escapeHtml(headline) + "</span>" +
                "    </div>" +
                "    <table style=\"width: 100%; border-collapse: collapse; margin-bottom: 20px; font-size: 14px; background-color: #f8fafc; border-radius: 6px;\">" +
                "      <tr><td style=\"padding: 8px 12px; font-weight: 600; color: #475569; width: 35%;\">Case Number</td><td style=\"padding: 8px 12px; color: #0f172a; font-weight: 600;\">" + escapeHtml(caseNumber != null ? caseNumber : "N/A") + "</td></tr>" +
                "      <tr><td style=\"padding: 8px 12px; font-weight: 600; color: #475569;\">Case Title</td><td style=\"padding: 8px 12px; color: #1e293b;\">" + escapeHtml(caseTitle != null ? caseTitle : "N/A") + "</td></tr>" +
                detailRows +
                "    </table>" +
                (actionRequired != null && !actionRequired.isBlank() ?
                "    <div style=\"background-color: #fefce8; border: 1px solid #fef08a; padding: 14px; border-radius: 6px; margin-bottom: 20px;\">" +
                "      <strong style=\"color: #854d0e; font-size: 13px; display: block; margin-bottom: 4px;\">ACTION REQUIRED</strong>" +
                "      <p style=\"margin: 0; color: #713f12; font-size: 14px;\">" + escapeHtml(actionRequired) + "</p>" +
                "    </div>" : "") +
                "    <p style=\"font-size: 13px; color: #64748b; margin: 20px 0 0 0; border-top: 1px solid #e2e8f0; padding-top: 14px;\">" +
                "      Dispatched: " + escapeHtml(timestamp) + "<br/>" +
                "      Please open the CrimeSphere AI command center to review details." +
                "    </p>" +
                "  </div>" +
                "</div></body></html>";
    }

    private String generateHtmlFromBody(String subject, String body) {
        if (body == null) body = "";
        String escapedBody = escapeHtml(body).replace("\n", "<br/>");
        return "<!DOCTYPE html><html><body style=\"font-family: sans-serif; padding: 20px; color: #1e293b;\">" +
                "<h3 style=\"color: #0f172a;\">" + escapeHtml(subject != null ? subject : "CrimeSphere AI Notification") + "</h3>" +
                "<p style=\"line-height: 1.6;\">" + escapedBody + "</p>" +
                "</body></html>";
    }

    private String extractMessageId(String responseBody) {
        try {
            if (responseBody != null && !responseBody.isBlank()) {
                JsonNode root = objectMapper.readTree(responseBody);
                if (root.has("id") && !root.get("id").isNull()) {
                    return root.get("id").asText();
                }
            }
        } catch (Exception ignored) {
        }
        return "msg-resend-" + UUID.randomUUID().toString().substring(0, 12);
    }
    /**
     * Builds standardized plain-text Login OTP verification email content.
     */
    public String buildLoginOtpEmailBody(String otp) {
        return String.format(
                "CrimeSphere AI — Identity & Access Verification\n\n" +
                "Your CrimeSphere AI verification code is:\n\n" +
                "%s\n\n" +
                "This code expires in 5 minutes.\n\n" +
                "If you did not attempt to log in, please contact your station administrator immediately.\n" +
                "Do not share this verification code with anyone. This is an automated security transmission.",
                otp != null ? otp : "------"
        );
    }

    /**
     * Builds standardized HTML Login OTP verification email content with forensic styling.
     */
    public String buildLoginOtpEmailHtml(String otp) {
        String safeOtp = escapeHtml(otp != null ? otp : "------");
        return String.format(
                "<!DOCTYPE html>" +
                "<html><head><meta charset='utf-8'>" +
                "<style>" +
                "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #0b0b0c; color: #d0d0d5; margin: 0; padding: 20px; }" +
                ".container { max-width: 540px; margin: 0 auto; background-color: #121214; border: 1px solid #2a2a2d; border-radius: 8px; overflow: hidden; box-shadow: 0 8px 24px rgba(0,0,0,0.6); }" +
                ".header { background: linear-gradient(135deg, #1c0003 0%%, #2d0006 100%%); border-bottom: 2px solid #c1121f; padding: 24px 28px; }" +
                ".header h1 { margin: 0; font-size: 20px; letter-spacing: 2px; color: #ffffff; font-weight: 800; }" +
                ".header h1 span { color: #c1121f; }" +
                ".header .badge { font-size: 11px; letter-spacing: 1.5px; color: #8a8a93; margin-top: 4px; text-transform: uppercase; font-weight: 600; }" +
                ".content { padding: 28px; }" +
                ".title { font-size: 16px; font-weight: 700; color: #ffffff; margin-bottom: 12px; }" +
                ".info-text { font-size: 14px; color: #a0a0aa; line-height: 1.5; margin-bottom: 20px; }" +
                ".otp-box { background-color: #0a0a0c; border: 2px dashed #c1121f; border-radius: 6px; padding: 18px; text-align: center; margin: 24px 0; }" +
                ".otp-code { font-family: 'Courier New', monospace; font-size: 32px; font-weight: 800; letter-spacing: 10px; color: #ff4d5a; margin: 0; }" +
                ".expiry-warning { font-size: 13px; color: #e63946; font-weight: 600; margin-top: 8px; }" +
                ".warning-box { background-color: rgba(193, 18, 31, 0.08); border-left: 3px solid #c1121f; padding: 12px 16px; border-radius: 4px; margin-top: 24px; font-size: 12px; color: #d0d0d5; line-height: 1.5; }" +
                ".footer { background-color: #0d0d0f; border-top: 1px solid #1c1c1f; padding: 16px 28px; font-size: 11px; color: #60606a; text-align: center; }" +
                "</style></head><body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>CRIMESPHERE <span>AI</span></h1>" +
                "<div class='badge'>Identity & Access Security • Multi-Factor Authentication</div>" +
                "</div>" +
                "<div class='content'>" +
                "<div class='title'>Login Verification Code</div>" +
                "<div class='info-text'>Your CrimeSphere AI verification code is:</div>" +
                "<div class='otp-box'>" +
                "<div class='otp-code'>%s</div>" +
                "<div class='expiry-warning'>Expires in 5 minutes</div>" +
                "</div>" +
                "<div class='warning-box'>" +
                "<strong>Security Notice:</strong> If you did not attempt to log in, please contact your station administrator immediately. Never share your verification code with anyone." +
                "</div>" +
                "</div>" +
                "<div class='footer'>Department of Police Intelligence • Automated Security Transmission</div>" +
                "</div></body></html>",
                safeOtp
        );
    }

    private String parseResendError(String responseBody, int statusCode) {
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(responseBody);
                String message = root.path("message").asText(null);
                String name = root.path("name").asText(null);
                if (message != null && !message.isBlank()) {
                    return (name != null ? "[" + name + "] " : "") + message;
                }
                if (root.has("error")) {
                    return root.path("error").asText();
                }
            } catch (Exception ignored) {
            }
            return responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
        }
        return switch (statusCode) {
            case 401 -> "Invalid API key or unauthorized";
            case 403 -> "Forbidden or unverified sender domain";
            case 422 -> "Validation error or invalid payload";
            case 429 -> "Rate limit exceeded";
            default -> "HTTP " + statusCode + " error";
        };
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
                    .replaceAll("(?i)api[_-]?key=[^&\\s]+", "api_key=***")
                    .replaceAll("(?i)password=[^,\\s]+", "password=***")
                    .replaceAll("(?i)secret=[^,\\s]+", "secret=***");
    }

    private boolean isPlaceholder(String val) {
        if (val == null) return true;
        String trimmed = val.trim().toLowerCase();
        return trimmed.isEmpty() ||
                trimmed.contains("replace") ||
                trimmed.contains("placeholder") ||
                trimmed.contains("change_me") ||
                trimmed.contains("your_resend_api_key") ||
                trimmed.contains("your_real_resend_api_key") ||
                trimmed.equals("xxx");
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
