package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.suspect.CreateSuspectRequest;
import com.crimesphere.backend.dto.suspect.SuspectResponse;
import com.crimesphere.backend.dto.suspect.UpdateSuspectRequest;
import com.crimesphere.backend.entity.AuditLog;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.entity.Suspect;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import com.crimesphere.backend.repository.SuspectRepository;
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
public class SuspectService {

    private static final Logger logger = LoggerFactory.getLogger(SuspectService.class);

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "UNDER_INVESTIGATION", "PERSON_OF_INTEREST", "CLEARED", "REFERRED"
    );

    private final SuspectRepository suspectRepository;
    private final CaseRepository caseRepository;
    private final PoliceOfficerRepository officerRepository;
    private final AuditLogRepository auditLogRepository;
    private final StorageService storageService;
    private final GeminiImageService geminiImageService;

    public SuspectService(SuspectRepository suspectRepository,
                          CaseRepository caseRepository,
                          PoliceOfficerRepository officerRepository,
                          AuditLogRepository auditLogRepository) {
        this(suspectRepository, caseRepository, officerRepository, auditLogRepository, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SuspectService(SuspectRepository suspectRepository,
                          CaseRepository caseRepository,
                          PoliceOfficerRepository officerRepository,
                          AuditLogRepository auditLogRepository,
                          StorageService storageService,
                          GeminiImageService geminiImageService) {
        this.suspectRepository = suspectRepository;
        this.caseRepository = caseRepository;
        this.officerRepository = officerRepository;
        this.auditLogRepository = auditLogRepository;
        this.storageService = storageService != null ? storageService : new StorageService();
        this.geminiImageService = geminiImageService != null ? geminiImageService : new GeminiImageService("", "imagen-3.0-generate-002", 45, true, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Transactional
    public SuspectResponse createSuspect(UUID caseId, CreateSuspectRequest request, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Suspect full name is required");
        }

        String status = request.getInvestigationStatus() != null && !request.getInvestigationStatus().trim().isEmpty()
                ? request.getInvestigationStatus().trim().toUpperCase() : "UNDER_INVESTIGATION";

        if (!ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid investigation status: " + status + ". Allowed: " + ALLOWED_STATUSES);
        }

        Suspect suspect = new Suspect();
        suspect.setCaseEntity(parentCase);
        suspect.setFullName(request.getFullName().trim());
        suspect.setAlias(request.getAlias() != null ? request.getAlias().trim() : null);
        suspect.setAge(request.getAge());
        suspect.setGender(request.getGender() != null ? request.getGender().trim().toUpperCase() : null);
        suspect.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        suspect.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        suspect.setOccupation(request.getOccupation() != null ? request.getOccupation().trim() : null);
        suspect.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        suspect.setInvestigationStatus(status);
        suspect.setImageSource("SYNTHETIC_DEMO");
        suspect.setCreatedBy(officer);

        Suspect savedSuspect = suspectRepository.save(suspect);

        // Update parent case updated_at
        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(officer, "SUSPECT_CREATED", "SUCCESS", httpRequest);
        logger.info("Suspect record created (ID: {}) for case {} by officer {}", savedSuspect.getId(), parentCase.getCaseNumber(), policeId);

        return new SuspectResponse(savedSuspect);
    }

    @Transactional(readOnly = true)
    public List<SuspectResponse> getSuspectsByCaseId(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        return suspectRepository.findByCaseEntityIdOrderByCreatedAtAsc(parentCase.getId())
                .stream()
                .map(SuspectResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SuspectResponse getSuspectById(UUID caseId, UUID suspectId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Suspect suspect = suspectRepository.findByIdAndCaseEntityId(suspectId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Suspect record not found in this case"));

        return new SuspectResponse(suspect);
    }

    @Transactional
    public SuspectResponse updateSuspect(UUID caseId, UUID suspectId, UpdateSuspectRequest request, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Suspect suspect = suspectRepository.findByIdAndCaseEntityId(suspectId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Suspect record not found in this case to update"));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            suspect.setFullName(request.getFullName().trim());
        }
        if (request.getAlias() != null) suspect.setAlias(request.getAlias().trim());
        if (request.getAge() != null) suspect.setAge(request.getAge());
        if (request.getGender() != null) suspect.setGender(request.getGender().trim().toUpperCase());
        if (request.getPhone() != null) suspect.setPhone(request.getPhone().trim());
        if (request.getAddress() != null) suspect.setAddress(request.getAddress().trim());
        if (request.getOccupation() != null) suspect.setOccupation(request.getOccupation().trim());
        if (request.getDescription() != null) suspect.setDescription(request.getDescription().trim());

        if (request.getInvestigationStatus() != null && !request.getInvestigationStatus().trim().isEmpty()) {
            String status = request.getInvestigationStatus().trim().toUpperCase();
            if (!ALLOWED_STATUSES.contains(status)) {
                throw new IllegalArgumentException("Invalid suspect investigation status: " + status + ". Allowed: " + ALLOWED_STATUSES);
            }
            suspect.setInvestigationStatus(status);
        }

        Suspect updatedSuspect = suspectRepository.save(suspect);

        // Update parent case updated_at
        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(officer, "SUSPECT_UPDATED", "SUCCESS", httpRequest);
        logger.info("Suspect record updated (ID: {}) for case {} by officer {}", suspectId, parentCase.getCaseNumber(), policeId);

        return new SuspectResponse(updatedSuspect);
    }

    @Transactional
    public SuspectResponse uploadSuspectPhoto(UUID caseId, UUID suspectId, org.springframework.web.multipart.MultipartFile file, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Suspect suspect = suspectRepository.findByIdAndCaseEntityId(suspectId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Suspect record not found in this case to upload photo"));

        storageService.validateFile(file);

        String storagePath = storageService.storeSuspectPhoto(caseId, suspectId, file);
        suspect.setPhotoStoragePath(storagePath);
        suspect.setImageSource("UPLOADED");
        suspect.setPhotoUrl("/api/cases/" + caseId + "/suspects/" + suspectId + "/photo");

        Suspect saved = suspectRepository.save(suspect);
        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(officer, "SUSPECT_PHOTO_UPLOADED", "SUCCESS", httpRequest);
        logger.info("Uploaded photo for suspect {} in case {}", suspectId, parentCase.getCaseNumber());

        return new SuspectResponse(saved);
    }

    @Transactional
    public StorageServicePhotoResult getSuspectDefaultImage(UUID caseId, UUID suspectId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        getAuthorizedCase(caseId, officer);
        return getSuspectDefaultImagePublic(caseId, suspectId);
    }

    @Transactional
    public StorageServicePhotoResult getSuspectDefaultImagePublic(UUID caseId, UUID suspectId) {
        Suspect suspect = suspectRepository.findByIdAndCaseEntityId(suspectId, caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Suspect record not found in this case"));

        Case parentCase = suspect.getCaseEntity();
        String caseTitle = parentCase != null ? parentCase.getTitle() : "Case Investigation";

        String syntheticPath = "cases/" + caseId + "/suspects/" + suspectId + "/synthetic_portrait.jpg";
        try {
            org.springframework.core.io.Resource cachedResource = storageService.loadEvidenceAsResource(syntheticPath);
            if (cachedResource.exists()) {
                return new StorageServicePhotoResult(cachedResource, "image/jpeg", "suspect_dummy_" + suspectId + ".jpg");
            }
        } catch (Exception ignored) {
            // Not cached yet, proceed to generate and persist
        }

        GeminiImageService.GeneratedImageResult result = geminiImageService.generateFictionalDossierPortrait(
                "SUSPECT",
                suspect.getFullName(),
                suspect.getAlias(),
                suspect.getAge(),
                suspect.getGender(),
                caseTitle,
                suspect.getId() != null ? suspect.getId().toString() : UUID.randomUUID().toString()
        );

        String storedPath = storageService.storeSyntheticPortraitBytes(caseId, "suspect", suspectId, result.getImageBytes(), result.getMimeType());
        org.springframework.core.io.Resource resource = storageService.loadEvidenceAsResource(storedPath);

        return new StorageServicePhotoResult(resource, result.getMimeType(), "suspect_dummy_" + suspectId + ".jpg");
    }

    @Transactional
    public StorageServicePhotoResult getSuspectPhoto(UUID caseId, UUID suspectId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        getAuthorizedCase(caseId, officer);
        return getSuspectPhotoPublic(caseId, suspectId);
    }

    @Transactional
    public StorageServicePhotoResult getSuspectPhotoPublic(UUID caseId, UUID suspectId) {
        Suspect suspect = suspectRepository.findByIdAndCaseEntityId(suspectId, caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Suspect record not found in this case"));

        if ("UPLOADED".equalsIgnoreCase(suspect.getImageSource()) && suspect.getPhotoStoragePath() != null && !suspect.getPhotoStoragePath().isBlank()) {
            try {
                org.springframework.core.io.Resource resource = storageService.loadEvidenceAsResource(suspect.getPhotoStoragePath());
                if (resource.exists()) {
                    return new StorageServicePhotoResult(resource, "image/jpeg", "suspect_" + suspectId + ".jpg");
                }
            } catch (Exception e) {
                logger.warn("Uploaded suspect photo could not be loaded, falling back to dummy image: {}", e.getMessage());
            }
        }

        // Fall back seamlessly to synthetic default portrait
        return getSuspectDefaultImagePublic(caseId, suspectId);
    }

    public static class StorageServicePhotoResult {
        private final org.springframework.core.io.Resource resource;
        private final String contentType;
        private final String filename;

        public StorageServicePhotoResult(org.springframework.core.io.Resource resource, String contentType, String filename) {
            this.resource = resource;
            this.contentType = contentType;
            this.filename = filename;
        }

        public org.springframework.core.io.Resource getResource() {
            return resource;
        }

        public String getContentType() {
            return contentType;
        }

        public String getFilename() {
            return filename;
        }
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
