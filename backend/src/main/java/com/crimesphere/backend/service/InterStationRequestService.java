package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.interstation.CreateInterStationRequestDto;
import com.crimesphere.backend.dto.interstation.InterStationRequestResponseDto;
import com.crimesphere.backend.dto.interstation.RespondInterStationRequestDto;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class InterStationRequestService {

    private static final Logger logger = LoggerFactory.getLogger(InterStationRequestService.class);

    private final InterStationRequestRepository requestRepository;
    private final PoliceStationRepository stationRepository;
    private final PoliceOfficerRepository officerRepository;
    private final CaseRepository caseRepository;
    private final VictimRepository victimRepository;
    private final AuditLogRepository auditLogRepository;
    private final ResendEmailService resendEmailService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public InterStationRequestService(InterStationRequestRepository requestRepository,
                                      PoliceStationRepository stationRepository,
                                      PoliceOfficerRepository officerRepository,
                                      CaseRepository caseRepository,
                                      VictimRepository victimRepository,
                                      AuditLogRepository auditLogRepository,
                                      ResendEmailService resendEmailService,
                                      NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.stationRepository = stationRepository;
        this.officerRepository = officerRepository;
        this.caseRepository = caseRepository;
        this.victimRepository = victimRepository;
        this.auditLogRepository = auditLogRepository;
        this.resendEmailService = resendEmailService;
        this.notificationService = notificationService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Creates and dispatches an inter-station coordination request.
     * Enforces station authorization, loads dynamic head_email, dispatches Resend email,
     * tracks delivery status, and records audit logs.
     */
    @Transactional
    public InterStationRequestResponseDto createRequest(CreateInterStationRequestDto dto, String policeId, HttpServletRequest httpRequest) {
        if (dto == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }
        if (dto.getCaseId() == null) {
            throw new IllegalArgumentException("Case ID is required");
        }
        if (dto.getDestinationStationId() == null) {
            throw new IllegalArgumentException("Destination police station is required");
        }
        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Reason / request message is required");
        }

        boolean hasSharedInfo = Boolean.TRUE.equals(dto.getShareCaseReference()) ||
                                Boolean.TRUE.equals(dto.getShareVictimDetails()) ||
                                Boolean.TRUE.equals(dto.getShareIncidentSummary()) ||
                                Boolean.TRUE.equals(dto.getShareEvidence()) ||
                                Boolean.TRUE.equals(dto.getShareWitnessInfo());
        if (!hasSharedInfo) {
            throw new IllegalArgumentException("At least one information-sharing option must be selected");
        }

        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        PoliceStation sourceStation = officer.getStation();
        if (sourceStation == null) {
            throw new IllegalStateException("Authenticated police officer has no assigned police station");
        }

        // Authorize parent case within officer's source station
        Case parentCase = caseRepository.findByIdAndStationId(dto.getCaseId(), sourceStation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Case not found or not authorized for your station"));

        // Validate victim if provided
        Victim victim = null;
        if (dto.getVictimId() != null) {
            victim = victimRepository.findByIdAndCaseEntityId(dto.getVictimId(), parentCase.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Victim not found in this case: " + dto.getVictimId()));
        }

        // Validate destination station
        PoliceStation destinationStation = stationRepository.findById(dto.getDestinationStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination police station not found: " + dto.getDestinationStationId()));

        if (destinationStation.getId().equals(sourceStation.getId())) {
            throw new IllegalArgumentException("Destination station cannot be the same as the source station");
        }

        // Compile shared information metadata list
        List<String> sharedItems = new ArrayList<>();
        Map<String, Object> snapshotMap = new LinkedHashMap<>();

        if (Boolean.TRUE.equals(dto.getShareCaseReference())) {
            sharedItems.add("Case Reference (" + parentCase.getCaseNumber() + " - " + parentCase.getTitle() + ")");
            snapshotMap.put("caseNumber", parentCase.getCaseNumber());
            snapshotMap.put("caseTitle", parentCase.getTitle());
            snapshotMap.put("casePriority", parentCase.getPriority());
        }

        if (Boolean.TRUE.equals(dto.getShareVictimDetails()) && victim != null) {
            sharedItems.add("Victim Details (" + victim.getFullName() + ")");
            Map<String, Object> vMap = new LinkedHashMap<>();
            vMap.put("fullName", victim.getFullName());
            vMap.put("age", victim.getAge());
            vMap.put("gender", victim.getGender());
            vMap.put("address", victim.getAddress());
            vMap.put("phone", victim.getPhone());
            vMap.put("status", victim.getStatus());
            snapshotMap.put("victim", vMap);
        }

        if (Boolean.TRUE.equals(dto.getShareIncidentSummary())) {
            sharedItems.add("Incident Summary");
            snapshotMap.put("incidentDescription", parentCase.getDescription());
        }

        if (Boolean.TRUE.equals(dto.getShareEvidence())) {
            sharedItems.add("Evidence Catalog Overview");
            snapshotMap.put("evidenceShared", true);
        }

        if (Boolean.TRUE.equals(dto.getShareWitnessInfo())) {
            sharedItems.add("Witness Information Profile");
            snapshotMap.put("witnessInfoShared", true);
        }

        String snapshotJson = null;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshotMap);
        } catch (Exception ignored) {
        }

        InterStationRequest requestEntity = new InterStationRequest();
        requestEntity.setCaseEntity(parentCase);
        requestEntity.setVictim(victim);
        requestEntity.setSourceStation(sourceStation);
        requestEntity.setDestinationStation(destinationStation);
        requestEntity.setRequestedByOfficer(officer);
        requestEntity.setReason(dto.getReason().trim());
        requestEntity.setShareCaseReference(dto.getShareCaseReference());
        requestEntity.setShareVictimDetails(dto.getShareVictimDetails());
        requestEntity.setShareIncidentSummary(dto.getShareIncidentSummary());
        requestEntity.setShareEvidence(dto.getShareEvidence());
        requestEntity.setShareWitnessInfo(dto.getShareWitnessInfo());
        requestEntity.setSharedDataSnapshot(snapshotJson);
        requestEntity.setStatus("SENT");
        requestEntity.setEmailDeliveryStatus("PENDING");

        InterStationRequest saved = requestRepository.save(requestEntity);

        // Record creation audit
        recordAuditLog(officer, "REQUEST_CREATED", "SUCCESS", httpRequest);

        // Dispatch Resend Email to destination station head
        String headEmail = destinationStation.getHeadEmail();
        if (headEmail == null || headEmail.isBlank()) {
            logger.warn("Destination station {} has no head_email configured. Resend email delivery skipped.", destinationStation.getStationCode());
            saved.setEmailDeliveryStatus("FAILED");
            saved.setEmailDeliveryError("Destination station head email is not configured.");
            saved.setStatus("DELIVERY_FAILED");
            saved = requestRepository.save(saved);
            recordAuditLog(officer, "REQUEST_EMAIL_FAILED", "FAILED: Destination head email not configured", httpRequest);
        } else {
            String subject = "Inter-Station Coordination Request - Case " + parentCase.getCaseNumber();
            String victimName = (victim != null && Boolean.TRUE.equals(dto.getShareVictimDetails())) ? victim.getFullName() : null;

            String textBody = resendEmailService.buildInterStationCoordinationBody(
                    sourceStation.getStationName() + " (" + sourceStation.getStationCode() + ")",
                    destinationStation.getStationName() + " (" + destinationStation.getStationCode() + ")",
                    parentCase.getCaseNumber() + " - " + parentCase.getTitle(),
                    victimName,
                    dto.getReason().trim(),
                    sharedItems,
                    "Please access CrimeSphere AI command center to review, accept, or respond to this coordination inquiry."
            );

            String htmlBody = resendEmailService.buildInterStationCoordinationHtml(
                    sourceStation.getStationName(),
                    destinationStation.getStationName(),
                    parentCase.getCaseNumber(),
                    victimName,
                    dto.getReason().trim(),
                    sharedItems,
                    "Please log into CrimeSphere AI to review, accept, or respond to this coordination request."
            );

            NotificationDispatchResult result = resendEmailService.sendEmail(headEmail.trim(), subject, textBody, htmlBody);
            if (result.isSuccess()) {
                saved.setEmailDeliveryStatus("SENT");
                saved.setProviderMessageId(result.getExternalMessageId());
                saved.setStatus("SENT");
                saved = requestRepository.save(saved);
                recordAuditLog(officer, "REQUEST_EMAIL_SENT", "SENT to " + headEmail, httpRequest);
            } else {
                saved.setEmailDeliveryStatus("FAILED");
                saved.setEmailDeliveryError(result.getErrorMessage());
                saved.setStatus("DELIVERY_FAILED");
                saved = requestRepository.save(saved);
                recordAuditLog(officer, "REQUEST_EMAIL_FAILED", "FAILED: " + result.getErrorMessage(), httpRequest);
            }
        }

        // Dispatch In-App / System Notification
        if (notificationService != null) {
            try {
                notificationService.dispatchCaseNotification(
                        parentCase,
                        NotificationType.CASE_ALERT,
                        "Inter-Station Request Dispatched: " + parentCase.getCaseNumber(),
                        String.format("Inter-station coordination request sent to %s for case %s.",
                                destinationStation.getStationName(), parentCase.getCaseNumber()),
                        "Track coordination response in Inter-Station Requests dashboard.",
                        officer
                );
            } catch (Exception e) {
                logger.warn("Failed to dispatch in-app notification for inter-station request: {}", e.getMessage());
            }
        }

        return new InterStationRequestResponseDto(saved);
    }

    /**
     * Lists sent requests created by the authenticated officer's station.
     */
    @Transactional(readOnly = true)
    public List<InterStationRequestResponseDto> getSentRequests(String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        PoliceStation station = officer.getStation();
        if (station == null) return Collections.emptyList();

        return requestRepository.findBySourceStationIdOrderByCreatedAtDesc(station.getId())
                .stream()
                .map(InterStationRequestResponseDto::new)
                .toList();
    }

    /**
     * Lists incoming requests sent to the authenticated officer's station.
     */
    @Transactional(readOnly = true)
    public List<InterStationRequestResponseDto> getReceivedRequests(String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        PoliceStation station = officer.getStation();
        if (station == null) return Collections.emptyList();

        return requestRepository.findByDestinationStationIdOrderByCreatedAtDesc(station.getId())
                .stream()
                .map(InterStationRequestResponseDto::new)
                .toList();
    }

    /**
     * Retrieves a single inter-station request with strict station isolation.
     */
    @Transactional(readOnly = true)
    public InterStationRequestResponseDto getRequestById(UUID requestId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        InterStationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Inter-station coordination request not found: " + requestId));

        UUID officerStationId = officer.getStation() != null ? officer.getStation().getId() : null;
        if (officerStationId == null ||
                (!officerStationId.equals(request.getSourceStation().getId()) &&
                 !officerStationId.equals(request.getDestinationStation().getId()))) {
            throw new AccessDeniedException("Access denied: You are not authorized to view this inter-station request");
        }

        return new InterStationRequestResponseDto(request);
    }

    /**
     * Destination station accepts an inter-station coordination request.
     */
    @Transactional
    public InterStationRequestResponseDto acceptRequest(UUID requestId, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        InterStationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Inter-station coordination request not found: " + requestId));

        UUID officerStationId = officer.getStation() != null ? officer.getStation().getId() : null;
        if (officerStationId == null || !officerStationId.equals(request.getDestinationStation().getId())) {
            throw new AccessDeniedException("Access denied: Only officers from the destination station can accept this request");
        }

        request.setStatus("ACCEPTED");
        request.setRespondedByOfficer(officer);
        request.setRespondedAt(OffsetDateTime.now());
        InterStationRequest saved = requestRepository.save(request);

        recordAuditLog(officer, "REQUEST_ACCEPTED", "ACCEPTED by " + officer.getPoliceId(), httpRequest);

        return new InterStationRequestResponseDto(saved);
    }

    /**
     * Destination station rejects an inter-station coordination request.
     */
    @Transactional
    public InterStationRequestResponseDto rejectRequest(UUID requestId, RespondInterStationRequestDto dto, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        InterStationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Inter-station coordination request not found: " + requestId));

        UUID officerStationId = officer.getStation() != null ? officer.getStation().getId() : null;
        if (officerStationId == null || !officerStationId.equals(request.getDestinationStation().getId())) {
            throw new AccessDeniedException("Access denied: Only officers from the destination station can reject this request");
        }

        request.setStatus("REJECTED");
        if (dto != null && dto.getResponseNotes() != null) {
            request.setResponseNotes(dto.getResponseNotes().trim());
        }
        request.setRespondedByOfficer(officer);
        request.setRespondedAt(OffsetDateTime.now());
        InterStationRequest saved = requestRepository.save(request);

        recordAuditLog(officer, "REQUEST_REJECTED", "REJECTED by " + officer.getPoliceId(), httpRequest);

        return new InterStationRequestResponseDto(saved);
    }

    /**
     * Destination station responds to / completes an inter-station coordination request.
     */
    @Transactional
    public InterStationRequestResponseDto respondToRequest(UUID requestId, RespondInterStationRequestDto dto, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        InterStationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Inter-station coordination request not found: " + requestId));

        UUID officerStationId = officer.getStation() != null ? officer.getStation().getId() : null;
        if (officerStationId == null || !officerStationId.equals(request.getDestinationStation().getId())) {
            throw new AccessDeniedException("Access denied: Only officers from the destination station can respond to this request");
        }

        request.setStatus("COMPLETED");
        if (dto != null && dto.getResponseNotes() != null) {
            request.setResponseNotes(dto.getResponseNotes().trim());
        } else if (request.getResponseNotes() == null) {
            request.setResponseNotes("Inter-station coordination completed.");
        }
        request.setRespondedByOfficer(officer);
        request.setRespondedAt(OffsetDateTime.now());
        InterStationRequest saved = requestRepository.save(request);

        recordAuditLog(officer, "REQUEST_RESPONDED", "RESPONDED by " + officer.getPoliceId(), httpRequest);
        recordAuditLog(officer, "REQUEST_COMPLETED", "COMPLETED", httpRequest);

        return new InterStationRequestResponseDto(saved);
    }

    private PoliceOfficer getAuthenticatedOfficer(String policeId) {
        return officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated police officer record not found: " + policeId));
    }

    private void recordAuditLog(PoliceOfficer officer, String action, String status, HttpServletRequest request) {
        try {
            String clientIp = extractClientIp(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : "Unknown";
            AuditLog log = new AuditLog(null, officer, action, status, clientIp, userAgent);
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
