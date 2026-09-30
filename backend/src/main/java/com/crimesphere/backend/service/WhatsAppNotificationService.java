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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppNotificationService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${whatsapp.api-url:https://graph.facebook.com}")
    private String apiUrl;

    @Value("${whatsapp.access-token:}")
    private String accessToken;

    @Value("${whatsapp.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.business-account-id:}")
    private String businessAccountId;

    @Value("${whatsapp.api-version:v19.0}")
    private String apiVersion;

    @Value("${whatsapp.template-enabled:false}")
    private boolean templateEnabled;

    @Value("${whatsapp.template-name:}")
    private String templateName;

    @Value("${whatsapp.template-language:en}")
    private String templateLanguage;

    @Value("${whatsapp.mock-enabled:true}")
    private boolean mockEnabled = true;

    public WhatsAppNotificationService() {
        this.restClient = RestClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    /**
     * Checks if WhatsApp provider has minimum credentials configured.
     */
    public boolean isConfigured() {
        return accessToken != null && !accessToken.isBlank() &&
               phoneNumberId != null && !phoneNumberId.isBlank();
    }

    /**
     * Sends a WhatsApp alert safely using WhatsApp Cloud API or Mock mode.
     * Does NOT crash when unconfigured; never fakes real delivery success.
     */
    public NotificationDispatchResult sendWhatsApp(String toPhone, String caseNumber, String caseTitle, String alertMessage, String actionRequired) {
        if (toPhone == null || toPhone.trim().isEmpty()) {
            logger.warn("Cannot send WhatsApp: recipient phone number is missing");
            return NotificationDispatchResult.failure("Recipient phone number is missing");
        }

        if (mockEnabled) {
            String mockMessageId = "mock-wa-" + java.util.UUID.randomUUID().toString().substring(0, 12);
            logger.info("WhatsApp DEVELOPMENT MOCK mode active. Simulated dispatch to {} (MessageId: {}). DEVELOPMENT MOCK — NOT ACTUALLY SENT.",
                    maskPhone(toPhone), mockMessageId);
            return NotificationDispatchResult.mockSuccess(mockMessageId);
        }

        if (!isConfigured()) {
            logger.info("WhatsApp provider is not configured. Delivery skipped for recipient: {}", maskPhone(toPhone));
            return NotificationDispatchResult.failure("WhatsApp provider is not configured. Missing WHATSAPP_ACCESS_TOKEN or WHATSAPP_PHONE_NUMBER_ID.");
        }

        // Clean phone number (strip whitespace, hyphens, ensure numeric or + prefix)
        String cleanedPhone = toPhone.replaceAll("[^0-9+]", "");
        if (cleanedPhone.startsWith("+")) {
            cleanedPhone = cleanedPhone.substring(1);
        }

        try {
            String url = String.format("%s/%s/%s/messages", apiUrl.replaceAll("/+$", ""), apiVersion, phoneNumberId);

            Map<String, Object> payload = buildPayload(cleanedPhone, caseNumber, caseTitle, alertMessage, actionRequired);
            String requestBody = objectMapper.writeValueAsString(payload);

            String responseBody = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            String messageId = extractMessageId(responseBody);
            logger.info("WhatsApp alert dispatched successfully to: {} (MessageId: {})", maskPhone(cleanedPhone), messageId);
            return NotificationDispatchResult.success(messageId);

        } catch (Exception e) {
            String sanitizedError = sanitizeErrorMessage(e.getMessage());
            logger.warn("WhatsApp dispatch failed for recipient {}: {}", maskPhone(cleanedPhone), sanitizedError);
            return NotificationDispatchResult.failure("WhatsApp dispatch failure: " + sanitizedError);
        }
    }

    /**
     * Formats concise WhatsApp message adhering to data minimization.
     */
    public String buildTextMessage(String caseNumber, String caseTitle, String alertMessage, String actionRequired) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
        return String.format(
                "CrimeSphere AI — Case Alert\n\n" +
                "Case: %s\n" +
                "Title: %s\n\n" +
                "Alert:\n" +
                "%s\n\n" +
                "Action:\n" +
                "%s\n\n" +
                "Time:\n" +
                "%s",
                caseNumber != null ? caseNumber : "N/A",
                caseTitle != null ? caseTitle : "N/A",
                alertMessage != null ? alertMessage : "Case update",
                actionRequired != null ? actionRequired : "Review case in system",
                timestamp
        );
    }

    private Map<String, Object> buildPayload(String recipientPhone, String caseNumber, String caseTitle, String alertMessage, String actionRequired) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", recipientPhone);

        if (templateEnabled && templateName != null && !templateName.isBlank()) {
            payload.put("type", "template");
            Map<String, Object> templateMap = new HashMap<>();
            templateMap.put("name", templateName);
            Map<String, String> langMap = new HashMap<>();
            langMap.put("code", templateLanguage != null ? templateLanguage : "en");
            templateMap.put("language", langMap);
            payload.put("template", templateMap);
        } else {
            payload.put("type", "text");
            Map<String, Object> textMap = new HashMap<>();
            textMap.put("preview_url", false);
            textMap.put("body", buildTextMessage(caseNumber, caseTitle, alertMessage, actionRequired));
            payload.put("text", textMap);
        }

        return payload;
    }

    private String extractMessageId(String responseBody) {
        try {
            if (responseBody != null) {
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode messages = root.path("messages");
                if (messages.isArray() && messages.size() > 0) {
                    return messages.get(0).path("id").asText("msg-wa-" + System.currentTimeMillis());
                }
            }
        } catch (Exception ignored) {
        }
        return "msg-wa-" + System.currentTimeMillis();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, 2) + "****" + phone.substring(phone.length() - 2);
    }

    private String sanitizeErrorMessage(String error) {
        if (error == null) return "Unknown error";
        return error.replaceAll("Bearer\\s+[A-Za-z0-9._-]+", "Bearer ***")
                    .replaceAll("access_token=[^&\\s]+", "access_token=***");
    }
}
