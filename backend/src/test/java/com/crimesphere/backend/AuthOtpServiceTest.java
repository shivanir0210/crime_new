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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
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
public class AuthOtpServiceTest {

    @Mock
    private PoliceOfficerRepository officerRepository;

    @Mock
    private PoliceStationRepository stationRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuthOtpChallengeRepository challengeRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private HttpServletRequest httpRequest;

    private static class TestResendEmailService extends ResendEmailService {
        String lastRecipient;
        String lastSubject;
        String lastTextBody;
        String lastHtmlBody;

        public TestResendEmailService() {
            super(null, null);
        }

        @Override
        public NotificationDispatchResult sendEmail(String to, String subject, String textBody, String htmlBody) {
            this.lastRecipient = to;
            this.lastSubject = subject;
            this.lastTextBody = textBody;
            this.lastHtmlBody = htmlBody;
            return NotificationDispatchResult.success("msg-otp-test-12345");
        }
    }

    private TestResendEmailService resendEmailService;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private OtpService otpService;
    private AuthService authService;

    private PoliceOfficer activeOfficer;
    private PoliceStation station;
    private final String rawPassword = "Demo@123";
    private String bcryptHash;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        bcryptHash = passwordEncoder.encode(rawPassword);

        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "crimeSphereSuperSecureJwtSecretKeyForPoliceIntelligenceSystem2026!");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L); // 1 hour

        resendEmailService = new TestResendEmailService();
        otpService = new OtpService(challengeRepository, passwordEncoder, resendEmailService);

        EmailService emailService = new EmailService(null, notificationRepository, null);
        StorageService storageService = new StorageService();
        OfficerOcrService ocrService = new OfficerOcrService("", "gemini-1.5-flash", new com.fasterxml.jackson.databind.ObjectMapper());

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
        activeOfficer.setStatus("ACTIVE");
        activeOfficer.setStation(station);

        lenient().when(notificationRepository.save(any(com.crimesphere.backend.entity.Notification.class))).thenAnswer(i -> {
            com.crimesphere.backend.entity.Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
    }

    @Test
    @DisplayName("1. Valid credentials returns OTP challenge and does NOT return JWT")
    void testValidCredentialsReturnsOtpChallengeWithoutJwt() {
        LoginRequest request = new LoginRequest("TN-POL-1001", "Demo@123");
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(activeOfficer));
        when(challengeRepository.countChallengesCreatedSince(any(UUID.class), any())).thenReturn(0L);
        when(challengeRepository.findActiveByOfficerId(any(UUID.class))).thenReturn(new ArrayList<>());
        when(challengeRepository.save(any(AuthOtpChallenge.class))).thenAnswer(i -> {
            AuthOtpChallenge c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        LoginChallengeResponse response = authService.login(request, httpRequest);

        assertNotNull(response);
        assertTrue(response.isRequiresOtp());
        assertNotNull(response.getChallengeId());
        assertEquals("of***@crimesphere.internal", response.getMaskedEmail());

        // Verify OTP email sent to officer.getEmail()
        assertEquals("officer1@crimesphere.internal", resendEmailService.lastRecipient);
        assertEquals("CrimeSphere AI - Login Verification Code", resendEmailService.lastSubject);
        assertTrue(resendEmailService.lastTextBody.contains("Your CrimeSphere AI verification code is:"));
        assertTrue(resendEmailService.lastTextBody.contains("expires in 5 minutes"));
    }

    @Test
    @DisplayName("2. Invalid password throws BadCredentialsException and dispatches NO OTP")
    void testInvalidPasswordDispatchesNoOtp() {
        LoginRequest request = new LoginRequest("TN-POL-1001", "WrongPassword");
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(activeOfficer));

        assertThrows(BadCredentialsException.class, () -> authService.login(request, httpRequest));
        assertNull(resendEmailService.lastRecipient);
        verify(challengeRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Pending account is blocked and dispatches NO OTP")
    void testPendingAccountBlocked() {
        activeOfficer.setStatus("PENDING_STATION_APPROVAL");
        activeOfficer.setIsActive(false);

        LoginRequest request = new LoginRequest("TN-POL-1001", "Demo@123");
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(activeOfficer));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request, httpRequest));
        assertTrue(ex.getMessage().contains("awaiting Station Head approval"));
        assertNull(resendEmailService.lastRecipient);
    }

    @Test
    @DisplayName("4. Rejected account is blocked and dispatches NO OTP")
    void testRejectedAccountBlocked() {
        activeOfficer.setStatus("REJECTED");
        activeOfficer.setIsActive(false);

        LoginRequest request = new LoginRequest("TN-POL-1001", "Demo@123");
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(activeOfficer));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request, httpRequest));
        assertTrue(ex.getMessage().contains("rejected"));
        assertNull(resendEmailService.lastRecipient);
    }

    @Test
    @DisplayName("5. Suspended account is blocked and dispatches NO OTP")
    void testSuspendedAccountBlocked() {
        activeOfficer.setStatus("SUSPENDED");
        activeOfficer.setIsActive(false);

        LoginRequest request = new LoginRequest("TN-POL-1001", "Demo@123");
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(activeOfficer));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request, httpRequest));
        assertTrue(ex.getMessage().contains("suspended"));
        assertNull(resendEmailService.lastRecipient);
    }

    @Test
    @DisplayName("6. Correct OTP generates standard JWT with intact claims and 1-hour expiration")
    void testCorrectOtpGeneratesStandardJwt() {
        UUID challengeId = UUID.randomUUID();
        String plainOtp = "482910";
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
        LoginResponse response = authService.verifyOtpAndGenerateToken(verifyRequest, httpRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals(3600000L, response.getExpiresIn());
        assertEquals("Bearer", response.getTokenType());

        // Verify Claims in generated JWT
        assertEquals("TN-POL-1001", jwtService.extractPoliceId(response.getToken()));
        assertEquals(activeOfficer.getPoliceId(), response.getOfficer().getPoliceId());
        assertEquals(station.getId().toString(), response.getStation().getId().toString());
        assertEquals(station.getStationCode(), response.getStation().getStationCode());
        assertTrue(challenge.getIsUsed());
    }

    @Test
    @DisplayName("7. Incorrect OTP increments attempt count and throws BadCredentialsException")
    void testIncorrectOtpIncrementsAttempts() {
        UUID challengeId = UUID.randomUUID();
        String plainOtp = "482910";
        String otpHash = passwordEncoder.encode(plainOtp);

        AuthOtpChallenge challenge = new AuthOtpChallenge(
                activeOfficer,
                otpHash,
                OffsetDateTime.now().plusMinutes(5),
                "127.0.0.1",
                "Mozilla/5.0"
        );
        challenge.setId(challengeId);
        challenge.setAttemptCount(1);

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        VerifyOtpRequest verifyRequest = new VerifyOtpRequest(challengeId, "000000");
        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.verifyOtpAndGenerateToken(verifyRequest, httpRequest));

        assertEquals("Invalid verification code.", ex.getMessage());
        assertEquals(2, challenge.getAttemptCount());
        assertFalse(challenge.getIsUsed());
    }

    @Test
    @DisplayName("8. Expired OTP throws expired error")
    void testExpiredOtpThrowsError() {
        UUID challengeId = UUID.randomUUID();
        String plainOtp = "482910";
        String otpHash = passwordEncoder.encode(plainOtp);

        AuthOtpChallenge challenge = new AuthOtpChallenge(
                activeOfficer,
                otpHash,
                OffsetDateTime.now().minusMinutes(1), // Expired 1 min ago
                "127.0.0.1",
                "Mozilla/5.0"
        );
        challenge.setId(challengeId);

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        VerifyOtpRequest verifyRequest = new VerifyOtpRequest(challengeId, plainOtp);
        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.verifyOtpAndGenerateToken(verifyRequest, httpRequest));

        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("9. Already used OTP throws error")
    void testAlreadyUsedOtpThrowsError() {
        UUID challengeId = UUID.randomUUID();
        String plainOtp = "482910";
        String otpHash = passwordEncoder.encode(plainOtp);

        AuthOtpChallenge challenge = new AuthOtpChallenge(
                activeOfficer,
                otpHash,
                OffsetDateTime.now().plusMinutes(5),
                "127.0.0.1",
                "Mozilla/5.0"
        );
        challenge.setId(challengeId);
        challenge.setIsUsed(true);

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        VerifyOtpRequest verifyRequest = new VerifyOtpRequest(challengeId, plainOtp);
        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.verifyOtpAndGenerateToken(verifyRequest, httpRequest));

        assertTrue(ex.getMessage().contains("already been used"));
    }

    @Test
    @DisplayName("10. 5 incorrect attempts invalidates the challenge")
    void testMaxAttemptsInvalidatesChallenge() {
        UUID challengeId = UUID.randomUUID();
        String plainOtp = "482910";
        String otpHash = passwordEncoder.encode(plainOtp);

        AuthOtpChallenge challenge = new AuthOtpChallenge(
                activeOfficer,
                otpHash,
                OffsetDateTime.now().plusMinutes(5),
                "127.0.0.1",
                "Mozilla/5.0"
        );
        challenge.setId(challengeId);
        challenge.setAttemptCount(4); // 5th attempt is next

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

        VerifyOtpRequest verifyRequest = new VerifyOtpRequest(challengeId, "999999");
        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.verifyOtpAndGenerateToken(verifyRequest, httpRequest));

        assertTrue(ex.getMessage().contains("Too many incorrect attempts"));
        assertTrue(challenge.getIsInvalidated());
        assertEquals(5, challenge.getAttemptCount());
    }

    @Test
    @DisplayName("11. Resend rate limit blocks > 3 requests within 10 minutes")
    void testResendRateLimitEnforced() {
        UUID challengeId = UUID.randomUUID();
        AuthOtpChallenge oldChallenge = new AuthOtpChallenge(
                activeOfficer,
                "somehash",
                OffsetDateTime.now().plusMinutes(5),
                "127.0.0.1",
                "Mozilla/5.0"
        );
        oldChallenge.setId(challengeId);

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(oldChallenge));
        // 3 recent challenges already created
        when(challengeRepository.countChallengesCreatedSince(eq(activeOfficer.getId()), any(OffsetDateTime.class))).thenReturn(3L);

        ResendOtpRequest resendRequest = new ResendOtpRequest(challengeId);
        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.resendOtp(resendRequest, httpRequest));

        assertTrue(ex.getMessage().contains("Too many verification code requests"));
    }
}
