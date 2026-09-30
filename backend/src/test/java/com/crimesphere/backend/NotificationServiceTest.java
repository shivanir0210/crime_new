package com.crimesphere.backend;

import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.repository.*;
import com.crimesphere.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationAuditLogRepository auditLogRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private PoliceOfficerRepository officerRepository;

    @Mock
    private CaseOfficerRepository caseOfficerRepository;

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private JavaMailSender javaMailSender;

    private TestEmailNotificationService emailNotificationService;
    private TestWhatsAppNotificationService whatsAppNotificationService;

    private NotificationService notificationService;
    private NotificationRetryService retryService;
    private EmailService emailService;

    private PoliceStation station;
    private PoliceOfficer officer;
    private Case testCase;

    // Test doubles avoiding Java 24 concrete class inline-mocking issues
    private static class TestEmailNotificationService extends EmailNotificationService {
        private NotificationDispatchResult resultToReturn = NotificationDispatchResult.success("msg-ext-12345");
        private int sendCount = 0;

        public TestEmailNotificationService(JavaMailSender mailSender) {
            super(mailSender);
        }

        public void setResultToReturn(NotificationDispatchResult result) {
            this.resultToReturn = result;
        }

        public int getSendCount() {
            return sendCount;
        }

        @Override
        public NotificationDispatchResult sendEmail(String toEmail, String subject, String body) {
            sendCount++;
            return resultToReturn;
        }
    }

    private static class TestWhatsAppNotificationService extends WhatsAppNotificationService {
        private NotificationDispatchResult resultToReturn = NotificationDispatchResult.success("wamid.123456789");
        private int sendCount = 0;
        private boolean configured = true;

        public TestWhatsAppNotificationService() {
            super();
        }

        public void setConfigured(boolean configured) {
            this.configured = configured;
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        public void setResultToReturn(NotificationDispatchResult result) {
            this.resultToReturn = result;
        }

        public int getSendCount() {
            return sendCount;
        }

        @Override
        public NotificationDispatchResult sendWhatsApp(String toPhone, String caseNumber, String caseTitle, String alertMessage, String actionRequired) {
            sendCount++;
            return resultToReturn;
        }
    }

    @BeforeEach
    void setUp() {
        emailNotificationService = new TestEmailNotificationService(javaMailSender);
        whatsAppNotificationService = new TestWhatsAppNotificationService();

        notificationService = new NotificationService(
                notificationRepository,
                auditLogRepository,
                preferenceRepository,
                officerRepository,
                caseOfficerRepository,
                deliveryRepository,
                emailNotificationService,
                whatsAppNotificationService
        );

        retryService = new NotificationRetryService(
                notificationRepository,
                auditLogRepository,
                notificationService
        );

        emailService = new EmailService(
                javaMailSender,
                notificationRepository,
                auditLogRepository
        );

        station = new PoliceStation();
        station.setId(UUID.randomUUID());
        station.setStationCode("PS-COIMB-01");
        station.setStationName("Coimbatore Central PS");
        station.setHeadEmail("head.cbe@crimesphere.internal");

        officer = new PoliceOfficer();
        officer.setId(UUID.randomUUID());
        officer.setPoliceId("TN-1001");
        officer.setFullName("Insp. V. Rathore");
        officer.setEmail("v.rathore@police.tn.gov.in");
        officer.setPhone("+919876543210");
        officer.setStation(station);

        testCase = new Case();
        testCase.setId(UUID.randomUUID());
        testCase.setCaseNumber("CS-2026-0001");
        testCase.setTitle("Homicide Investigation");
        testCase.setStation(station);
        testCase.setCreatedBy(officer);
    }

    @Test
    @DisplayName("1. Email notification sent successfully updates status to SENT")
    void testEmailNotificationSuccess() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            if (n.getId() == null) n.setId(UUID.randomUUID());
            return n;
        });

        Notification result = notificationService.createAndSendNotification(
                testCase, officer, NotificationChannel.EMAIL, NotificationType.CASE_CREATED,
                "New Case Registered", "Case registered successfully",
                "Review dossier", officer.getEmail(), null
        );

        assertNotNull(result);
        assertEquals(NotificationStatus.SENT, result.getStatus());
        assertEquals("msg-ext-12345", result.getExternalMessageId());
        verify(auditLogRepository, atLeast(2)).save(any(NotificationAuditLog.class));
        assertEquals(1, emailNotificationService.getSendCount());
    }

    @Test
    @DisplayName("2. Email notification failure updates status to FAILED and records error")
    void testEmailNotificationFailure() {
        emailNotificationService.setResultToReturn(
                NotificationDispatchResult.failure("SMTP Server connection timed out"));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            if (n.getId() == null) n.setId(UUID.randomUUID());
            return n;
        });

        Notification result = notificationService.createAndSendNotification(
                testCase, officer, NotificationChannel.EMAIL, NotificationType.CASE_STATUS_CHANGED,
                "Status Alert", "Case status updated to UNDER_INVESTIGATION",
                "Review dossier", officer.getEmail(), null
        );

        assertNotNull(result);
        assertEquals(NotificationStatus.FAILED, result.getStatus());
        assertTrue(result.getLastError().contains("SMTP Server connection timed out"));
        verify(auditLogRepository, atLeast(2)).save(any(NotificationAuditLog.class));
    }

    @Test
    @DisplayName("3. WhatsApp notification sent successfully updates status to SENT")
    void testWhatsAppNotificationSuccess() {
        whatsAppNotificationService.setConfigured(true);
        whatsAppNotificationService.setResultToReturn(NotificationDispatchResult.success("wa-msg-999"));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            if (n.getId() == null) n.setId(UUID.randomUUID());
            return n;
        });

        Notification result = notificationService.createAndSendNotification(
                testCase, officer, NotificationChannel.WHATSAPP, NotificationType.CASE_PRIORITY_CHANGED,
                "Priority Alert", "Case priority escalated to CRITICAL",
                "Immediate review", null, officer.getPhone()
        );

        assertNotNull(result);
        assertEquals(NotificationStatus.SENT, result.getStatus());
        assertEquals("wa-msg-999", result.getExternalMessageId());
        verify(auditLogRepository, atLeast(2)).save(any(NotificationAuditLog.class));
        assertEquals(1, whatsAppNotificationService.getSendCount());
    }

    @Test
    @DisplayName("4. WhatsApp notification when unconfigured records SKIPPED without crashing")
    void testWhatsAppUnconfiguredHandling() {
        whatsAppNotificationService.setConfigured(false);
        org.springframework.test.util.ReflectionTestUtils.setField(whatsAppNotificationService, "mockEnabled", false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            if (n.getId() == null) n.setId(UUID.randomUUID());
            return n;
        });

        assertDoesNotThrow(() -> {
            Notification result = notificationService.createAndSendNotification(
                    testCase, officer, NotificationChannel.WHATSAPP, NotificationType.CASE_ALERT,
                    "Alert", "Test alert", "None", null, officer.getPhone()
            );
            assertEquals(NotificationStatus.SKIPPED, result.getStatus());
            assertTrue(result.getLastError().contains("WHATSAPP_NOT_CONFIGURED") || result.getLastError().contains("TEMPLATE_DISABLED") || result.getLastError().contains("not configured"));
        });
    }

    @Test
    @DisplayName("5. Notification preferences: opted-out event type is skipped")
    void testPreferencesOptOutEventSkipped() {
        NotificationPreference prefs = new NotificationPreference(officer);
        prefs.setCaseStatusEnabled(false); // Opt-out of case status updates
        lenient().when(preferenceRepository.findByPoliceOfficerId(officer.getId())).thenReturn(Optional.of(prefs));

        notificationService.dispatchCaseNotification(
                testCase,
                NotificationType.CASE_STATUS_CHANGED,
                "Status Changed",
                "Case status updated",
                "Review",
                officer
        );

        // Since case status is disabled in preferences, officer should receive no notification
        verify(notificationRepository, never()).save(argThat(n ->
                n != null && n.getRecipientOfficer() != null && n.getRecipientOfficer().getId().equals(officer.getId())
        ));
    }

    @Test
    @DisplayName("6. Mandatory LOGIN_ALERT ignores normal preferences and is always dispatched")
    void testMandatoryLoginAlertPolicy() {
        NotificationPreference prefs = new NotificationPreference(officer);
        prefs.setEmailEnabled(false); // Officer turned off email

        boolean isEnabled = notificationService.isNotificationTypeEnabled(prefs, NotificationType.LOGIN_ALERT);
        assertTrue(isEnabled, "LOGIN_ALERT must remain enabled regardless of preferences");
    }

    @Test
    @DisplayName("7. Duplicate notification prevention within 10-second idempotency window")
    void testDuplicatePrevention() {
        Notification recentNotification = new Notification();
        recentNotification.setId(UUID.randomUUID());
        recentNotification.setStatus(NotificationStatus.SENT);

        when(notificationRepository.findFirstByCaseEntityIdAndNotificationTypeAndCreatedAtAfterOrderByCreatedAtDesc(
                any(), any(), any()
        )).thenReturn(Optional.of(recentNotification));

        notificationService.dispatchCaseNotification(
                testCase,
                NotificationType.CASE_STATUS_CHANGED,
                "Status update",
                "Status changed",
                "Review",
                officer
        );

        // Verification: test doubles should not be invoked due to duplicate skip
        assertEquals(0, emailNotificationService.getSendCount());
        assertEquals(0, whatsAppNotificationService.getSendCount());
    }

    @Test
    @DisplayName("8. Retry mechanism: retries transient failed notifications up to max attempts")
    void testRetryMechanism() {
        Notification failedNotification = new Notification();
        failedNotification.setId(UUID.randomUUID());
        failedNotification.setCaseEntity(testCase);
        failedNotification.setRecipientOfficer(officer);
        failedNotification.setChannel(NotificationChannel.EMAIL);
        failedNotification.setRecipientEmail(officer.getEmail());
        failedNotification.setStatus(NotificationStatus.FAILED);
        failedNotification.setAttemptCount(1);
        failedNotification.setLastError("Connection timed out");

        emailNotificationService.setResultToReturn(NotificationDispatchResult.success("msg-retry-success"));

        when(notificationRepository.findByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(any(), anyInt()))
                .thenReturn(List.of(failedNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        retryService.retryFailedNotifications();

        assertEquals(NotificationStatus.SENT, failedNotification.getStatus());
        assertEquals("msg-retry-success", failedNotification.getExternalMessageId());
        verify(auditLogRepository, atLeastOnce()).save(any(NotificationAuditLog.class));
    }

    @Test
    @DisplayName("9. Phase 3 Login Alert email service records notification and audit trail without breaking auth")
    void testPhase3LoginAlertEmailServicePreserved() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
        doNothing().when(javaMailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> {
            emailService.sendStationHeadLoginNotification(officer, station);
        });

        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
        verify(notificationRepository, atLeast(1)).save(any(Notification.class));
        verify(auditLogRepository, atLeast(1)).save(any(NotificationAuditLog.class));
    }

    @Test
    @DisplayName("10. Development test notification endpoint dispatches correctly marked alert")
    void testDevTestNotification() {
        emailNotificationService.setResultToReturn(NotificationDispatchResult.success("msg-test-111"));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            if (n.getId() == null) n.setId(UUID.randomUUID());
            return n;
        });

        Notification result = notificationService.sendTestNotification(testCase, officer, NotificationChannel.EMAIL);

        assertNotNull(result);
        assertTrue(result.getSubject().contains("[DEV TEST]"));
        assertTrue(result.getMessage().contains("[DEVELOPMENT TEST NOTIFICATION]"));
        assertEquals(NotificationStatus.SENT, result.getStatus());
    }
}
