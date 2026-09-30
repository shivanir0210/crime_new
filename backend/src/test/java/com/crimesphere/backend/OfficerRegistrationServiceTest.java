package com.crimesphere.backend;

import com.crimesphere.backend.dto.*;
import com.crimesphere.backend.entity.AuditLog;
import com.crimesphere.backend.entity.AuthOtpChallenge;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.entity.PoliceStation;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.AuthOtpChallengeRepository;
import com.crimesphere.backend.repository.NotificationRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import com.crimesphere.backend.repository.PoliceStationRepository;
import com.crimesphere.backend.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OfficerRegistrationServiceTest {

    @Mock
    private PoliceOfficerRepository officerRepository;

    @Mock
    private PoliceStationRepository stationRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private HttpServletRequest httpRequest;

    private static class TestResendEmailService extends ResendEmailService {
        private String lastToEmail;
        private String lastSubject;
        private int sendCount = 0;

        public TestResendEmailService() {
            super(null, null);
        }

        public String getLastToEmail() {
            return lastToEmail;
        }

        public String getLastSubject() {
            return lastSubject;
        }

        public int getSendCount() {
            return sendCount;
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public NotificationDispatchResult sendEmail(String toEmail, String subject, String textBody, String htmlBody) {
            this.lastToEmail = toEmail;
            this.lastSubject = subject;
            this.sendCount++;
            return NotificationDispatchResult.success("msg-resend-test-123");
        }
    }

    @Mock
    private AuthOtpChallengeRepository challengeRepository;

    private StorageService storageService;
    private EmailService emailService;
    private TestResendEmailService resendEmailService;
    private OfficerOcrService ocrService;
    private OtpService otpService;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;
    private OfficerRegistrationService registrationService;

    private PoliceStation stationCbe;
    private PoliceStation stationEgm;
    private PoliceOfficer stationHeadCbe;
    private PoliceOfficer stationHeadEgm;

    @BeforeEach
    void setUp() {
        storageService = new StorageService();
        emailService = new EmailService(null, null, null);
        resendEmailService = new TestResendEmailService();
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "crimeSphereSuperSecureJwtSecretKeyForPoliceIntelligenceSystem2026!");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 86400000L);

        ocrService = new OfficerOcrService("", "gemini-1.5-flash", new com.fasterxml.jackson.databind.ObjectMapper());
        otpService = new OtpService(challengeRepository, passwordEncoder, resendEmailService);

        authService = new AuthService(
                officerRepository,
                stationRepository,
                auditLogRepository,
                notificationRepository,
                passwordEncoder,
                jwtService,
                emailService,
                resendEmailService,
                storageService,
                ocrService,
                otpService
        );

        registrationService = new OfficerRegistrationService(
                officerRepository,
                storageService,
                resendEmailService,
                auditLogRepository,
                notificationRepository
        );

        // Station 1: Coimbatore Central
        stationCbe = new PoliceStation();
        stationCbe.setId(UUID.randomUUID());
        stationCbe.setStationCode("TN-POLICE-CBE");
        stationCbe.setStationName("Coimbatore Central Police Station");
        stationCbe.setDistrict("Coimbatore");
        stationCbe.setState("Tamil Nadu");
        stationCbe.setHeadName("CBE Station Head");
        stationCbe.setHeadEmail("head.cbe@crimesphere.internal");

        // Station 2: Egmore
        stationEgm = new PoliceStation();
        stationEgm.setId(UUID.randomUUID());
        stationEgm.setStationCode("TN-POLICE-EGM");
        stationEgm.setStationName("Egmore Police Station");
        stationEgm.setDistrict("Chennai");
        stationEgm.setState("Tamil Nadu");
        stationEgm.setHeadName("EGM Station Head");
        stationEgm.setHeadEmail("head.egm@crimesphere.internal");

        // Station Heads
        stationHeadCbe = new PoliceOfficer();
        stationHeadCbe.setId(UUID.randomUUID());
        stationHeadCbe.setPoliceId("TN-POL-1001");
        stationHeadCbe.setFullName("CBE Station Head");
        stationHeadCbe.setStation(stationCbe);
        stationHeadCbe.setRole("STATION_HEAD");
        stationHeadCbe.setIsActive(true);
        stationHeadCbe.setStatus("ACTIVE");

        stationHeadEgm = new PoliceOfficer();
        stationHeadEgm.setId(UUID.randomUUID());
        stationHeadEgm.setPoliceId("TN-POL-1002");
        stationHeadEgm.setFullName("EGM Station Head");
        stationHeadEgm.setStation(stationEgm);
        stationHeadEgm.setRole("STATION_HEAD");
        stationHeadEgm.setIsActive(true);
        stationHeadEgm.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("1. Registration creates PENDING officer and does NOT activate account automatically")
    void testRegistrationCreatesPendingOfficer() {
        UUID stationId = stationCbe.getId();
        OfficerRegistrationRequest request = new OfficerRegistrationRequest(
                "R. Kumar", "TN-POL-5001", "rkumar@example.com", "9876543210",
                "OfficerSecure@123", "OfficerSecure@123", stationId, "INVESTIGATOR"
        );

        String idContent = "POLICE ID: TN-POL-5001\nNAME: R. Kumar\nSTATION: Coimbatore Central Police Station\nROLE: INVESTIGATOR";
        MockMultipartFile idFile = new MockMultipartFile("idDocument", "id_card.pdf", "application/pdf", idContent.getBytes());

        when(stationRepository.findById(stationId)).thenReturn(Optional.of(stationCbe));
        when(officerRepository.findByPoliceId("TN-POL-5001")).thenReturn(Optional.empty());
        when(officerRepository.save(any(PoliceOfficer.class))).thenAnswer(i -> i.getArgument(0));

        OfficerRegistrationResponse response = authService.registerOfficer(request, idFile, httpRequest);

        assertNotNull(response);
        assertEquals("TN-POL-5001", response.getPoliceId());
        assertEquals("PENDING_STATION_APPROVAL", response.getStatus());
        assertEquals(Boolean.FALSE, response.getIsActive(), "Account must NOT be active immediately upon registration");
        assertEquals("PASSED", response.getOcrStatus());

        ArgumentCaptor<PoliceOfficer> captor = ArgumentCaptor.forClass(PoliceOfficer.class);
        verify(officerRepository).save(captor.capture());
        PoliceOfficer saved = captor.getValue();
        assertEquals("PENDING_STATION_APPROVAL", saved.getStatus());
        assertFalse(saved.getIsActive());
        assertTrue(passwordEncoder.matches("OfficerSecure@123", saved.getPasswordHash()));
    }

    @Test
    @DisplayName("2. Duplicate Police ID is rejected")
    void testDuplicatePoliceIdRejected() {
        OfficerRegistrationRequest request = new OfficerRegistrationRequest(
                "R. Kumar", "TN-POL-1001", "rkumar@example.com", "9876543210",
                "OfficerSecure@123", "OfficerSecure@123", stationCbe.getId(), "OFFICER"
        );
        MockMultipartFile idFile = new MockMultipartFile("idDocument", "id_card.pdf", "application/pdf", "dummy".getBytes());

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(stationHeadCbe));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.registerOfficer(request, idFile, httpRequest));
        assertTrue(ex.getMessage().contains("already registered"));
    }

    @Test
    @DisplayName("3. Duplicate Email is rejected")
    void testDuplicateEmailRejected() {
        OfficerRegistrationRequest request = new OfficerRegistrationRequest(
                "Another Officer", "TN-POL-9999", "officer1@crimesphere.internal", "9876543210",
                "OfficerSecure@123", "OfficerSecure@123", stationCbe.getId(), "OFFICER"
        );
        MockMultipartFile idFile = new MockMultipartFile("idDocument", "id_card.pdf", "application/pdf", "dummy".getBytes());

        PoliceOfficer existing = new PoliceOfficer();
        existing.setEmail("officer1@crimesphere.internal");

        when(officerRepository.findByPoliceId("TN-POL-9999")).thenReturn(Optional.empty());
        when(officerRepository.findAll()).thenReturn(List.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.registerOfficer(request, idFile, httpRequest));
        assertTrue(ex.getMessage().contains("email"));
    }

    @Test
    @DisplayName("4. Missing ID document is rejected")
    void testMissingIdDocumentRejected() {
        OfficerRegistrationRequest request = new OfficerRegistrationRequest(
                "R. Kumar", "TN-POL-5001", "rkumar@example.com", "9876543210",
                "OfficerSecure@123", "OfficerSecure@123", stationCbe.getId(), "OFFICER"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.registerOfficer(request, null, httpRequest));
        assertTrue(ex.getMessage().contains("upload is required"));
    }

    @Test
    @DisplayName("5. OCR extraction matches data and results in PASSED status")
    void testOcrSuccessMatching() {
        OfficerRegistrationRequest request = new OfficerRegistrationRequest(
                "R. Kumar", "TN-POL-5001", "rkumar@example.com", "9876543210",
                "OfficerSecure@123", "OfficerSecure@123", stationCbe.getId(), "OFFICER"
        );
        String idContent = "NAME: R. Kumar\nPOLICE ID: TN-POL-5001\nSTATION: Coimbatore Central Police Station\nROLE: OFFICER";
        MockMultipartFile idFile = new MockMultipartFile("idDocument", "id.txt", "text/plain", idContent.getBytes());

        OfficerOcrService.OcrExtractionResult ocrResult = ocrService.processAndVerifyIdDocument(idFile, request, stationCbe);

        assertEquals("PASSED", ocrResult.getOcrStatus());
        assertTrue(ocrResult.isNameMatches());
        assertTrue(ocrResult.isPoliceIdMatches());
        assertTrue(ocrResult.isStationMatches());
        assertTrue(ocrResult.isRoleMatches());
    }

    @Test
    @DisplayName("6. OCR mismatch flags REVIEW_REQUIRED without labeling user fraudulent")
    void testOcrMismatchFlagsReviewRequired() {
        OfficerRegistrationRequest request = new OfficerRegistrationRequest(
                "R. Kumar", "TN-POL-5001", "rkumar@example.com", "9876543210",
                "OfficerSecure@123", "OfficerSecure@123", stationCbe.getId(), "OFFICER"
        );
        // ID card contains different station
        String idContent = "NAME: R. Kumar\nPOLICE ID: TN-POL-5001\nSTATION: Egmore Police Station\nROLE: OFFICER";
        MockMultipartFile idFile = new MockMultipartFile("idDocument", "id.txt", "text/plain", idContent.getBytes());

        OfficerOcrService.OcrExtractionResult ocrResult = ocrService.processAndVerifyIdDocument(idFile, request, stationCbe);

        assertEquals("REVIEW_REQUIRED", ocrResult.getOcrStatus());
        assertFalse(ocrResult.isStationMatches());
        assertTrue(ocrResult.isNameMatches());
        assertTrue(ocrResult.isPoliceIdMatches());
    }

    @Test
    @DisplayName("7. Station Head email is dynamically resolved from police_stations.head_email")
    void testDynamicStationHeadLookup() {
        UUID stationId = stationCbe.getId();
        OfficerRegistrationRequest request = new OfficerRegistrationRequest(
                "S. Sharma", "TN-POL-6001", "ssharma@example.com", "9876543210",
                "OfficerSecure@123", "OfficerSecure@123", stationId, "OFFICER"
        );
        MockMultipartFile idFile = new MockMultipartFile("idDocument", "id.txt", "text/plain",
                "NAME: S. Sharma\nPOLICE ID: TN-POL-6001\nSTATION: Coimbatore Central".getBytes());

        when(stationRepository.findById(stationId)).thenReturn(Optional.of(stationCbe));
        when(officerRepository.findByPoliceId("TN-POL-6001")).thenReturn(Optional.empty());
        when(officerRepository.save(any(PoliceOfficer.class))).thenAnswer(i -> i.getArgument(0));

        authService.registerOfficer(request, idFile, httpRequest);

        // Verify Resend email sent to Coimbatore Station Head email dynamically
        assertEquals("head.cbe@crimesphere.internal", resendEmailService.getLastToEmail());
        assertEquals("New Officer Registration Requires Approval", resendEmailService.getLastSubject());
        assertTrue(resendEmailService.getSendCount() >= 1);
    }

    @Test
    @DisplayName("8. Station Head can approve registration belonging to their station")
    void testStationHeadApproveOwnStationRegistration() {
        PoliceOfficer pendingOfficer = new PoliceOfficer();
        UUID officerId = UUID.randomUUID();
        pendingOfficer.setId(officerId);
        pendingOfficer.setPoliceId("TN-POL-7001");
        pendingOfficer.setFullName("Pending Officer");
        pendingOfficer.setEmail("pending@example.com");
        pendingOfficer.setStation(stationCbe);
        pendingOfficer.setStatus("PENDING_STATION_APPROVAL");
        pendingOfficer.setIsActive(false);

        when(officerRepository.findById(officerId)).thenReturn(Optional.of(pendingOfficer));
        when(officerRepository.save(any(PoliceOfficer.class))).thenAnswer(i -> i.getArgument(0));

        PendingOfficerRegistrationDto approved = registrationService.approveRegistration(officerId, stationHeadCbe, httpRequest);

        assertNotNull(approved);
        assertEquals("ACTIVE", approved.getStatus());
        assertTrue(approved.getIsActive());

        ArgumentCaptor<PoliceOfficer> officerCaptor = ArgumentCaptor.forClass(PoliceOfficer.class);
        verify(officerRepository).save(officerCaptor.capture());
        assertEquals("ACTIVE", officerCaptor.getValue().getStatus());
        assertTrue(officerCaptor.getValue().getIsActive());
        assertEquals(stationHeadCbe, officerCaptor.getValue().getApprovedByOfficer());
        assertNotNull(officerCaptor.getValue().getApprovedAt());
    }

    @Test
    @DisplayName("9. Station Head cannot approve registration from another station (HTTP 403 AccessDeniedException)")
    void testStationHeadCannotApproveOtherStationRegistration() {
        PoliceOfficer pendingOfficer = new PoliceOfficer();
        UUID officerId = UUID.randomUUID();
        pendingOfficer.setId(officerId);
        pendingOfficer.setPoliceId("TN-POL-7001");
        pendingOfficer.setFullName("Pending Officer");
        pendingOfficer.setStation(stationCbe); // Belonging to Coimbatore
        pendingOfficer.setStatus("PENDING_STATION_APPROVAL");
        pendingOfficer.setIsActive(false);

        when(officerRepository.findById(officerId)).thenReturn(Optional.of(pendingOfficer));

        // Attempted approval by Egmore Station Head
        assertThrows(AccessDeniedException.class,
                () -> registrationService.approveRegistration(officerId, stationHeadEgm, httpRequest));

        verify(officerRepository, never()).save(any(PoliceOfficer.class));
    }

    @Test
    @DisplayName("10. Rejection requires reason, marks account REJECTED, and prevents activation")
    void testRejectionRequiresReasonAndPreventsActivation() {
        PoliceOfficer pendingOfficer = new PoliceOfficer();
        UUID officerId = UUID.randomUUID();
        pendingOfficer.setId(officerId);
        pendingOfficer.setPoliceId("TN-POL-8001");
        pendingOfficer.setFullName("Rejected Officer");
        pendingOfficer.setEmail("rejected@example.com");
        pendingOfficer.setStation(stationCbe);
        pendingOfficer.setStatus("PENDING_STATION_APPROVAL");
        pendingOfficer.setIsActive(false);

        when(officerRepository.findById(officerId)).thenReturn(Optional.of(pendingOfficer));
        when(officerRepository.save(any(PoliceOfficer.class))).thenAnswer(i -> i.getArgument(0));

        // Rejection without reason throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> registrationService.rejectRegistration(officerId, "   ", stationHeadCbe, httpRequest));

        // Rejection with valid reason
        PendingOfficerRegistrationDto rejected = registrationService.rejectRegistration(
                officerId, "ID document photograph does not match official records", stationHeadCbe, httpRequest);

        assertEquals("REJECTED", rejected.getStatus());
        assertFalse(rejected.getIsActive());
        assertEquals("ID document photograph does not match official records", rejected.getRejectionReason());
    }

    @Test
    @DisplayName("11. Pending account cannot login and receives clear status message")
    void testPendingAccountCannotLogin() {
        PoliceOfficer pendingOfficer = new PoliceOfficer();
        pendingOfficer.setId(UUID.randomUUID());
        pendingOfficer.setPoliceId("TN-POL-9001");
        pendingOfficer.setPasswordHash(passwordEncoder.encode("TestPass@123"));
        pendingOfficer.setStatus("PENDING_STATION_APPROVAL");
        pendingOfficer.setIsActive(false);
        pendingOfficer.setStation(stationCbe);

        when(officerRepository.findByPoliceId("TN-POL-9001")).thenReturn(Optional.of(pendingOfficer));

        LoginRequest loginRequest = new LoginRequest("TN-POL-9001", "TestPass@123");
        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest, httpRequest));

        assertEquals("Your registration is awaiting Station Head approval.", ex.getMessage());

        // Verify audit log recorded for blocked pending login
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertEquals("LOGIN_BLOCKED_PENDING_APPROVAL", auditCaptor.getValue().getAction());
    }

    @Test
    @DisplayName("12. Rejected account cannot login and receives rejection status message")
    void testRejectedAccountCannotLogin() {
        PoliceOfficer rejectedOfficer = new PoliceOfficer();
        rejectedOfficer.setId(UUID.randomUUID());
        rejectedOfficer.setPoliceId("TN-POL-9002");
        rejectedOfficer.setPasswordHash(passwordEncoder.encode("TestPass@123"));
        rejectedOfficer.setStatus("REJECTED");
        rejectedOfficer.setIsActive(false);
        rejectedOfficer.setStation(stationCbe);

        when(officerRepository.findByPoliceId("TN-POL-9002")).thenReturn(Optional.of(rejectedOfficer));

        LoginRequest loginRequest = new LoginRequest("TN-POL-9002", "TestPass@123");
        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest, httpRequest));

        assertEquals("Your registration was rejected. Please contact your station administrator.", ex.getMessage());
    }

    @Test
    @DisplayName("13. Active approved officer can login successfully via OTP and generate JWT")
    void testActiveApprovedOfficerLogin() {
        PoliceOfficer activeOfficer = new PoliceOfficer();
        UUID officerId = UUID.randomUUID();
        activeOfficer.setId(officerId);
        activeOfficer.setPoliceId("TN-POL-9003");
        activeOfficer.setFullName("Active Officer");
        activeOfficer.setEmail("active.officer@crimesphere.internal");
        activeOfficer.setPasswordHash(passwordEncoder.encode("TestPass@123"));
        activeOfficer.setStatus("ACTIVE");
        activeOfficer.setIsActive(true);
        activeOfficer.setStation(stationCbe);

        when(officerRepository.findByPoliceId("TN-POL-9003")).thenReturn(Optional.of(activeOfficer));
        when(challengeRepository.countChallengesCreatedSince(any(UUID.class), any())).thenReturn(0L);
        when(challengeRepository.findActiveByOfficerId(any(UUID.class))).thenReturn(new ArrayList<>());
        when(challengeRepository.save(any(AuthOtpChallenge.class))).thenAnswer(i -> {
            AuthOtpChallenge c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        LoginRequest loginRequest = new LoginRequest("TN-POL-9003", "TestPass@123");
        LoginChallengeResponse response = authService.login(loginRequest, httpRequest);

        assertNotNull(response);
        assertTrue(response.isRequiresOtp());
        assertNotNull(response.getChallengeId());

        // Now verify OTP
        UUID challengeId = response.getChallengeId();
        String plainOtp = "123456";
        String otpHash = passwordEncoder.encode(plainOtp);
        AuthOtpChallenge challenge = new AuthOtpChallenge(
                activeOfficer,
                otpHash,
                OffsetDateTime.now().plusMinutes(5),
                "127.0.0.1",
                "Mozilla/5.0"
        );
        challenge.setId(challengeId);
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        VerifyOtpRequest verifyRequest = new VerifyOtpRequest(challengeId, plainOtp);
        LoginResponse loginResponse = authService.verifyOtpAndGenerateToken(verifyRequest, httpRequest);

        assertNotNull(loginResponse);
        assertNotNull(loginResponse.getToken());
        assertEquals("TN-POL-9003", jwtService.extractPoliceId(loginResponse.getToken()));
        assertEquals("TN-POL-9003", loginResponse.getOfficer().getPoliceId());
    }

    @Test
    @DisplayName("14. Officer ID document download enforces station authorization")
    void testIdDocumentAuthorization() {
        PoliceOfficer targetOfficer = new PoliceOfficer();
        UUID officerId = UUID.randomUUID();
        targetOfficer.setId(officerId);
        targetOfficer.setPoliceId("TN-POL-7777");
        targetOfficer.setStation(stationCbe);

        MockMultipartFile docFile = new MockMultipartFile("idDocument", "id.pdf", "application/pdf", "PDF-DATA".getBytes());
        String storedPath = storageService.storeOfficerIdDocument(officerId, docFile);
        targetOfficer.setIdDocumentPath(storedPath);

        when(officerRepository.findById(officerId)).thenReturn(Optional.of(targetOfficer));

        // Authorized: Station Head of same station
        Resource res = registrationService.getOfficerIdDocumentResource(officerId, stationHeadCbe, httpRequest);
        assertNotNull(res);
        assertTrue(res.exists());

        // Unauthorized: Station Head of another station
        assertThrows(AccessDeniedException.class,
                () -> registrationService.getOfficerIdDocumentResource(officerId, stationHeadEgm, httpRequest));
    }

    @Test
    @DisplayName("15. Password and password hash are never exposed in registration response or DTOs")
    void testPasswordNeverExposedInResponses() {
        OfficerRegistrationRequest request = new OfficerRegistrationRequest(
                "R. Kumar", "TN-POL-5001", "rkumar@example.com", "9876543210",
                "SuperSecretPassword!@#", "SuperSecretPassword!@#", stationCbe.getId(), "OFFICER"
        );
        MockMultipartFile idFile = new MockMultipartFile("idDocument", "id.pdf", "application/pdf", "dummy".getBytes());

        when(stationRepository.findById(stationCbe.getId())).thenReturn(Optional.of(stationCbe));
        when(officerRepository.findByPoliceId("TN-POL-5001")).thenReturn(Optional.empty());
        when(officerRepository.save(any(PoliceOfficer.class))).thenAnswer(i -> i.getArgument(0));

        OfficerRegistrationResponse response = authService.registerOfficer(request, idFile, httpRequest);

        // Verify response DTO has no password getter or exposed password hash
        assertNotNull(response);
        assertEquals("TN-POL-5001", response.getPoliceId());
        assertFalse(response.toString().contains("SuperSecretPassword!@#"));
    }

    @Test
    @DisplayName("16. Audit log captures REGISTRATION_SUBMITTED, ID_DOCUMENT_UPLOADED, and OCR results")
    void testAuditLogsRecordedDuringRegistration() {
        OfficerRegistrationRequest request = new OfficerRegistrationRequest(
                "R. Kumar", "TN-POL-5001", "rkumar@example.com", "9876543210",
                "TestPass@123", "TestPass@123", stationCbe.getId(), "OFFICER"
        );
        String idContent = "NAME: R. Kumar\nPOLICE ID: TN-POL-5001\nSTATION: Coimbatore Central";
        MockMultipartFile idFile = new MockMultipartFile("idDocument", "id.pdf", "application/pdf", idContent.getBytes());

        when(stationRepository.findById(stationCbe.getId())).thenReturn(Optional.of(stationCbe));
        when(officerRepository.findByPoliceId("TN-POL-5001")).thenReturn(Optional.empty());
        when(officerRepository.save(any(PoliceOfficer.class))).thenAnswer(i -> i.getArgument(0));

        authService.registerOfficer(request, idFile, httpRequest);

        // Check audit log recordings
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, atLeast(3)).save(captor.capture());
        List<String> actions = captor.getAllValues().stream().map(AuditLog::getAction).toList();

        assertTrue(actions.contains("REGISTRATION_SUBMITTED"));
        assertTrue(actions.contains("ID_DOCUMENT_UPLOADED"));
        assertTrue(actions.contains("OCR_COMPLETED") || actions.contains("OCR_REVIEW_REQUIRED"));
        assertTrue(actions.contains("STATION_HEAD_NOTIFIED"));
    }

    @Test
    @DisplayName("17. Station Head receives in-app notification on new officer registration")
    void testStationHeadReceivesInAppNotification() {
        UUID stationId = stationCbe.getId();
        OfficerRegistrationRequest request = new OfficerRegistrationRequest(
                "N. Sundar", "TN-POL-5555", "nsundar@example.com", "9876543210",
                "SecurePassword!@#", "SecurePassword!@#", stationId, "INVESTIGATOR"
        );
        MockMultipartFile idFile = new MockMultipartFile("idDocument", "id.pdf", "application/pdf", "dummy".getBytes());

        when(stationRepository.findById(stationId)).thenReturn(Optional.of(stationCbe));
        when(officerRepository.findByPoliceId("TN-POL-5555")).thenReturn(Optional.empty());
        when(officerRepository.findByStationId(stationId)).thenReturn(List.of(stationHeadCbe));
        when(officerRepository.save(any(PoliceOfficer.class))).thenAnswer(i -> i.getArgument(0));

        authService.registerOfficer(request, idFile, httpRequest);

        ArgumentCaptor<com.crimesphere.backend.entity.Notification> notifCaptor = ArgumentCaptor.forClass(com.crimesphere.backend.entity.Notification.class);
        verify(notificationRepository, atLeastOnce()).save(notifCaptor.capture());

        com.crimesphere.backend.entity.Notification notif = notifCaptor.getValue();
        assertEquals(stationHeadCbe, notif.getRecipientOfficer());
        assertEquals(com.crimesphere.backend.entity.NotificationChannel.IN_APP, notif.getChannel());
        assertEquals(com.crimesphere.backend.entity.NotificationType.NEW_OFFICER_REGISTRATION, notif.getNotificationType());
        assertEquals("NEW OFFICER REGISTRATION", notif.getSubject());
        assertTrue(notif.getMessage().contains("N. Sundar"));
        assertTrue(notif.getMessage().contains("TN-POL-5555"));
    }

    @Test
    @DisplayName("18. Applicant receives in-app notification upon Station Head approval")
    void testApplicantReceivesApprovalInAppNotification() {
        PoliceOfficer pendingOfficer = new PoliceOfficer();
        UUID officerId = UUID.randomUUID();
        pendingOfficer.setId(officerId);
        pendingOfficer.setPoliceId("TN-POL-7700");
        pendingOfficer.setFullName("Approve Applicant");
        pendingOfficer.setEmail("approve.app@example.com");
        pendingOfficer.setStation(stationCbe);
        pendingOfficer.setStatus("PENDING_STATION_APPROVAL");
        pendingOfficer.setIsActive(false);

        when(officerRepository.findById(officerId)).thenReturn(Optional.of(pendingOfficer));
        when(officerRepository.save(any(PoliceOfficer.class))).thenAnswer(i -> i.getArgument(0));

        registrationService.approveRegistration(officerId, stationHeadCbe, httpRequest);

        ArgumentCaptor<com.crimesphere.backend.entity.Notification> notifCaptor = ArgumentCaptor.forClass(com.crimesphere.backend.entity.Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        com.crimesphere.backend.entity.Notification notif = notifCaptor.getValue();
        assertEquals(pendingOfficer, notif.getRecipientOfficer());
        assertEquals(com.crimesphere.backend.entity.NotificationType.OFFICER_ACCOUNT_APPROVED, notif.getNotificationType());
        assertEquals("YOUR OFFICER ACCOUNT HAS BEEN APPROVED", notif.getSubject());
        assertTrue(notif.getMessage().contains("TN-POL-7700"));
    }

    @Test
    @DisplayName("19. Applicant receives in-app notification upon Station Head rejection")
    void testApplicantReceivesRejectionInAppNotification() {
        PoliceOfficer pendingOfficer = new PoliceOfficer();
        UUID officerId = UUID.randomUUID();
        pendingOfficer.setId(officerId);
        pendingOfficer.setPoliceId("TN-POL-8800");
        pendingOfficer.setFullName("Reject Applicant");
        pendingOfficer.setEmail("reject.app@example.com");
        pendingOfficer.setStation(stationCbe);
        pendingOfficer.setStatus("PENDING_STATION_APPROVAL");
        pendingOfficer.setIsActive(false);

        when(officerRepository.findById(officerId)).thenReturn(Optional.of(pendingOfficer));
        when(officerRepository.save(any(PoliceOfficer.class))).thenAnswer(i -> i.getArgument(0));

        registrationService.rejectRegistration(officerId, "Invalid Police Identification Document", stationHeadCbe, httpRequest);

        ArgumentCaptor<com.crimesphere.backend.entity.Notification> notifCaptor = ArgumentCaptor.forClass(com.crimesphere.backend.entity.Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());

        com.crimesphere.backend.entity.Notification notif = notifCaptor.getValue();
        assertEquals(pendingOfficer, notif.getRecipientOfficer());
        assertEquals(com.crimesphere.backend.entity.NotificationType.OFFICER_REGISTRATION_REJECTED, notif.getNotificationType());
        assertEquals("YOUR OFFICER REGISTRATION WAS REJECTED", notif.getSubject());
        assertTrue(notif.getMessage().contains("Invalid Police Identification Document"));
    }

    @Test
    @DisplayName("20. Station Head viewing registration detail logs REGISTRATION_VIEWED and prevents re-approving active registration")
    void testViewRegistrationAuditLogAndNonPendingApprovalCheck() {
        PoliceOfficer pendingOfficer = new PoliceOfficer();
        UUID officerId = UUID.randomUUID();
        pendingOfficer.setId(officerId);
        pendingOfficer.setPoliceId("TN-POL-9900");
        pendingOfficer.setFullName("Review Applicant");
        pendingOfficer.setStation(stationCbe);
        pendingOfficer.setStatus("PENDING_STATION_APPROVAL");
        pendingOfficer.setIsActive(false);

        when(officerRepository.findById(officerId)).thenReturn(Optional.of(pendingOfficer));

        PendingOfficerRegistrationDto detail = registrationService.getRegistrationDetail(officerId, stationHeadCbe, httpRequest);
        assertNotNull(detail);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertEquals("REGISTRATION_VIEWED", auditCaptor.getValue().getAction());

        // Now test that approving an already ACTIVE officer throws IllegalStateException
        pendingOfficer.setStatus("ACTIVE");
        pendingOfficer.setIsActive(true);
        assertThrows(IllegalStateException.class,
                () -> registrationService.approveRegistration(officerId, stationHeadCbe, httpRequest));
    }
}
