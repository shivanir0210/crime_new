package com.crimesphere.backend.service;

import com.crimesphere.backend.entity.AuthOtpChallenge;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.exception.InvalidOtpException;
import com.crimesphere.backend.repository.AuthOtpChallengeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_RESENDS_IN_WINDOW = 3;
    private static final int RESEND_WINDOW_MINUTES = 10;

    private final AuthOtpChallengeRepository challengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResendEmailService resendEmailService;
    private final SecureRandom secureRandom = new SecureRandom();

    private boolean demoEmailEnabled = false;
    private String demoRecipientEmail = "";

    public OtpService(AuthOtpChallengeRepository challengeRepository,
                      PasswordEncoder passwordEncoder,
                      ResendEmailService resendEmailService) {
        this(challengeRepository, passwordEncoder, resendEmailService, false, "");
    }

    @Autowired
    public OtpService(AuthOtpChallengeRepository challengeRepository,
                      PasswordEncoder passwordEncoder,
                      ResendEmailService resendEmailService,
                      @Value("${mfa.demo-email.enabled:${MFA_DEMO_EMAIL_ENABLED:false}}") boolean demoEmailEnabled,
                      @Value("${mfa.demo-recipient-email:${MFA_DEMO_RECIPIENT_EMAIL:}}") String demoRecipientEmail) {
        this.challengeRepository = challengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.resendEmailService = resendEmailService;
        this.demoEmailEnabled = demoEmailEnabled;
        this.demoRecipientEmail = demoRecipientEmail != null ? demoRecipientEmail.trim() : "";
    }

    public boolean isDemoEmailEnabled() {
        return demoEmailEnabled;
    }

    public void setDemoEmailEnabled(boolean demoEmailEnabled) {
        this.demoEmailEnabled = demoEmailEnabled;
    }

    public String getDemoRecipientEmail() {
        return demoRecipientEmail;
    }

    public void setDemoRecipientEmail(String demoRecipientEmail) {
        this.demoRecipientEmail = demoRecipientEmail;
    }

    /**
     * Resolves the effective OTP delivery recipient email address.
     * In demo/sandbox environments with mfa.demo-email.enabled=true, routes OTP to the designated demo recipient.
     * In production/default mode, routes OTP to the officer's registered email address.
     */
    public String resolveOtpDeliveryEmail(PoliceOfficer officer) {
        if (demoEmailEnabled && demoRecipientEmail != null && !demoRecipientEmail.isBlank()) {
            return demoRecipientEmail.trim();
        }
        return officer != null && officer.getEmail() != null ? officer.getEmail().trim() : null;
    }

    /**
     * Returns the safely masked representation of the resolved OTP delivery email.
     */
    public String getMaskedDeliveryEmail(PoliceOfficer officer) {
        String resolvedEmail = resolveOtpDeliveryEmail(officer);
        return maskEmail(resolvedEmail);
    }

    /**
     * Generates a secure 6-digit OTP, creates a hashed challenge record, and dispatches the code via email.
     */
    @Transactional
    public AuthOtpChallenge createAndSendOtpChallenge(PoliceOfficer officer, String ipAddress, String userAgent) {
        if (officer == null) {
            throw new IllegalArgumentException("Officer cannot be null");
        }

        // Check rate limiting: max 3 challenges in 10 minutes
        OffsetDateTime windowStart = OffsetDateTime.now().minusMinutes(RESEND_WINDOW_MINUTES);
        long recentCount = challengeRepository.countChallengesCreatedSince(officer.getId(), windowStart);
        if (recentCount >= MAX_RESENDS_IN_WINDOW) {
            logger.warn("[OTP] Rate limit exceeded for officer {}: {} challenges in last {} mins",
                    officer.getPoliceId(), recentCount, RESEND_WINDOW_MINUTES);
            throw new InvalidOtpException("Too many verification code requests. Please try again later.");
        }

        // Invalidate any existing active challenges for this officer
        invalidateActiveChallenges(officer.getId());

        // Generate cryptographically secure 6-digit OTP
        int otpNumber = 100000 + secureRandom.nextInt(900000);
        String plainOtp = String.valueOf(otpNumber);

        // Hash the OTP with BCrypt (never store plaintext)
        String otpHash = passwordEncoder.encode(plainOtp);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        AuthOtpChallenge challenge = new AuthOtpChallenge(officer, otpHash, expiresAt, ipAddress, userAgent);
        challenge.setResendCount((int) recentCount);
        AuthOtpChallenge savedChallenge = challengeRepository.save(challenge);

        String deliveryEmail = resolveOtpDeliveryEmail(officer);
        logger.info("[OTP] Challenge created: {}", savedChallenge.getId());
        logger.info("[OTP] Recipient: {}", maskEmail(deliveryEmail));
        logger.info("[OTP] Email provider: RESEND");

        // Dispatch OTP via Resend to resolved delivery email
        sendOtpEmail(deliveryEmail, plainOtp, officer.getPoliceId());

        return savedChallenge;
    }

    /**
     * Validates an OTP against a challenge ID.
     */
    @Transactional
    public PoliceOfficer verifyOtp(UUID challengeId, String otpInput) {
        if (challengeId == null || otpInput == null || otpInput.trim().isBlank()) {
            logger.warn("[OTP] OTP verification: FAILED (missing challenge ID or code)");
            throw new InvalidOtpException("Invalid verification code.");
        }

        AuthOtpChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> {
                    logger.warn("[OTP] OTP verification: FAILED (challenge {} not found)", challengeId);
                    return new InvalidOtpException("Invalid or expired verification session. Please log in again.");
                });

        if (Boolean.TRUE.equals(challenge.getIsUsed())) {
            logger.warn("[OTP] OTP verification: FAILED (challenge {} already used)", challengeId);
            throw new InvalidOtpException("Verification code has already been used. Please request a new code.");
        }

        if (Boolean.TRUE.equals(challenge.getIsInvalidated())) {
            logger.warn("[OTP] OTP verification: FAILED (challenge {} invalidated)", challengeId);
            throw new InvalidOtpException("Verification session has been invalidated. Please log in again.");
        }

        // Check expiration
        if (challenge.getExpiresAt().isBefore(OffsetDateTime.now())) {
            challenge.setIsInvalidated(true);
            challenge.setInvalidatedAt(OffsetDateTime.now());
            challengeRepository.save(challenge);
            logger.warn("[OTP] OTP verification: FAILED (challenge {} expired)", challengeId);
            throw new InvalidOtpException("Verification code expired. Please request a new code.");
        }

        // Check maximum attempts
        if (challenge.getAttemptCount() >= MAX_ATTEMPTS) {
            challenge.setIsInvalidated(true);
            challenge.setInvalidatedAt(OffsetDateTime.now());
            challengeRepository.save(challenge);
            logger.warn("[OTP] OTP verification: FAILED (challenge {} exceeded max attempts)", challengeId);
            throw new InvalidOtpException("Too many incorrect attempts. Please request a new code.");
        }

        // Securely compare OTP
        boolean matches = passwordEncoder.matches(otpInput.trim(), challenge.getOtpHash());
        if (!matches) {
            int updatedAttempts = challenge.getAttemptCount() + 1;
            challenge.setAttemptCount(updatedAttempts);
            if (updatedAttempts >= MAX_ATTEMPTS) {
                challenge.setIsInvalidated(true);
                challenge.setInvalidatedAt(OffsetDateTime.now());
                challengeRepository.save(challenge);
                logger.warn("[OTP] OTP verification: FAILED (challenge {} reached max attempts)", challengeId);
                throw new InvalidOtpException("Too many incorrect attempts. Please request a new code.");
            }
            challengeRepository.save(challenge);
            logger.warn("[OTP] OTP verification: FAILED (invalid code for challenge {}, attempt {}/{})",
                    challengeId, updatedAttempts, MAX_ATTEMPTS);
            throw new InvalidOtpException("Invalid verification code.");
        }

        // Successfully verified
        challenge.setIsUsed(true);
        challengeRepository.save(challenge);

        logger.info("[OTP] OTP verification: SUCCESS for officer: {}", challenge.getOfficer().getPoliceId());
        return challenge.getOfficer();
    }

    /**
     * Resends a new OTP for an existing challenge session while enforcing rate limits.
     */
    @Transactional
    public AuthOtpChallenge resendOtp(UUID challengeId, String ipAddress, String userAgent) {
        if (challengeId == null) {
            throw new InvalidOtpException("Challenge ID is required.");
        }

        AuthOtpChallenge oldChallenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new InvalidOtpException("Invalid verification session. Please log in again."));

        PoliceOfficer officer = oldChallenge.getOfficer();
        return createAndSendOtpChallenge(officer, ipAddress, userAgent);
    }

    private void invalidateActiveChallenges(UUID officerId) {
        List<AuthOtpChallenge> activeList = challengeRepository.findActiveByOfficerId(officerId);
        OffsetDateTime now = OffsetDateTime.now();
        for (AuthOtpChallenge c : activeList) {
            c.setIsInvalidated(true);
            c.setInvalidatedAt(now);
        }
        if (!activeList.isEmpty()) {
            challengeRepository.saveAll(activeList);
        }
    }

    private void sendOtpEmail(String recipientEmail, String plainOtp, String policeId) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            logger.warn("[OTP] Officer {} has no configured OTP delivery email. Email dispatch skipped.",
                    policeId);
            return;
        }

        String subject = "CrimeSphere AI - Login Verification Code";
        String textBody = resendEmailService.buildLoginOtpEmailBody(plainOtp);
        String htmlBody = resendEmailService.buildLoginOtpEmailHtml(plainOtp);

        try {
            NotificationDispatchResult result = resendEmailService.sendEmail(recipientEmail.trim(), subject, textBody, htmlBody);
            if (result.isSuccess()) {
                logger.info("[OTP] Email send result: SUCCESS");
                logger.info("[OTP] Provider message ID: {}", result.getExternalMessageId());
            } else {
                logger.warn("[OTP] Email send result: FAILED | Recipient: {} | Reason: {}",
                        maskEmail(recipientEmail), result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.warn("[OTP] Email send exception for officer {}: {}", policeId, e.getMessage());
        }
    }

    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@crimesphere.internal";
        }
        int atIndex = email.indexOf('@');
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (username.length() <= 2) {
            return username + domain;
        }
        return username.substring(0, 2) + "***" + domain;
    }
}
