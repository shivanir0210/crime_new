package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.*;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.NotificationRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import com.crimesphere.backend.repository.PoliceStationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final PoliceOfficerRepository officerRepository;
    private final PoliceStationRepository stationRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final ResendEmailService resendEmailService;
    private final StorageService storageService;
    private final OfficerOcrService ocrService;
    private final OtpService otpService;

    public AuthService(PoliceOfficerRepository officerRepository,
                       PoliceStationRepository stationRepository,
                       AuditLogRepository auditLogRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EmailService emailService,
                       ResendEmailService resendEmailService,
                       StorageService storageService,
                       OfficerOcrService ocrService,
                       OtpService otpService) {
        this(officerRepository, stationRepository, auditLogRepository, null,
                passwordEncoder, jwtService, emailService, resendEmailService, storageService, ocrService, otpService);
    }

    @Autowired
    public AuthService(PoliceOfficerRepository officerRepository,
                       PoliceStationRepository stationRepository,
                       AuditLogRepository auditLogRepository,
                       NotificationRepository notificationRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EmailService emailService,
                       ResendEmailService resendEmailService,
                       StorageService storageService,
                       OfficerOcrService ocrService,
                       OtpService otpService) {
        this.officerRepository = officerRepository;
        this.stationRepository = stationRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.resendEmailService = resendEmailService;
        this.storageService = storageService;
        this.ocrService = ocrService;
        this.otpService = otpService;
    }

    /**
     * Submits a trusted officer registration with ID document upload, runs OCR verification,
     * persists pending officer account (is_active = false), and alerts the Station Head.
     */
    @Transactional
    public OfficerRegistrationResponse registerOfficer(OfficerRegistrationRequest request,
                                                      MultipartFile idDocument,
                                                      HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "Unknown";

        // 1. Validation of required inputs
        if (request == null) {
            throw new IllegalArgumentException("Registration payload is required.");
        }
        if (request.getFullName() == null || request.getFullName().trim().isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (request.getPoliceId() == null || request.getPoliceId().trim().isBlank()) {
            throw new IllegalArgumentException("Police ID is required.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        if (request.getConfirmPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (request.getStationId() == null) {
            throw new IllegalArgumentException("Police station selection is required.");
        }
        if (idDocument == null || idDocument.isEmpty()) {
            throw new IllegalArgumentException("Official Police ID document upload is required.");
        }

        String policeId = request.getPoliceId().trim().toUpperCase();

        // 2. Check duplicate Police ID
        if (officerRepository.findByPoliceId(policeId).isPresent()) {
            throw new IllegalArgumentException("An officer with Police ID '" + policeId + "' is already registered.");
        }

        // 3. Check duplicate Email (if provided)
        if (request.getEmail() != null && !request.getEmail().trim().isBlank()) {
            String email = request.getEmail().trim().toLowerCase();
            boolean emailExists = officerRepository.findAll().stream()
                    .anyMatch(o -> email.equalsIgnoreCase(o.getEmail()) && !"STATION_HEAD".equalsIgnoreCase(o.getRole()) && !policeId.equalsIgnoreCase(o.getPoliceId()));
            if (emailExists) {
                throw new IllegalArgumentException("An officer with email '" + email + "' is already registered.");
            }
        }

        // 4. Validate selected station
        PoliceStation station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Selected police station not found with ID: " + request.getStationId()));

        // 5. Generate unique officer ID
        UUID officerId = UUID.randomUUID();

        // 6. Store ID Document securely
        String storedDocPath;
        try {
            storedDocPath = storageService.storeOfficerIdDocument(officerId, idDocument);
        } catch (Exception e) {
            logger.error("Failed to store ID document during registration for PID {}: {}", policeId, e.getMessage());
            throw new RuntimeException("Failed to securely store ID document: " + e.getMessage(), e);
        }

        // 7. Run OCR extraction & comparison
        OfficerOcrService.OcrExtractionResult ocrResult = ocrService.processAndVerifyIdDocument(idDocument, request, station);

        // 8. Create PoliceOfficer entity with inactive pending status
        PoliceOfficer newOfficer = new PoliceOfficer();
        newOfficer.setId(officerId);
        newOfficer.setPoliceId(policeId);
        newOfficer.setFullName(request.getFullName().trim());
        newOfficer.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        newOfficer.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        newOfficer.setStation(station);
        newOfficer.setRole(request.getRole() != null && !request.getRole().isBlank() ? request.getRole().trim() : "OFFICER");
        newOfficer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newOfficer.setIsActive(false); // CRITICAL: Never activate automatically
        newOfficer.setStatus("PENDING_STATION_APPROVAL");
        newOfficer.setIdDocumentPath(storedDocPath);
        newOfficer.setOcrStatus(ocrResult.getOcrStatus());
        newOfficer.setOcrDetectedName(ocrResult.getDetectedName());
        newOfficer.setOcrDetectedPoliceId(ocrResult.getDetectedPoliceId());
        newOfficer.setOcrDetectedStation(ocrResult.getDetectedStation());
        newOfficer.setOcrDetectedRole(ocrResult.getDetectedRole());
        newOfficer.setOcrRawText(ocrResult.getRawText());

        PoliceOfficer savedOfficer = officerRepository.save(newOfficer);

        // 9. Audit Logging
        recordAuditLog(savedOfficer, "REGISTRATION_SUBMITTED", "SUCCESS", clientIp, userAgent);
        recordAuditLog(savedOfficer, "ID_DOCUMENT_UPLOADED", "SUCCESS", clientIp, userAgent);
        if ("PASSED".equalsIgnoreCase(ocrResult.getOcrStatus())) {
            recordAuditLog(savedOfficer, "OCR_COMPLETED", "SUCCESS", clientIp, userAgent);
        } else {
            recordAuditLog(savedOfficer, "OCR_REVIEW_REQUIRED", "PENDING_REVIEW", clientIp, userAgent);
        }

        // 10. Notify Station Head via in-app notification & Resend email
        notifyStationHead(savedOfficer, station, ocrResult.getOcrStatus());

        return new OfficerRegistrationResponse(
                savedOfficer.getId(),
                savedOfficer.getPoliceId(),
                savedOfficer.getFullName(),
                savedOfficer.getEmail(),
                savedOfficer.getPhone(),
                savedOfficer.getRole(),
                station.getStationName(),
                station.getStationCode(),
                savedOfficer.getStatus(),
                savedOfficer.getIsActive(),
                savedOfficer.getOcrStatus(),
                ocrResult.toDto(),
                "Registration submitted successfully. Your account will become active after Station Head approval.",
                savedOfficer.getCreatedAt()
        );
    }

    private void notifyStationHead(PoliceOfficer officer, PoliceStation station, String ocrStatus) {
        if (station == null) {
            return;
        }

        // 1. Create in-app notification for Station Head(s) of this station
        try {
            List<PoliceOfficer> stationHeads = officerRepository.findByStationId(station.getId()).stream()
                    .filter(o -> "STATION_HEAD".equalsIgnoreCase(o.getRole()) && Boolean.TRUE.equals(o.getIsActive()))
                    .toList();

            for (PoliceOfficer head : stationHeads) {
                Notification notif = new Notification();
                notif.setRecipientOfficer(head);
                notif.setChannel(NotificationChannel.IN_APP);
                notif.setNotificationType(NotificationType.NEW_OFFICER_REGISTRATION);
                notif.setSubject("NEW OFFICER REGISTRATION");
                notif.setMessage("Officer " + officer.getFullName() + " (Police ID: " + officer.getPoliceId() +
                        ") has registered with " + station.getStationName() + " (" + officer.getRole() + "). Station Head review and authorization required.");
                notif.setStatus(NotificationStatus.SENT);
                notif.setSeverity("WARNING");
                notif.setReferenceType("OFFICER_REGISTRATION");
                notif.setReferenceId(officer.getId());
                notif.setSentAt(OffsetDateTime.now());
                notificationRepository.save(notif);
            }
        } catch (Exception e) {
            logger.warn("Failed to create in-app notification for Station Head(s) at station {}: {}",
                    station.getStationCode(), e.getMessage());
        }

        // 2. Dispatch Resend Email to Station Head
        if (station.getHeadEmail() == null || station.getHeadEmail().isBlank()) {
            logger.warn("Station Head email is not configured for station {}. Email alert skipped.", station.getStationCode());
            return;
        }

        String headEmail = station.getHeadEmail().trim();
        String subject = "New Officer Registration Requires Approval";
        String reviewUrl = "http://localhost:5173/station-head/registrations/" + officer.getId();

        String textBody = resendEmailService.buildOfficerRegistrationAlertBody(
                officer.getFullName(),
                officer.getPoliceId(),
                station.getStationName(),
                officer.getRole(),
                ocrStatus,
                reviewUrl
        );

        String htmlBody = resendEmailService.buildOfficerRegistrationAlertHtml(
                officer.getFullName(),
                officer.getPoliceId(),
                station.getStationName(),
                officer.getRole(),
                ocrStatus,
                reviewUrl
        );

        try {
            resendEmailService.sendEmail(headEmail, subject, textBody, htmlBody);
            recordAuditLog(officer, "STATION_HEAD_NOTIFIED", "SUCCESS",
                    "System",
                    "Station Head notified at " + headEmail + " for Registration ID: " + officer.getId() + ", Station: " + station.getStationCode());
            logger.info("Station Head notified for new registration of officer: {} (Station: {})",
                    officer.getPoliceId(), station.getStationCode());
        } catch (Exception e) {
            logger.warn("Failed to dispatch Resend email to Station Head {}: {}", headEmail, e.getMessage());
        }
    }

    @Transactional
    public LoginChallengeResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "Unknown";

        if (request == null || request.getPoliceId() == null || request.getPassword() == null) {
            recordAuditLog(null, "LOGIN_FAILED", "FAILED", clientIp, userAgent);
            throw new BadCredentialsException("Invalid Police ID or password");
        }

        Optional<PoliceOfficer> officerOpt = officerRepository.findByPoliceId(request.getPoliceId().trim());

        if (officerOpt.isEmpty()) {
            recordAuditLog(null, "LOGIN_FAILED", "FAILED", clientIp, userAgent);
            throw new BadCredentialsException("Invalid Police ID or password");
        }

        PoliceOfficer officer = officerOpt.get();

        // 1. Check password
        if (officer.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), officer.getPasswordHash())) {
            recordAuditLog(officer, "LOGIN_FAILED", "FAILED", clientIp, userAgent);
            throw new BadCredentialsException("Invalid Police ID or password");
        }

        // 2. Check Account Status Lifecycle
        String status = officer.getStatus() != null ? officer.getStatus().toUpperCase() : "ACTIVE";

        if ("PENDING_STATION_APPROVAL".equals(status) || "PENDING_VERIFICATION".equals(status)) {
            recordAuditLog(officer, "LOGIN_BLOCKED_PENDING_APPROVAL", "BLOCKED", clientIp, userAgent);
            if ("PENDING_VERIFICATION".equals(status)) {
                throw new BadCredentialsException("Your registration is still being verified.");
            }
            throw new BadCredentialsException("Your registration is awaiting Station Head approval.");
        }

        if ("REJECTED".equals(status)) {
            recordAuditLog(officer, "LOGIN_FAILED", "BLOCKED", clientIp, userAgent);
            throw new BadCredentialsException("Your registration was rejected. Please contact your station administrator.");
        }

        if ("SUSPENDED".equals(status)) {
            recordAuditLog(officer, "LOGIN_FAILED", "BLOCKED", clientIp, userAgent);
            throw new BadCredentialsException("Your account is currently suspended.");
        }

        if (!Boolean.TRUE.equals(officer.getIsActive())) {
            recordAuditLog(officer, "LOGIN_BLOCKED_PENDING_APPROVAL", "BLOCKED", clientIp, userAgent);
            throw new BadCredentialsException("Your registration is awaiting Station Head approval.");
        }

        // 3. Initiate OTP generation & dispatch
        AuthOtpChallenge challenge;
        try {
            challenge = otpService.createAndSendOtpChallenge(officer, clientIp, userAgent);
            recordAuditLog(officer, "LOGIN_OTP_REQUESTED", "SUCCESS", clientIp, userAgent);
        } catch (BadCredentialsException e) {
            recordAuditLog(officer, "LOGIN_OTP_RATE_LIMITED", "FAILED", clientIp, userAgent);
            throw e;
        }

        String maskedEmail = otpService.getMaskedDeliveryEmail(officer);
        return new LoginChallengeResponse(true, challenge.getId(), maskedEmail, "Verification code sent to " + maskedEmail);
    }

    @Transactional
    public LoginResponse verifyOtpAndGenerateToken(VerifyOtpRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "Unknown";

        if (request == null || request.getChallengeId() == null || request.getOtp() == null) {
            recordAuditLog(null, "LOGIN_OTP_FAILED", "FAILED", clientIp, userAgent);
            throw new BadCredentialsException("Invalid verification code.");
        }

        PoliceOfficer officer;
        try {
            officer = otpService.verifyOtp(request.getChallengeId(), request.getOtp());
        } catch (BadCredentialsException e) {
            recordAuditLog(null, "LOGIN_OTP_FAILED", "FAILED", clientIp, userAgent);
            throw e;
        }

        // Re-check officer lifecycle status
        if (!Boolean.TRUE.equals(officer.getIsActive())) {
            recordAuditLog(officer, "LOGIN_BLOCKED_PENDING_APPROVAL", "BLOCKED", clientIp, userAgent);
            throw new BadCredentialsException("Your registration is awaiting Station Head approval.");
        }

        // Generate JWT Token - ONLY NOW after successful OTP verification
        String token = jwtService.generateToken(officer);
        long expiresIn = jwtService.getExpirationMs();

        // Audit Logging
        recordAuditLog(officer, "LOGIN_OTP_VERIFIED", "SUCCESS", clientIp, userAgent);
        recordAuditLog(officer, "LOGIN_SUCCESS", "SUCCESS", clientIp, userAgent);

        // Send non-blocking Station Head email notification
        if (emailService != null) {
            emailService.sendStationHeadLoginNotification(officer, officer.getStation());
        }

        // Prepare response DTOs
        OfficerDto officerDto = new OfficerDto(officer);
        PoliceStationDto stationDto = officer.getStation() != null ? new PoliceStationDto(officer.getStation()) : null;

        return new LoginResponse(token, expiresIn, officerDto, stationDto);
    }

    @Transactional
    public ResendOtpResponse resendOtp(ResendOtpRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "Unknown";

        if (request == null || request.getChallengeId() == null) {
            throw new BadCredentialsException("Challenge ID is required.");
        }

        AuthOtpChallenge newChallenge;
        try {
            newChallenge = otpService.resendOtp(request.getChallengeId(), clientIp, userAgent);
            recordAuditLog(newChallenge.getOfficer(), "LOGIN_OTP_REQUESTED", "SUCCESS", clientIp, userAgent);
        } catch (BadCredentialsException e) {
            recordAuditLog(null, "LOGIN_OTP_RATE_LIMITED", "FAILED", clientIp, userAgent);
            throw e;
        }

        String maskedEmail = otpService.getMaskedDeliveryEmail(newChallenge.getOfficer());
        return new ResendOtpResponse(true, newChallenge.getId(), maskedEmail, "New verification code dispatched to " + maskedEmail);
    }

    public AuthMeResponse getAuthMe(String policeId) {
        PoliceOfficer officer = officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new BadCredentialsException("Police Officer not found"));

        PoliceStationDto stationDto = officer.getStation() != null ? new PoliceStationDto(officer.getStation()) : null;
        String officerId = officer.getId() != null ? officer.getId().toString() : null;
        String stationId = officer.getStation() != null && officer.getStation().getId() != null ? officer.getStation().getId().toString() : null;

        return new AuthMeResponse(
                officerId,
                officer.getPoliceId(),
                officer.getFullName(),
                officer.getEmail(),
                officer.getPhone(),
                officer.getRole(),
                stationId,
                stationDto
        );
    }

    private void recordAuditLog(PoliceOfficer officer, String action, String status, String ipAddress, String userAgent) {
        try {
            String safeIp = ipAddress;
            if (safeIp != null && safeIp.length() > 95) {
                safeIp = safeIp.substring(0, 95);
            }
            AuditLog log = new AuditLog(null, officer, action, status, safeIp, userAgent);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to record audit log for action {}: {}", action, e.getMessage());
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "Unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "Unknown";
    }
}
