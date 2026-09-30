package com.crimesphere.backend;

import com.crimesphere.backend.dto.AuthMeResponse;
import com.crimesphere.backend.dto.LoginRequest;
import com.crimesphere.backend.dto.LoginResponse;
import com.crimesphere.backend.entity.AuditLog;
import com.crimesphere.backend.entity.Notification;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.entity.PoliceStation;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.NotificationAuditLogRepository;
import com.crimesphere.backend.repository.NotificationRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import com.crimesphere.backend.service.AuthService;
import com.crimesphere.backend.service.EmailService;
import com.crimesphere.backend.service.JwtService;
import com.crimesphere.backend.service.OfficerOcrService;
import com.crimesphere.backend.service.OtpService;
import com.crimesphere.backend.service.ResendEmailService;
import com.crimesphere.backend.service.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private PoliceOfficerRepository officerRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationAuditLogRepository notificationAuditLogRepository;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private com.crimesphere.backend.repository.AuthOtpChallengeRepository challengeRepository;

    @Mock
    private com.crimesphere.backend.repository.PoliceStationRepository stationRepository;

    @Mock
    private HttpServletRequest httpRequest;

    private ResendEmailService resendEmailService;
    private StorageService storageService;
    private OfficerOcrService ocrService;
    private OtpService otpService;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private EmailService emailService;
    private AuthService authService;

    private PoliceOfficer activeOfficer;
    private PoliceOfficer inactiveOfficer;
    private PoliceStation station;
    private final String rawPassword = "Demo@123";
    private String bcryptHash;

    @BeforeEach
    void setUp() {
        resendEmailService = new ResendEmailService(null, null);
        storageService = new StorageService();
        ocrService = new OfficerOcrService("", "gemini-1.5-flash", new com.fasterxml.jackson.databind.ObjectMapper());
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "crimeSphereSuperSecureJwtSecretKeyForPoliceIntelligenceSystem2026!");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 86400000L);

        otpService = new OtpService(challengeRepository, passwordEncoder, resendEmailService);
        emailService = new EmailService(javaMailSender, notificationRepository, notificationAuditLogRepository);

        authService = new AuthService(
                officerRepository,
                stationRepository,
                auditLogRepository,
                passwordEncoder,
                jwtService,
                emailService,
                resendEmailService,
                storageService,
                ocrService,
                otpService
        );

        bcryptHash = passwordEncoder.encode(rawPassword);

        station = new PoliceStation();
        station.setId(UUID.randomUUID());
        station.setStationCode("TN-POLICE-CBE");
        station.setStationName("Coimbatore Central Police Station");
        station.setHeadName("Development Station Head");
        station.setHeadEmail("head.cbe@crimesphere.internal");

        activeOfficer = new PoliceOfficer();
        activeOfficer.setId(UUID.randomUUID());
        activeOfficer.setPoliceId("TN-POL-1001");
        activeOfficer.setFullName("Development Officer One");
        activeOfficer.setEmail("officer1@crimesphere.internal");
        activeOfficer.setPhone("9876543210");
        activeOfficer.setPasswordHash(bcryptHash);
        activeOfficer.setRole("OFFICER");
        activeOfficer.setIsActive(true);
        activeOfficer.setStation(station);

        inactiveOfficer = new PoliceOfficer();
        inactiveOfficer.setId(UUID.randomUUID());
        inactiveOfficer.setPoliceId("TN-POL-1002");
        inactiveOfficer.setFullName("Inactive Officer");
        inactiveOfficer.setPasswordHash(bcryptHash);
        inactiveOfficer.setIsActive(false);
        inactiveOfficer.setStation(station);
    }

    @Test
    @DisplayName("1. Phase 1 Login with valid credentials initiates OTP challenge and does NOT return JWT")
    void testSuccessfulPhase1Login() {
        LoginRequest request = new LoginRequest("TN-POL-1001", "Demo@123");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(activeOfficer));
        lenient().when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        lenient().when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(challengeRepository.countChallengesCreatedSince(any(UUID.class), any())).thenReturn(0L);
        lenient().when(challengeRepository.save(any(com.crimesphere.backend.entity.AuthOtpChallenge.class)))
                .thenAnswer(i -> {
                    com.crimesphere.backend.entity.AuthOtpChallenge c = i.getArgument(0);
                    c.setId(UUID.randomUUID());
                    return c;
                });

        com.crimesphere.backend.dto.LoginChallengeResponse response = authService.login(request, httpRequest);

        assertNotNull(response);
        assertTrue(response.isRequiresOtp());
        assertNotNull(response.getChallengeId());
        assertEquals("of***@crimesphere.internal", response.getMaskedEmail());

        // Verify AuditLog creation with LOGIN_OTP_REQUESTED
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(auditCaptor.capture());
        assertEquals("LOGIN_OTP_REQUESTED", auditCaptor.getValue().getAction());
        assertEquals("SUCCESS", auditCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("1b. Phase 2 OTP verification generates JWT and LoginResponse")
    void testSuccessfulPhase2OtpVerification() {
        UUID challengeId = UUID.randomUUID();
        String plainOtp = "654321";
        String otpHash = passwordEncoder.encode(plainOtp);

        com.crimesphere.backend.entity.AuthOtpChallenge challenge = new com.crimesphere.backend.entity.AuthOtpChallenge(
                activeOfficer,
                otpHash,
                java.time.OffsetDateTime.now().plusMinutes(5),
                "127.0.0.1",
                "Mozilla/5.0"
        );
        challenge.setId(challengeId);

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        lenient().when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
        lenient().doNothing().when(javaMailSender).send(any(SimpleMailMessage.class));

        com.crimesphere.backend.dto.VerifyOtpRequest verifyRequest = new com.crimesphere.backend.dto.VerifyOtpRequest(challengeId, plainOtp);
        com.crimesphere.backend.dto.LoginResponse response = authService.verifyOtpAndGenerateToken(verifyRequest, httpRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("TN-POL-1001", jwtService.extractPoliceId(response.getToken()));
        assertEquals(86400000L, response.getExpiresIn());
        assertEquals("TN-POL-1001", response.getOfficer().getPoliceId());
        assertTrue(challenge.getIsUsed());
    }

    @Test
    @DisplayName("2. Login with wrong password throws 401 BadCredentialsException and records FAILED audit log")
    void testWrongPassword() {
        LoginRequest request = new LoginRequest("TN-POL-1001", "WrongPass123");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(activeOfficer));

        assertThrows(BadCredentialsException.class, () -> authService.login(request, httpRequest));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(auditCaptor.capture());
        assertEquals("LOGIN_FAILED", auditCaptor.getValue().getAction());
        assertEquals("FAILED", auditCaptor.getValue().getStatus());
        verifyNoInteractions(javaMailSender);
    }

    @Test
    @DisplayName("3. Login with unknown Police ID throws 401 BadCredentialsException")
    void testUnknownPoliceId() {
        LoginRequest request = new LoginRequest("UNKNOWN-ID", "Demo@123");

        when(officerRepository.findByPoliceId("UNKNOWN-ID")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(request, httpRequest));

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
        verifyNoInteractions(javaMailSender);
    }

    @Test
    @DisplayName("4. Login with inactive officer account is rejected")
    void testInactiveOfficerLogin() {
        LoginRequest request = new LoginRequest("TN-POL-1002", "Demo@123");

        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(inactiveOfficer));

        assertThrows(BadCredentialsException.class, () -> authService.login(request, httpRequest));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(auditCaptor.capture());
        assertEquals("BLOCKED", auditCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("5. Email notification failure does not cause login failure")
    void testEmailFailureResilience() {
        UUID challengeId = UUID.randomUUID();
        String plainOtp = "123456";
        String otpHash = passwordEncoder.encode(plainOtp);

        com.crimesphere.backend.entity.AuthOtpChallenge challenge = new com.crimesphere.backend.entity.AuthOtpChallenge(
                activeOfficer,
                otpHash,
                java.time.OffsetDateTime.now().plusMinutes(5),
                "127.0.0.1",
                "Mozilla/5.0"
        );
        challenge.setId(challengeId);

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        lenient().when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        // Simulate SMTP error
        lenient().doThrow(new RuntimeException("SMTP connection timed out"))
                .when(javaMailSender).send(any(SimpleMailMessage.class));

        com.crimesphere.backend.dto.VerifyOtpRequest verifyRequest = new com.crimesphere.backend.dto.VerifyOtpRequest(challengeId, plainOtp);
        LoginResponse response = authService.verifyOtpAndGenerateToken(verifyRequest, httpRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("TN-POL-1001", jwtService.extractPoliceId(response.getToken()));
    }

    @Test
    @DisplayName("6. getAuthMe returns safe officer identity information without secrets")
    void testGetAuthMe() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(activeOfficer));

        AuthMeResponse response = authService.getAuthMe("TN-POL-1001");

        assertNotNull(response);
        assertEquals("TN-POL-1001", response.getPoliceId());
        assertEquals("Development Officer One", response.getFullName());
        assertEquals("officer1@crimesphere.internal", response.getEmail());
        assertEquals("OFFICER", response.getRole());
        assertEquals(station.getId().toString(), response.getStationId());
    }

    @Test
    @DisplayName("7. Password verification uses BCrypt and matches plain text input")
    void testBCryptPasswordVerification() {
        assertTrue(passwordEncoder.matches("Demo@123", bcryptHash));
        assertFalse(passwordEncoder.matches("WrongPassword", bcryptHash));
    }
}
