package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.*;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import com.crimesphere.backend.service.AuthService;
import com.crimesphere.backend.service.OfficerRegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OfficerRegistrationService registrationService;
    private final PoliceOfficerRepository officerRepository;
    private final ObjectMapper objectMapper;

    public AuthController(AuthService authService,
                          OfficerRegistrationService registrationService,
                          PoliceOfficerRepository officerRepository,
                          ObjectMapper objectMapper) {
        this.authService = authService;
        this.registrationService = registrationService;
        this.officerRepository = officerRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginChallengeResponse> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        LoginChallengeResponse response = authService.login(loginRequest, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<LoginResponse> verifyOtp(@RequestBody VerifyOtpRequest verifyRequest, HttpServletRequest request) {
        LoginResponse response = authService.verifyOtpAndGenerateToken(verifyRequest, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ResendOtpResponse> resendOtp(@RequestBody ResendOtpRequest resendRequest, HttpServletRequest request) {
        ResendOtpResponse response = authService.resendOtp(resendRequest, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> getAuthMe() {
        PoliceOfficer currentOfficer = getCurrentOfficer();
        AuthMeResponse response = authService.getAuthMe(currentOfficer.getPoliceId());
        return ResponseEntity.ok(response);
    }

    /**
     * Submit new officer registration with ID document upload.
     */
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OfficerRegistrationResponse> registerOfficer(
            @RequestPart("data") String dataJson,
            @RequestPart("idDocument") MultipartFile idDocument,
            HttpServletRequest request
    ) throws Exception {
        OfficerRegistrationRequest regRequest = objectMapper.readValue(dataJson, OfficerRegistrationRequest.class);
        OfficerRegistrationResponse response = authService.registerOfficer(regRequest, idDocument, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve all pending officer registrations for the authenticated Station Head's station.
     */
    @GetMapping("/station-registrations/pending")
    public ResponseEntity<List<PendingOfficerRegistrationDto>> getPendingRegistrations() {
        PoliceOfficer currentOfficer = getCurrentOfficer();
        List<PendingOfficerRegistrationDto> pendingList = registrationService.getPendingRegistrationsForStation(currentOfficer);
        return ResponseEntity.ok(pendingList);
    }

    /**
     * Retrieve details for a specific officer registration.
     */
    @GetMapping("/station-registrations/{id}")
    public ResponseEntity<PendingOfficerRegistrationDto> getRegistrationDetail(@PathVariable("id") UUID id, HttpServletRequest request) {
        PoliceOfficer currentOfficer = getCurrentOfficer();
        PendingOfficerRegistrationDto detail = registrationService.getRegistrationDetail(id, currentOfficer, request);
        return ResponseEntity.ok(detail);
    }

    /**
     * View the uploaded officer ID document (strictly authenticated and station isolated).
     */
    @GetMapping("/station-registrations/{id}/document")
    public ResponseEntity<Resource> getOfficerIdDocument(@PathVariable("id") UUID id, HttpServletRequest request) {
        PoliceOfficer currentOfficer = getCurrentOfficer();
        Resource resource = registrationService.getOfficerIdDocumentResource(id, currentOfficer, request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"officer_id_document\"")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .body(resource);
    }

    /**
     * Approve an officer registration (Station Head only, station isolated).
     */
    @PatchMapping("/station-registrations/{id}/approve")
    public ResponseEntity<PendingOfficerRegistrationDto> approveRegistration(
            @PathVariable("id") UUID id,
            HttpServletRequest request
    ) {
        PoliceOfficer currentOfficer = getCurrentOfficer();
        PendingOfficerRegistrationDto approved = registrationService.approveRegistration(id, currentOfficer, request);
        return ResponseEntity.ok(approved);
    }

    /**
     * Reject an officer registration with reason (Station Head only, station isolated).
     */
    @PatchMapping("/station-registrations/{id}/reject")
    public ResponseEntity<PendingOfficerRegistrationDto> rejectRegistration(
            @PathVariable("id") UUID id,
            @RequestBody OfficerReviewDecisionRequest decisionRequest,
            HttpServletRequest request
    ) {
        PoliceOfficer currentOfficer = getCurrentOfficer();
        String reason = decisionRequest != null ? decisionRequest.getRejectionReason() : null;
        PendingOfficerRegistrationDto rejected = registrationService.rejectRegistration(id, reason, currentOfficer, request);
        return ResponseEntity.ok(rejected);
    }

    private PoliceOfficer getCurrentOfficer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        String policeId = authentication.getName();
        return officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated officer not found"));
    }
}
