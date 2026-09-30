package com.crimesphere.backend;

import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.service.NotificationDispatchResult;
import com.crimesphere.backend.service.ResendEmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class ResendEmailServiceTest {

    private ResendEmailService resendEmailService;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;

    private static final String TEST_API_KEY = "re_test_key_1234567890abcdef";
    private static final String TEST_FROM_EMAIL = "onboarding@resend.dev";
    private static final String TEST_API_URL = "https://api.resend.com";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        resendEmailService = new ResendEmailService(TEST_API_KEY, TEST_FROM_EMAIL, TEST_API_URL, restClient, objectMapper);
    }

    @Test
    @DisplayName("1. Successful Resend email delivery returns success with message ID")
    void testSendEmailSuccess() {
        String toEmail = "investigator@police.gov.in";
        String subject = "Evidence Uploaded - AI Review Required";
        String body = "New forensic specimen uploaded for case CS-2026-0001.";

        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_API_KEY))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.from").value("onboarding@resend.dev"))
                .andExpect(jsonPath("$.to[0]").value(toEmail))
                .andExpect(jsonPath("$.subject").value(subject))
                .andRespond(withSuccess("{\"id\": \"49a3999c-0ce1-4ea6-ab68-afcd6dc2e794\"}", MediaType.APPLICATION_JSON));

        NotificationDispatchResult result = resendEmailService.sendEmail(toEmail, subject, body);

        mockServer.verify();
        assertNotNull(result);
        assertTrue(result.isSuccess(), "Dispatch should be successful");
        assertFalse(result.isMock(), "Dispatch should not be mock");
        assertEquals("49a3999c-0ce1-4ea6-ab68-afcd6dc2e794", result.getExternalMessageId());
        assertNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("2. Resend API returns HTTP 401 (Invalid API Key) - returns sanitized failure")
    void testSendEmailInvalidApiKeyError() {
        String toEmail = "investigator@police.gov.in";
        String subject = "Case Alert";
        String body = "Test Body";

        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withUnauthorizedRequest()
                        .body("{\"statusCode\": 401, \"message\": \"API key is invalid\", \"name\": \"invalid_api_key\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        NotificationDispatchResult result = resendEmailService.sendEmail(toEmail, subject, body);

        mockServer.verify();
        assertNotNull(result);
        assertFalse(result.isSuccess(), "Dispatch should fail on 401");
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("401"), "Error message should mention HTTP 401");
        assertTrue(result.getErrorMessage().contains("invalid_api_key") || result.getErrorMessage().contains("API key is invalid"));
        assertFalse(result.getErrorMessage().contains(TEST_API_KEY), "API key must NEVER be leaked in error messages");
    }

    @Test
    @DisplayName("3. Resend API returns HTTP 422 (Validation Error) - returns clear failure")
    void testSendEmailValidationError() {
        String toEmail = "investigator@police.gov.in";

        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withRawStatus(422)
                        .body("{\"statusCode\": 422, \"message\": \"The 'to' field must be a valid email address\", \"name\": \"validation_error\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        NotificationDispatchResult result = resendEmailService.sendEmail(toEmail, "Subject", "Body");

        mockServer.verify();
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("422"));
        assertTrue(result.getErrorMessage().contains("validation_error") || result.getErrorMessage().contains("valid email address"));
    }

    @Test
    @DisplayName("4. Missing officer email returns failure without calling Resend API")
    void testMissingOfficerEmailHandling() {
        NotificationDispatchResult resultNull = resendEmailService.sendEmail(null, "Subject", "Body");
        assertNotNull(resultNull);
        assertFalse(resultNull.isSuccess());
        assertEquals("Assigned officer email address is not configured.", resultNull.getErrorMessage());

        NotificationDispatchResult resultBlank = resendEmailService.sendEmail("   ", "Subject", "Body");
        assertNotNull(resultBlank);
        assertFalse(resultBlank.isSuccess());
        assertEquals("Assigned officer email address is not configured.", resultBlank.getErrorMessage());

        // Verify zero HTTP requests were made
        mockServer.verify();
    }

    @Test
    @DisplayName("5. Unconfigured API key returns failure without calling Resend API")
    void testUnconfiguredApiKeyHandling() {
        resendEmailService.setApiKey(null);
        assertFalse(resendEmailService.isConfigured());

        NotificationDispatchResult result = resendEmailService.sendEmail("officer@police.gov.in", "Subject", "Body");
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Resend API key is not configured"));

        // Placeholder key test
        resendEmailService.setApiKey("your_real_resend_api_key");
        assertFalse(resendEmailService.isConfigured());

        NotificationDispatchResult result2 = resendEmailService.sendEmail("officer@police.gov.in", "Subject", "Body");
        assertFalse(result2.isSuccess());
        assertTrue(result2.getErrorMessage().contains("Resend API key is not configured"));

        // Verify zero HTTP requests were made
        mockServer.verify();
    }

    @Test
    @DisplayName("6. Recipient email dynamically resolved from PoliceOfficer.getEmail()")
    void testDynamicRecipientFromPoliceOfficerEntity() {
        PoliceOfficer officer = new PoliceOfficer();
        officer.setId(UUID.randomUUID());
        officer.setPoliceId("TN-POL-1001");
        officer.setFullName("Development Officer One");
        officer.setEmail("dynamic.officer.1001@crimesphere.gov.in");

        assertEquals("dynamic.officer.1001@crimesphere.gov.in", officer.getEmail());

        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.to[0]").value("dynamic.officer.1001@crimesphere.gov.in"))
                .andRespond(withSuccess("{\"id\": \"re_dyn_1001\"}", MediaType.APPLICATION_JSON));

        NotificationDispatchResult result = resendEmailService.sendEmail(
                officer.getEmail(),
                "Evidence Uploaded - AI Review Required",
                "Specimen uploaded for analysis"
        );

        mockServer.verify();
        assertTrue(result.isSuccess());
        assertEquals("re_dyn_1001", result.getExternalMessageId());
    }

    @Test
    @DisplayName("7. Configurable RESEND_FROM_EMAIL sender is respected")
    void testConfigurableSender() {
        resendEmailService.setFromEmail("notifications@crimesphere.internal");

        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.from").value("notifications@crimesphere.internal"))
                .andRespond(withSuccess("{\"id\": \"re_sender_test\"}", MediaType.APPLICATION_JSON));

        NotificationDispatchResult result = resendEmailService.sendEmail("officer@police.gov.in", "Subject", "Body");
        mockServer.verify();
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("8. Structured Evidence Uploaded body and HTML formatting")
    void testEvidenceUploadedFormatting() {
        String body = resendEmailService.buildEvidenceUploadedBody(
                "CS-2026-0042",
                "Jewellery Heist Case",
                "EVD-2026-0099",
                "CCTV Surveillance Footage",
                "Insp. Rathore",
                "Review newly logged evidence in the case dossier."
        );

        assertNotNull(body);
        assertTrue(body.contains("CS-2026-0042"));
        assertTrue(body.contains("Jewellery Heist Case"));
        assertTrue(body.contains("EVD-2026-0099"));
        assertTrue(body.contains("CCTV Surveillance Footage"));
        assertTrue(body.contains("Insp. Rathore"));
        assertTrue(body.contains("Review newly logged evidence"));
    }

    @Test
    @DisplayName("9. Structured AI Review Required body formatting")
    void testAiReviewRequiredFormatting() {
        String body = resendEmailService.buildAiReviewRequiredBody(
                "CS-2026-0042",
                "Jewellery Heist Case",
                "High probability weapon match with ballistic archive.",
                "SUPPORTED",
                "Please review and validate the finding in CrimeSphere AI."
        );

        assertNotNull(body);
        assertTrue(body.contains("CS-2026-0042"));
        assertTrue(body.contains("Jewellery Heist Case"));
        assertTrue(body.contains("High probability weapon match"));
        assertTrue(body.contains("SUPPORTED"));
        assertTrue(body.contains("decision-support outputs"));
    }
}
