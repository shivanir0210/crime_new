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
import org.springframework.mail.javamail.JavaMailSender;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Phase13CollaborationTest {

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

    private NotificationService notificationService;

    private PoliceStation station;
    private PoliceOfficer officer1;
    private PoliceOfficer officer2;
    private Case testCase;

    @BeforeEach
    void setUp() {
        EmailNotificationService emailService = new EmailNotificationService(javaMailSender);
        WhatsAppNotificationService whatsAppService = new WhatsAppNotificationService();

        notificationService = new NotificationService(
                notificationRepository,
                auditLogRepository,
                preferenceRepository,
                officerRepository,
                caseOfficerRepository,
                deliveryRepository,
                emailService,
                whatsAppService
        );

        station = new PoliceStation();
        station.setId(UUID.randomUUID());
        station.setStationCode("PS-COIMB-01");
        station.setStationName("Coimbatore Central PS");

        officer1 = new PoliceOfficer();
        officer1.setId(UUID.randomUUID());
        officer1.setPoliceId("TN-1001");
        officer1.setFullName("Insp. V. Rathore");
        officer1.setEmail("rathore@police.gov.in");

        officer2 = new PoliceOfficer();
        officer2.setId(UUID.randomUUID());
        officer2.setPoliceId("TN-1002");
        officer2.setFullName("Sub-Insp. K. Sharma");
        officer2.setEmail("sharma@police.gov.in");

        testCase = new Case();
        testCase.setId(UUID.randomUUID());
        testCase.setCaseNumber("CS-2026-0001");
        testCase.setTitle("Homicide Investigation");
        testCase.setStation(station);
        testCase.setCreatedBy(officer1);
    }

    @Test
    @DisplayName("Recipient Exclusion Rule: Acting officer must NOT receive their own notification alert")
    void testRecipientExclusionRule() {
        CaseOfficer co1 = new CaseOfficer(testCase, officer1, officer1);
        CaseOfficer co2 = new CaseOfficer(testCase, officer2, officer1);

        when(caseOfficerRepository.findByCaseEntityIdAndIsActiveTrue(testCase.getId()))
                .thenReturn(List.of(co1, co2));

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            if (n.getId() == null) n.setId(UUID.randomUUID());
            return n;
        });

        // Trigger action done by officer1 (acting officer)
        notificationService.dispatchCaseNotification(
                testCase,
                NotificationType.EVIDENCE_ADDED,
                "🔎 NEW CLUE ADDED",
                "Officer 1 added evidence",
                "Review evidence log",
                officer1
        );

        // Verification: Notification saved must NOT have officer1 as recipient
        verify(notificationRepository, atLeastOnce()).save(argThat(n -> {
            if (n.getRecipientOfficer() != null) {
                assertNotEquals(officer1.getId(), n.getRecipientOfficer().getId(), "Acting officer should be excluded!");
            }
            return true;
        }));
    }

    @Test
    @DisplayName("WhatsApp unconfigured or template disabled must record SKIPPED delivery status")
    void testWhatsAppUnconfiguredSkippedStatus() {
        WhatsAppNotificationService whatsAppService = new WhatsAppNotificationService();
        org.springframework.test.util.ReflectionTestUtils.setField(whatsAppService, "mockEnabled", false);
        notificationService = new NotificationService(
                notificationRepository,
                auditLogRepository,
                preferenceRepository,
                officerRepository,
                caseOfficerRepository,
                deliveryRepository,
                new EmailNotificationService(javaMailSender),
                whatsAppService
        );

        NotificationPreference pref = new NotificationPreference();
        pref.setWhatsappEnabled(true);
        lenient().when(preferenceRepository.findByPoliceOfficerId(officer2.getId())).thenReturn(Optional.of(pref));

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            if (n.getId() == null) n.setId(UUID.randomUUID());
            return n;
        });

        Notification notif = notificationService.createAndSendNotification(
                testCase,
                officer2,
                NotificationChannel.WHATSAPP,
                NotificationType.CASE_ALERT,
                "High Severity Case Alert",
                "Critical clue uploaded",
                "Check system dossier",
                officer2.getEmail(),
                officer2.getPhone()
        );

        assertNotNull(notif);
        assertEquals(NotificationStatus.SKIPPED, notif.getStatus());
        assertTrue(notif.getLastError().contains("WHATSAPP_NOT_CONFIGURED") || notif.getLastError().contains("TEMPLATE_DISABLED"));
    }

    @Test
    @DisplayName("Unread count for officer should filter read status")
    void testUnreadCount() {
        when(notificationRepository.countByRecipientOfficerIdAndIsReadFalse(officer1.getId())).thenReturn(5L);
        long unread = notificationService.getUnreadCount(officer1);
        assertEquals(5L, unread);
    }
}
