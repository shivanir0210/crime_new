package com.crimesphere.backend;

import com.crimesphere.backend.dto.*;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EgmoreStationHeadAuthTest {

    @Mock
    private PoliceOfficerRepository officerRepository;

    @Mock
    private PoliceStationRepository stationRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuthOtpChallengeRepository challengeRepository;

    @Mock
    private HttpServletRequest httpRequest;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private ResendEmailService resendEmailService;
    private StorageService storageService;
    private OfficerOcrService ocrService;
    private OtpService otpService;
    private AuthService authService;
    private OfficerRegistrationService registrationService;

    private PoliceStation egmoreStation;
    private PoliceStation cbeStation;
    private PoliceOfficer egmoreStationHead;

    // Same verified BCrypt hash as seeded in V36 migration for Demo@123
    private static final String SEEDED_BCRYPT_HASH = "$2a$10$rheXR.OOQ0hHW20S2RUZieD2fLpzFsflOZ3VBuIjM7B9sKn6Ex8Pi";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "crimeSphereSuperSecureJwtSecretKeyForPoliceIntelligenceSystem2026!");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 86400000L);

        resendEmailService = new ResendEmailService(null, null);
        storageService = new StorageService();
        ocrService = new OfficerOcrService("", "gemini-1.5-flash", new com.fasterxml.jackson.databind.ObjectMapper());
        otpService = new OtpService(challengeRepository, passwordEncoder, resendEmailService);

        authService = new AuthService(
                officerRepository,
                stationRepository,
                auditLogRepository,
                notificationRepository,
                passwordEncoder,
                jwtService,
                null,
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

        // Egmore Police Station
        egmoreStation = new PoliceStation();
        egmoreStation.setId(UUID.fromString("b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22"));
        egmoreStation.setStationCode("TN-POLICE-EGM");
        egmoreStation.setStationName("Egmore Police Station");
        egmoreStation.setDistrict("Chennai");
        egmoreStation.setState("Tamil Nadu");
        egmoreStation.setHeadName("Egmore Station Head");
        egmoreStation.setHeadEmail("head.egm@example.com");

        // Coimbatore Station (for isolation tests)
        cbeStation = new PoliceStation();
        cbeStation.setId(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));
        cbeStation.setStationCode("TN-POLICE-CBE");
        cbeStation.setStationName("Coimbatore Central Police Station");

        // Demo Station Head: TN-HEAD-001
        egmoreStationHead = new PoliceOfficer();
        egmoreStationHead.setId(UUID.fromString("a1000000-9c0b-4ef8-bb6d-6bb9bd380001"));
        egmoreStationHead.setPoliceId("TN-HEAD-001");
        egmoreStationHead.setFullName("Egmore Station Head");
        egmoreStationHead.setEmail("head.egm@example.com");
        egmoreStationHead.setPhone("9876543220");
        egmoreStationHead.setStation(egmoreStation);
        egmoreStationHead.setRole("STATION_HEAD");
        egmoreStationHead.setStatus("ACTIVE");
        egmoreStationHead.setIsActive(true);
        egmoreStationHead.setPasswordHash(SEEDED_BCRYPT_HASH);
    }

    @Test
    @DisplayName("1. BCrypt hash matches password Demo@123")
    void testBcryptHashMatchesDemoPassword() {
        assertTrue(passwordEncoder.matches("Demo@123", SEEDED_BCRYPT_HASH),
                "Seeded BCrypt hash in migration must match 'Demo@123'");
        assertFalse(passwordEncoder.matches("WrongPass!@#", SEEDED_BCRYPT_HASH));
    }

    @Test
    @DisplayName("2. Safe verification query returns expected non-sensitive officer profile")
    void testSafeProfileVerification() {
        assertEquals("TN-HEAD-001", egmoreStationHead.getPoliceId());
        assertEquals("Egmore Station Head", egmoreStationHead.getFullName());
        assertEquals("head.egm@example.com", egmoreStationHead.getEmail());
        assertEquals(egmoreStation.getId(), egmoreStationHead.getStation().getId());
        assertEquals("STATION_HEAD", egmoreStationHead.getRole());
        assertTrue(egmoreStationHead.getIsActive());
        assertEquals("ACTIVE", egmoreStationHead.getStatus());
    }

    @Test
    @DisplayName("3. TN-HEAD-001 + Demo@123 successfully completes Phase 1 and Phase 2 login and gets valid JWT")
    void testEgmoreStationHeadAuthenticationFlow() {
        when(officerRepository.findByPoliceId("TN-HEAD-001")).thenReturn(Optional.of(egmoreStationHead));
        lenient().when(challengeRepository.countChallengesCreatedSince(any(UUID.class), any())).thenReturn(0L);
        lenient().when(challengeRepository.findActiveByOfficerId(any(UUID.class))).thenReturn(new ArrayList<>());
        lenient().when(challengeRepository.save(any(AuthOtpChallenge.class))).thenAnswer(i -> {
            AuthOtpChallenge c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        // Phase 1: Login challenge
        LoginRequest loginRequest = new LoginRequest("TN-HEAD-001", "Demo@123");
        LoginChallengeResponse challengeResponse = authService.login(loginRequest, httpRequest);

        assertNotNull(challengeResponse);
        assertTrue(challengeResponse.isRequiresOtp());
        assertNotNull(challengeResponse.getChallengeId());

        // Phase 2: Verify OTP
        UUID challengeId = challengeResponse.getChallengeId();
        String plainOtp = "654321";
        AuthOtpChallenge challenge = new AuthOtpChallenge(
                egmoreStationHead,
                passwordEncoder.encode(plainOtp),
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
        assertEquals("TN-HEAD-001", loginResponse.getOfficer().getPoliceId());
        assertEquals("STATION_HEAD", loginResponse.getOfficer().getRole());
        assertEquals("TN-HEAD-001", jwtService.extractPoliceId(loginResponse.getToken()));
        assertFalse(jwtService.isTokenExpired(loginResponse.getToken()));
    }

    @Test
    @DisplayName("4. Egmore Station Head can view and approve registrations for Egmore station")
    void testEgmoreStationHeadCanApproveOwnStationRegistration() {
        PoliceOfficer applicant = new PoliceOfficer();
        UUID applicantId = UUID.randomUUID();
        applicant.setId(applicantId);
        applicant.setPoliceId("TN-POL-EGM-501");
        applicant.setFullName("Officer Applicant Egmore");
        applicant.setStation(egmoreStation);
        applicant.setStatus("PENDING_STATION_APPROVAL");
        applicant.setIsActive(false);

        when(officerRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(officerRepository.save(any(PoliceOfficer.class))).thenAnswer(i -> i.getArgument(0));

        PendingOfficerRegistrationDto approved = registrationService.approveRegistration(applicantId, egmoreStationHead, httpRequest);

        assertNotNull(approved);
        assertEquals("ACTIVE", approved.getStatus());
        assertTrue(approved.getIsActive());
        assertEquals("TN-POL-EGM-501", approved.getPoliceId());
    }

    @Test
    @DisplayName("5. Egmore Station Head is blocked from approving registrations for Coimbatore station (HTTP 403)")
    void testEgmoreStationHeadBlockedFromOtherStationRegistration() {
        PoliceOfficer cbeApplicant = new PoliceOfficer();
        UUID cbeApplicantId = UUID.randomUUID();
        cbeApplicant.setId(cbeApplicantId);
        cbeApplicant.setPoliceId("TN-POL-CBE-501");
        cbeApplicant.setFullName("Officer Applicant CBE");
        cbeApplicant.setStation(cbeStation); // Belongs to Coimbatore
        cbeApplicant.setStatus("PENDING_STATION_APPROVAL");
        cbeApplicant.setIsActive(false);

        when(officerRepository.findById(cbeApplicantId)).thenReturn(Optional.of(cbeApplicant));

        assertThrows(AccessDeniedException.class,
                () -> registrationService.approveRegistration(cbeApplicantId, egmoreStationHead, httpRequest));

        verify(officerRepository, never()).save(any(PoliceOfficer.class));
    }
}
