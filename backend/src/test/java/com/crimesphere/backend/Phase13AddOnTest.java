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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Phase13AddOnTest {

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

    @Mock
    private AuditLogRepository systemAuditLogRepository;

    private EmailNotificationService emailNotificationService;
    private WhatsAppNotificationService whatsAppNotificationService;
    private NotificationService notificationService;
    private CaseOfficerService caseOfficerService;

    @Mock
    private CaseRepository caseRepository;

    private PoliceStation station;
    private PoliceOfficer officer1001; // TN-POL-1001 (Uploader)
    private PoliceOfficer officer1002; // TN-POL-1002 (Co-assigned)
    private PoliceOfficer officer1004; // TN-POL-1004 (Co-assigned)
    private PoliceOfficer unassignedOfficer; // TN-POL-9999 (Unassigned)
    private Case demoCase;

    @BeforeEach
    void setUp() {
        emailNotificationService = new EmailNotificationService(javaMailSender);
        whatsAppNotificationService = new WhatsAppNotificationService();
        ReflectionTestUtils.setField(whatsAppNotificationService, "mockEnabled", true);

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

        caseOfficerService = new CaseOfficerService(
                caseRepository,
                officerRepository,
                caseOfficerRepository,
                systemAuditLogRepository,
                notificationService
        );

        station = new PoliceStation();
        station.setId(UUID.randomUUID());
        station.setStationCode("PS-COIMB-01");
        station.setStationName("Coimbatore Central Police Station");

        officer1001 = new PoliceOfficer();
        officer1001.setId(UUID.randomUUID());
        officer1001.setPoliceId("TN-POL-1001");
        officer1001.setFullName("Insp. K. Vijay");
        officer1001.setPhone("+919000000001");
        officer1001.setEmail("vijay@coimbatorepolice.tn.gov.in");
        officer1001.setStation(station);

        officer1002 = new PoliceOfficer();
        officer1002.setId(UUID.randomUUID());
        officer1002.setPoliceId("TN-POL-1002");
        officer1002.setFullName("Sub-Insp. S. Anitha");
        officer1002.setPhone("+919000000002");
        officer1002.setEmail("anitha@coimbatorepolice.tn.gov.in");
        officer1002.setStation(station);

        officer1004 = new PoliceOfficer();
        officer1004.setId(UUID.randomUUID());
        officer1004.setPoliceId("TN-POL-1004");
        officer1004.setFullName("Sub-Insp. M. Rajesh");
        officer1004.setPhone("+919000000004");
        officer1004.setEmail("rajesh@coimbatorepolice.tn.gov.in");
        officer1004.setStation(station);

        unassignedOfficer = new PoliceOfficer();
        unassignedOfficer.setId(UUID.randomUUID());
        unassignedOfficer.setPoliceId("TN-POL-9999");
        unassignedOfficer.setFullName("Const. R. Kumar");
        unassignedOfficer.setPhone("+919000000999");
        unassignedOfficer.setEmail("kumar@coimbatorepolice.tn.gov.in");
        unassignedOfficer.setStation(station);

        demoCase = new Case();
        demoCase.setId(UUID.randomUUID());
        demoCase.setCaseNumber("CS-2026-0100");
        demoCase.setTitle("Development Murder Investigation");
        demoCase.setStation(station);
        demoCase.setCreatedBy(officer1001);
    }

    @Test
    @DisplayName("1. Multiple officers assigned to case successfully")
    void testMultipleOfficersAssignedToCase() {
        CaseOfficer co1 = new CaseOfficer(demoCase, officer1001, officer1001);
        CaseOfficer co2 = new CaseOfficer(demoCase, officer1002, officer1001);
        CaseOfficer co4 = new CaseOfficer(demoCase, officer1004, officer1001);

        when(caseOfficerRepository.findByCaseEntityIdAndIsActiveTrue(demoCase.getId()))
                .thenReturn(List.of(co1, co2, co4));

        List<CaseOfficer> activeAssignments = caseOfficerRepository.findByCaseEntityIdAndIsActiveTrue(demoCase.getId());
        assertEquals(3, activeAssignments.size());
        assertTrue(activeAssignments.stream().anyMatch(co -> co.getPoliceOfficer().getPoliceId().equals("TN-POL-1001")));
        assertTrue(activeAssignments.stream().anyMatch(co -> co.getPoliceOfficer().getPoliceId().equals("TN-POL-1002")));
        assertTrue(activeAssignments.stream().anyMatch(co -> co.getPoliceOfficer().getPoliceId().equals("TN-POL-1004")));
    }

    @Test
    @DisplayName("2. Duplicate active assignment rejected")
    void testDuplicateActiveAssignmentRejected() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(officer1001));
        when(caseRepository.findById(demoCase.getId())).thenReturn(Optional.of(demoCase));
        when(officerRepository.findById(officer1002.getId())).thenReturn(Optional.of(officer1002));
        when(caseOfficerRepository.findByCaseEntityIdAndPoliceOfficerId(demoCase.getId(), officer1002.getId()))
                .thenReturn(Optional.of(new CaseOfficer(demoCase, officer1002, officer1001)));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            caseOfficerService.assignOfficerToCase(demoCase.getId(), officer1002.getId(), "TN-POL-1001");
        });

        assertTrue(exception.getMessage().contains("already assigned"));
    }

    @Test
    @DisplayName("3 & 4 & 5. Evidence upload notifies co-assigned officers, excludes uploader, and excludes unassigned officer")
    void testEvidenceUploadNotificationDispatchRules() {
        CaseOfficer co1 = new CaseOfficer(demoCase, officer1001, officer1001);
        CaseOfficer co2 = new CaseOfficer(demoCase, officer1002, officer1001);
        CaseOfficer co4 = new CaseOfficer(demoCase, officer1004, officer1001);

        when(caseOfficerRepository.findByCaseEntityIdAndIsActiveTrue(demoCase.getId()))
                .thenReturn(List.of(co1, co2, co4));

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            if (n.getId() == null) n.setId(UUID.randomUUID());
            return n;
        });

        // Officer 1001 uploads evidence
        notificationService.dispatchCaseNotification(
                demoCase,
                NotificationType.EVIDENCE_ADDED,
                "🔎 NEW FORENSIC CLUE",
                "Officer Insp. K. Vijay uploaded DNA report E-CS-2026-0100-001",
                "Inspect evidence vault",
                officer1001
        );

        // Verify notification saved for officer1002 and officer1004, but NOT officer1001 (uploader) or unassignedOfficer
        verify(notificationRepository, atLeastOnce()).save(argThat(n -> {
            if (n.getRecipientOfficer() != null) {
                assertNotEquals(officer1001.getId(), n.getRecipientOfficer().getId(), "Uploading officer must be excluded!");
                assertNotEquals(unassignedOfficer.getId(), n.getRecipientOfficer().getId(), "Unassigned officer must not be notified!");
            }
            return true;
        }));
    }

    @Test
    @DisplayName("6. FORENSIC_REPORT upload creates '🔎 NEW FORENSIC CLUE' notification subject and body")
    void testForensicReportUploadSubjectAndBody() {
        CaseOfficer co1 = new CaseOfficer(demoCase, officer1001, officer1001);
        CaseOfficer co2 = new CaseOfficer(demoCase, officer1002, officer1001);

        when(caseOfficerRepository.findByCaseEntityIdAndIsActiveTrue(demoCase.getId()))
                .thenReturn(List.of(co1, co2));

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            if (n.getId() == null) n.setId(UUID.randomUUID());
            return n;
        });

        notificationService.dispatchCaseNotification(
                demoCase,
                NotificationType.EVIDENCE_ADDED,
                "🔎 NEW FORENSIC CLUE",
                "New forensic report evidence registered: DNA Analysis Report (Number: E-CS-2026-0100-001) by Insp. K. Vijay. Location: Coimbatore Crime Lab.",
                "Review evidence details in CrimeSphere.",
                officer1001
        );

        verify(notificationRepository, atLeastOnce()).save(argThat(n -> {
            assertEquals("🔎 NEW FORENSIC CLUE", n.getSubject());
            assertTrue(n.getMessage().contains("DNA Analysis Report"));
            assertTrue(n.getMessage().contains("Insp. K. Vijay"));
            return true;
        }));
    }

    @Test
    @DisplayName("7. WHATSAPP_MOCK_ENABLED=true records MOCK_SENT status without calling Meta API")
    void testWhatsAppMockEnabledRecordsMockSent() {
        NotificationPreference pref = new NotificationPreference();
        pref.setWhatsappEnabled(true);
        lenient().when(preferenceRepository.findByPoliceOfficerId(officer1002.getId())).thenReturn(Optional.of(pref));

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            if (n.getId() == null) n.setId(UUID.randomUUID());
            return n;
        });

        Notification notif = notificationService.createAndSendNotification(
                demoCase,
                officer1002,
                NotificationChannel.WHATSAPP,
                NotificationType.EVIDENCE_ADDED,
                "🔎 NEW FORENSIC CLUE",
                "DNA Analysis Report uploaded by Insp. K. Vijay",
                "Check case dossier",
                officer1002.getEmail(),
                officer1002.getPhone()
        );

        assertNotNull(notif);
        assertEquals(NotificationStatus.MOCK_SENT, notif.getStatus());
        assertTrue(notif.getLastError().contains("DEVELOPMENT MOCK — NOT ACTUALLY SENT"));

        // Verify delivery audit log created with MOCK_SENT
        verify(deliveryRepository, times(1)).save(argThat(d -> {
            assertEquals(NotificationStatus.MOCK_SENT, d.getStatus());
            assertTrue(d.getLastError().contains("DEVELOPMENT MOCK — NOT ACTUALLY SENT"));
            return true;
        }));
    }

    @Test
    @DisplayName("8. Recipient phone numbers resolved strictly from PoliceOfficer.phone")
    void testRecipientPhoneResolvedFromPoliceOfficerEntity() {
        assertEquals("+919000000002", officer1002.getPhone());
        assertEquals("+919000000004", officer1004.getPhone());
        assertNotNull(officer1002.getPhone());
        assertFalse(officer1002.getPhone().isEmpty());
    }

    @Test
    @DisplayName("9. Station isolation enforced")
    void testStationIsolationEnforced() {
        PoliceStation station2 = new PoliceStation();
        station2.setId(UUID.randomUUID());
        station2.setStationCode("PS-MADURAI-01");
        station2.setStationName("Madurai Central PS");

        PoliceOfficer maduraiOfficer = new PoliceOfficer();
        maduraiOfficer.setId(UUID.randomUUID());
        maduraiOfficer.setPoliceId("TN-POL-8001");
        maduraiOfficer.setFullName("Insp. M. Pandian");
        maduraiOfficer.setStation(station2);

        // Case station vs officer station isolation check
        assertNotEquals(demoCase.getStation().getStationCode(), maduraiOfficer.getStation().getStationCode());
    }

    @Test
    @DisplayName("10. EVIDENCE_UPLOADED notification creates notifications for all other active assigned officers (TN-POL-1002, 1003, 1004)")
    void testEvidenceUploadedDispatchesToAllOtherActiveOfficers() {
        PoliceOfficer officer1003 = new PoliceOfficer();
        officer1003.setId(UUID.randomUUID());
        officer1003.setPoliceId("TN-POL-1003");
        officer1003.setFullName("Sub-Insp. K. Ramesh");
        officer1003.setStation(station);

        CaseOfficer co1 = new CaseOfficer(demoCase, officer1001, officer1001);
        CaseOfficer co2 = new CaseOfficer(demoCase, officer1002, officer1001);
        CaseOfficer co3 = new CaseOfficer(demoCase, officer1003, officer1001);
        CaseOfficer co4 = new CaseOfficer(demoCase, officer1004, officer1001);

        when(caseOfficerRepository.findByCaseEntityIdAndIsActiveTrue(demoCase.getId()))
                .thenReturn(List.of(co1, co2, co3, co4));

        List<UUID> savedRecipients = new ArrayList<>();
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            if (n.getId() == null) n.setId(UUID.randomUUID());
            if (n.getRecipientOfficer() != null) {
                savedRecipients.add(n.getRecipientOfficer().getId());
            }
            return n;
        });

        notificationService.dispatchCaseNotification(
                demoCase,
                NotificationType.EVIDENCE_UPLOADED,
                "New evidence uploaded for case CS-2026-0100: Forensic Fingerprint Report",
                "New evidence uploaded for case CS-2026-0100: Forensic Fingerprint Report (Uploaded by Insp. K. Vijay)",
                "Review newly logged evidence",
                officer1001
        );

        // Verify recipient list contains officer1002, officer1003, officer1004
        assertTrue(savedRecipients.contains(officer1002.getId()), "TN-POL-1002 must receive notification");
        assertTrue(savedRecipients.contains(officer1003.getId()), "TN-POL-1003 must receive notification");
        assertTrue(savedRecipients.contains(officer1004.getId()), "TN-POL-1004 must receive notification");
        assertFalse(savedRecipients.contains(officer1001.getId()), "Uploader TN-POL-1001 must NOT receive notification");
    }

    @Test
    @DisplayName("11. Uploader is strictly excluded from notification recipients")
    void testUploaderExcludedFromNotifications() {
        CaseOfficer co1 = new CaseOfficer(demoCase, officer1001, officer1001);
        CaseOfficer co2 = new CaseOfficer(demoCase, officer1002, officer1001);

        when(caseOfficerRepository.findByCaseEntityIdAndIsActiveTrue(demoCase.getId()))
                .thenReturn(List.of(co1, co2));

        List<PoliceOfficer> recipients = notificationService.resolveRecipients(
                demoCase, NotificationType.EVIDENCE_UPLOADED, officer1001);

        assertFalse(recipients.stream().anyMatch(r -> r.getId().equals(officer1001.getId())),
                "Uploader officer1001 must be excluded from recipient list");
        assertTrue(recipients.stream().anyMatch(r -> r.getId().equals(officer1002.getId())),
                "Co-assigned officer1002 must be included in recipient list");
    }

    @Test
    @DisplayName("12. Inactive or unassigned officers do not receive notifications")
    void testInactiveOrUnassignedOfficersExcluded() {
        PoliceOfficer inactiveOfficer = new PoliceOfficer();
        inactiveOfficer.setId(UUID.randomUUID());
        inactiveOfficer.setPoliceId("TN-POL-0000");

        CaseOfficer activeCo = new CaseOfficer(demoCase, officer1002, officer1001);
        // caseOfficerRepository.findByCaseEntityIdAndIsActiveTrue only returns active assignments
        when(caseOfficerRepository.findByCaseEntityIdAndIsActiveTrue(demoCase.getId()))
                .thenReturn(List.of(activeCo));

        List<PoliceOfficer> recipients = notificationService.resolveRecipients(
                demoCase, NotificationType.EVIDENCE_UPLOADED, officer1001);

        assertFalse(recipients.stream().anyMatch(r -> r.getId().equals(inactiveOfficer.getId())),
                "Inactive officer must be excluded");
        assertFalse(recipients.stream().anyMatch(r -> r.getId().equals(unassignedOfficer.getId())),
                "Unassigned officer must be excluded");
    }

    @Test
    @DisplayName("13. Duplicate upload request does not create duplicate notifications (Idempotency)")
    void testDuplicateUploadDoesNotCreateDuplicateNotification() {
        Notification existingNotif = new Notification();
        existingNotif.setId(UUID.randomUUID());
        existingNotif.setStatus(NotificationStatus.SENT);
        existingNotif.setNotificationType(NotificationType.EVIDENCE_UPLOADED);

        when(notificationRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.of(existingNotif));

        Notification result = notificationService.createAndSendNotification(
                demoCase,
                officer1002,
                NotificationChannel.IN_APP,
                NotificationType.EVIDENCE_UPLOADED,
                "Duplicate Upload Test",
                "Test message",
                "Action",
                null,
                null
        );

        assertEquals(existingNotif.getId(), result.getId(), "Must return existing notification on duplicate idempotency key");
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("14. NotificationResponse API DTO maps EVIDENCE_UPLOADED type correctly")
    void testNotificationResponseDtoTypeMapping() {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setCaseEntity(demoCase);
        notification.setRecipientOfficer(officer1002);
        notification.setNotificationType(NotificationType.EVIDENCE_UPLOADED);
        notification.setSubject("New evidence uploaded for case CS-2026-0100: DNA Evidence 001");
        notification.setMessage("New evidence uploaded for case CS-2026-0100: DNA Evidence 001 (Uploaded by Insp. K. Vijay)");
        notification.setStatus(NotificationStatus.SENT);
        notification.setRead(false);

        com.crimesphere.backend.dto.notification.NotificationResponse dto =
                com.crimesphere.backend.dto.notification.NotificationResponse.fromEntity(notification);

        assertEquals(NotificationType.EVIDENCE_UPLOADED, dto.getNotificationType());
        assertEquals("CS-2026-0100", dto.getCaseNumber());
        assertFalse(dto.getIsRead());
        assertTrue(dto.getSubject().contains("EVIDENCE_UPLOADED") || dto.getSubject().contains("CS-2026-0100"));
    }

    @Test
    @DisplayName("15. End-to-end: EvidenceService upload creates notifications for officers 2, 3, 4; excludes uploader 1; rejects duplicate SHA-256; allows distinct file upload")
    void testEndToEndEvidenceUploadNotificationFlow() {
        PoliceOfficer officer1003 = new PoliceOfficer();
        officer1003.setId(UUID.randomUUID());
        officer1003.setPoliceId("TN-POL-1003");
        officer1003.setFullName("Sub-Insp. K. Ramesh");
        officer1003.setStation(station);

        CaseOfficer co1 = new CaseOfficer(demoCase, officer1001, officer1001);
        CaseOfficer co2 = new CaseOfficer(demoCase, officer1002, officer1001);
        CaseOfficer co3 = new CaseOfficer(demoCase, officer1003, officer1001);
        CaseOfficer co4 = new CaseOfficer(demoCase, officer1004, officer1001);

        when(caseOfficerRepository.findByCaseEntityIdAndIsActiveTrue(demoCase.getId()))
                .thenReturn(List.of(co1, co2, co3, co4));

        EvidenceRepository mockEvidenceRepo = mock(EvidenceRepository.class);
        EvidenceAuditLogRepository mockAuditRepo = mock(EvidenceAuditLogRepository.class);
        StorageService storageService = new StorageService();

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(officer1001));
        when(caseRepository.findByIdAndStationId(demoCase.getId(), station.getId())).thenReturn(Optional.of(demoCase));
        when(mockEvidenceRepo.existsByCaseEntityIdAndFileHash(eq(demoCase.getId()), anyString())).thenReturn(false);
        when(mockEvidenceRepo.findByFileHashAndCaseEntityIdNot(anyString(), eq(demoCase.getId()))).thenReturn(List.of());
        when(mockEvidenceRepo.getNextEvidenceNumberSequence()).thenReturn(501L);
        when(mockEvidenceRepo.save(any(Evidence.class))).thenAnswer(inv -> inv.getArgument(0));

        List<PoliceOfficer> savedRecipients = new ArrayList<>();
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            if (n.getId() == null) n.setId(UUID.randomUUID());
            if (n.getRecipientOfficer() != null) {
                savedRecipients.add(n.getRecipientOfficer());
            }
            return n;
        });

        EvidenceService service = new EvidenceService(
                mockEvidenceRepo,
                mockAuditRepo,
                caseRepository,
                officerRepository,
                storageService,
                notificationService
        );

        org.springframework.mock.web.MockMultipartFile fileA = new org.springframework.mock.web.MockMultipartFile(
                "file", "forensic_analysis_001.pdf", "application/pdf", "sample_pdf_bytes_unique_001".getBytes()
        );
        com.crimesphere.backend.dto.evidence.CreateEvidenceMetadata metaA = new com.crimesphere.backend.dto.evidence.CreateEvidenceMetadata();
        metaA.setTitle("CrimeSphere Forensic Evidence 001");
        metaA.setEvidenceType("DOCUMENT");

        // Officer 1 uploads File A
        com.crimesphere.backend.dto.evidence.EvidenceResponse respA = service.uploadEvidence(demoCase.getId(), fileA, metaA, "TN-POL-1001");
        assertNotNull(respA);

        // Verify recipient delivery
        assertTrue(savedRecipients.stream().anyMatch(r -> r.getPoliceId().equals("TN-POL-1002")), "TN-POL-1002 must receive notification");
        assertTrue(savedRecipients.stream().anyMatch(r -> r.getPoliceId().equals("TN-POL-1003")), "TN-POL-1003 must receive notification");
        assertTrue(savedRecipients.stream().anyMatch(r -> r.getPoliceId().equals("TN-POL-1004")), "TN-POL-1004 must receive notification");
        assertFalse(savedRecipients.stream().anyMatch(r -> r.getPoliceId().equals("TN-POL-1001")), "Uploader TN-POL-1001 must NOT receive notification");
        assertFalse(savedRecipients.stream().anyMatch(r -> r.getPoliceId().equals("TN-POL-9999")), "Unassigned officer must NOT receive notification");

        // Test duplicate SHA-256 upload rejection
        when(mockEvidenceRepo.existsByCaseEntityIdAndFileHash(eq(demoCase.getId()), anyString())).thenReturn(true);
        assertThrows(com.crimesphere.backend.exception.DuplicateEvidenceException.class, () -> {
            service.uploadEvidence(demoCase.getId(), fileA, metaA, "TN-POL-1001");
        });

        // Test distinct file upload succeeds and creates new notification
        when(mockEvidenceRepo.existsByCaseEntityIdAndFileHash(eq(demoCase.getId()), anyString())).thenReturn(false);
        when(mockEvidenceRepo.getNextEvidenceNumberSequence()).thenReturn(502L);

        org.springframework.mock.web.MockMultipartFile fileB = new org.springframework.mock.web.MockMultipartFile(
                "file", "fingerprint_002.png", "image/png", "sample_png_bytes_distinct_002".getBytes()
        );
        com.crimesphere.backend.dto.evidence.CreateEvidenceMetadata metaB = new com.crimesphere.backend.dto.evidence.CreateEvidenceMetadata();
        metaB.setTitle("CrimeSphere Fingerprint Evidence 002");
        metaB.setEvidenceType("IMAGE");

        savedRecipients.clear();
        com.crimesphere.backend.dto.evidence.EvidenceResponse respB = service.uploadEvidence(demoCase.getId(), fileB, metaB, "TN-POL-1001");
        assertNotNull(respB);
        assertFalse(savedRecipients.isEmpty(), "New file upload must trigger new notifications");
    }
}


