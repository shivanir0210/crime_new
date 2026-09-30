package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.victim.CreateVictimRequest;
import com.crimesphere.backend.dto.victim.UpdateVictimRequest;
import com.crimesphere.backend.dto.victim.VictimResponse;
import com.crimesphere.backend.entity.AuditLog;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.entity.Victim;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import com.crimesphere.backend.repository.VictimRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VictimService {

    private static final Logger logger = LoggerFactory.getLogger(VictimService.class);

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "ACTIVE", "DECEASED", "MISSING", "IDENTIFIED"
    );

    private final VictimRepository victimRepository;
    private final CaseRepository caseRepository;
    private final PoliceOfficerRepository officerRepository;
    private final AuditLogRepository auditLogRepository;
    private final StorageService storageService;
    private final GeminiImageService geminiImageService;

    public VictimService(VictimRepository victimRepository,
                         CaseRepository caseRepository,
                         PoliceOfficerRepository officerRepository,
                         AuditLogRepository auditLogRepository) {
        this(victimRepository, caseRepository, officerRepository, auditLogRepository, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public VictimService(VictimRepository victimRepository,
                         CaseRepository caseRepository,
                         PoliceOfficerRepository officerRepository,
                         AuditLogRepository auditLogRepository,
                         StorageService storageService,
                         GeminiImageService geminiImageService) {
        this.victimRepository = victimRepository;
        this.caseRepository = caseRepository;
        this.officerRepository = officerRepository;
        this.auditLogRepository = auditLogRepository;
        this.storageService = storageService != null ? storageService : new StorageService();
        this.geminiImageService = geminiImageService != null ? geminiImageService : new GeminiImageService("", "imagen-3.0-generate-002", 45, true, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Transactional
    public VictimResponse createVictim(UUID caseId, CreateVictimRequest request, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Victim full name is required");
        }

        String status = request.getStatus() != null && !request.getStatus().trim().isEmpty()
                ? request.getStatus().trim().toUpperCase() : "ACTIVE";

        if (!ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid victim status: " + status + ". Allowed: " + ALLOWED_STATUSES);
        }

        Victim victim = new Victim();
        victim.setCaseEntity(parentCase);
        victim.setFullName(request.getFullName().trim());
        victim.setAge(request.getAge());
        victim.setGender(request.getGender() != null ? request.getGender().trim().toUpperCase() : null);
        victim.setDateOfBirth(request.getDateOfBirth());
        victim.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        victim.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        victim.setOccupation(request.getOccupation() != null ? request.getOccupation().trim() : null);
        victim.setRelationshipToCase(request.getRelationshipToCase() != null ? request.getRelationshipToCase().trim() : null);
        victim.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        victim.setStatus(status);
        victim.setImageSource("SYNTHETIC_DEMO");
        victim.setCreatedBy(officer);

        Victim savedVictim = victimRepository.save(victim);

        // Update parent case updated_at
        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(officer, "VICTIM_CREATED", "SUCCESS", httpRequest);
        logger.info("Victim record created (ID: {}) for case {} by officer {}", savedVictim.getId(), parentCase.getCaseNumber(), policeId);

        return new VictimResponse(savedVictim);
    }

    @Transactional(readOnly = true)
    public List<VictimResponse> getVictimsByCaseId(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        return victimRepository.findByCaseEntityIdOrderByCreatedAtAsc(parentCase.getId())
                .stream()
                .map(VictimResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VictimResponse getVictimById(UUID caseId, UUID victimId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Victim victim = victimRepository.findByIdAndCaseEntityId(victimId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Victim record not found in this case"));

        return new VictimResponse(victim);
    }

    @Transactional
    public VictimResponse updateVictim(UUID caseId, UUID victimId, UpdateVictimRequest request, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Victim victim = victimRepository.findByIdAndCaseEntityId(victimId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Victim record not found in this case to update"));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            victim.setFullName(request.getFullName().trim());
        }
        if (request.getAge() != null) victim.setAge(request.getAge());
        if (request.getGender() != null) victim.setGender(request.getGender().trim().toUpperCase());
        if (request.getDateOfBirth() != null) victim.setDateOfBirth(request.getDateOfBirth());
        if (request.getPhone() != null) victim.setPhone(request.getPhone().trim());
        if (request.getAddress() != null) victim.setAddress(request.getAddress().trim());
        if (request.getOccupation() != null) victim.setOccupation(request.getOccupation().trim());
        if (request.getRelationshipToCase() != null) victim.setRelationshipToCase(request.getRelationshipToCase().trim());
        if (request.getDescription() != null) victim.setDescription(request.getDescription().trim());

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            String status = request.getStatus().trim().toUpperCase();
            if (!ALLOWED_STATUSES.contains(status)) {
                throw new IllegalArgumentException("Invalid victim status: " + status + ". Allowed: " + ALLOWED_STATUSES);
            }
            victim.setStatus(status);
        }

        Victim updatedVictim = victimRepository.save(victim);

        // Update parent case updated_at
        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(officer, "VICTIM_UPDATED", "SUCCESS", httpRequest);
        logger.info("Victim record updated (ID: {}) for case {} by officer {}", victimId, parentCase.getCaseNumber(), policeId);

        return new VictimResponse(updatedVictim);
    }

    @Transactional
    public VictimResponse uploadVictimPhoto(UUID caseId, UUID victimId, org.springframework.web.multipart.MultipartFile file, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Victim victim = victimRepository.findByIdAndCaseEntityId(victimId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Victim record not found in this case to upload photo"));

        storageService.validateFile(file);

        String storagePath = storageService.storeVictimPhoto(caseId, victimId, file);
        victim.setPhotoStoragePath(storagePath);
        victim.setImageSource("UPLOADED");
        victim.setPhotoUrl("/api/cases/" + caseId + "/victims/" + victimId + "/photo");

        Victim saved = victimRepository.save(victim);
        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(officer, "VICTIM_PHOTO_UPLOADED", "SUCCESS", httpRequest);
        logger.info("Uploaded photo for victim {} in case {}", victimId, parentCase.getCaseNumber());

        return new VictimResponse(saved);
    }

    @Transactional
    public SuspectService.StorageServicePhotoResult getVictimDefaultImage(UUID caseId, UUID victimId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        getAuthorizedCase(caseId, officer);
        return getVictimDefaultImagePublic(caseId, victimId);
    }

    @Transactional
    public SuspectService.StorageServicePhotoResult getVictimDefaultImagePublic(UUID caseId, UUID victimId) {
        Victim victim = victimRepository.findByIdAndCaseEntityId(victimId, caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Victim record not found in this case"));

        Case parentCase = victim.getCaseEntity();
        String caseTitle = parentCase != null ? parentCase.getTitle() : "Case Investigation";

        String syntheticPath = "cases/" + caseId + "/victims/" + victimId + "/synthetic_portrait.jpg";
        try {
            org.springframework.core.io.Resource cachedResource = storageService.loadEvidenceAsResource(syntheticPath);
            if (cachedResource.exists()) {
                return new SuspectService.StorageServicePhotoResult(cachedResource, "image/jpeg", "victim_dummy_" + victimId + ".jpg");
            }
        } catch (Exception ignored) {
            // Not cached yet, generate and persist
        }

        GeminiImageService.GeneratedImageResult result = geminiImageService.generateFictionalDossierPortrait(
                "VICTIM",
                victim.getFullName(),
                victim.getRelationshipToCase(),
                victim.getAge(),
                victim.getGender(),
                caseTitle,
                victim.getId() != null ? victim.getId().toString() : UUID.randomUUID().toString()
        );

        String storedPath = storageService.storeSyntheticPortraitBytes(caseId, "victim", victimId, result.getImageBytes(), result.getMimeType());
        org.springframework.core.io.Resource resource = storageService.loadEvidenceAsResource(storedPath);

        return new SuspectService.StorageServicePhotoResult(resource, result.getMimeType(), "victim_dummy_" + victimId + ".jpg");
    }

    @Transactional
    public SuspectService.StorageServicePhotoResult getVictimPhoto(UUID caseId, UUID victimId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        getAuthorizedCase(caseId, officer);
        return getVictimPhotoPublic(caseId, victimId);
    }

    @Transactional
    public SuspectService.StorageServicePhotoResult getVictimPhotoPublic(UUID caseId, UUID victimId) {
        Victim victim = victimRepository.findByIdAndCaseEntityId(victimId, caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Victim record not found in this case"));

        if ("UPLOADED".equalsIgnoreCase(victim.getImageSource()) && victim.getPhotoStoragePath() != null && !victim.getPhotoStoragePath().isBlank()) {
            try {
                org.springframework.core.io.Resource resource = storageService.loadEvidenceAsResource(victim.getPhotoStoragePath());
                if (resource.exists()) {
                    return new SuspectService.StorageServicePhotoResult(resource, "image/jpeg", "victim_" + victimId + ".jpg");
                }
            } catch (Exception e) {
                logger.warn("Uploaded victim photo could not be loaded, falling back to dummy image: {}", e.getMessage());
            }
        }

        // Fall back seamlessly to synthetic default portrait
        return getVictimDefaultImagePublic(caseId, victimId);
    }

    private Case getAuthorizedCase(UUID caseId, PoliceOfficer officer) {
        UUID stationId = officer.getStation() != null ? officer.getStation().getId() : null;
        if (stationId == null) {
            throw new IllegalStateException("Police officer has no assigned station");
        }
        return caseRepository.findByIdAndStationId(caseId, stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found or access restricted to your station"));
    }

    private PoliceOfficer getAuthenticatedOfficer(String policeId) {
        return officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated police officer record not found"));
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
