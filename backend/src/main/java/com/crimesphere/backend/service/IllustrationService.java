package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.illustration.CaseIllustrationResponse;
import com.crimesphere.backend.dto.illustration.GenerateIllustrationRequest;
import com.crimesphere.backend.entity.AuditLog;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.CaseIllustration;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.CaseIllustrationRepository;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class IllustrationService {

    private static final Logger logger = LoggerFactory.getLogger(IllustrationService.class);

    private final CaseIllustrationRepository illustrationRepository;
    private final CaseRepository caseRepository;
    private final PoliceOfficerRepository officerRepository;
    private final GeminiImageService geminiImageService;
    private final StorageService storageService;
    private final AuditLogRepository auditLogRepository;

    public IllustrationService(
            CaseIllustrationRepository illustrationRepository,
            CaseRepository caseRepository,
            PoliceOfficerRepository officerRepository,
            GeminiImageService geminiImageService,
            StorageService storageService,
            AuditLogRepository auditLogRepository
    ) {
        this.illustrationRepository = illustrationRepository;
        this.caseRepository = caseRepository;
        this.officerRepository = officerRepository;
        this.geminiImageService = geminiImageService;
        this.storageService = storageService;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Generates a new AI illustration for the specified case with strict station isolation and audit logging.
     */
    @Transactional
    public CaseIllustrationResponse generateIllustration(
            UUID caseId,
            GenerateIllustrationRequest request,
            String policeId,
            HttpServletRequest httpRequest
    ) {
        if (request == null || !StringUtils.hasText(request.getPrompt())) {
            throw new IllegalArgumentException("Prompt is mandatory for AI illustration generation");
        }

        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        String prompt = request.getPrompt().trim();
        logger.info("Officer {} generating AI scene illustration for case {} ({})",
                policeId, parentCase.getCaseNumber(), caseId);

        // 1. Call Gemini Image Generation Service
        GeminiImageService.GeneratedImageResult result = geminiImageService.generateIllustration(prompt, request.getAspectRatio());

        // 2. Compute file hash
        byte[] imageBytes = result.getImageBytes();
        String fileHash = storageService.calculateSha256(imageBytes);

        UUID illustrationId = UUID.randomUUID();

        // 3. Store in secure storage
        String storagePath = storageService.storeIllustrationBytes(caseId, illustrationId, imageBytes, result.getMimeType());

        // 4. Save entity with strict non-evidence flags
        CaseIllustration illustration = new CaseIllustration();
        illustration.setId(illustrationId);
        illustration.setCaseEntity(parentCase);
        illustration.setPrompt(prompt);
        illustration.setRevisedPrompt(result.getRevisedPrompt());
        illustration.setModelName(result.getModelUsed());
        illustration.setStoragePath(storagePath);
        illustration.setMimeType(result.getMimeType());
        illustration.setFileSize((long) imageBytes.length);
        illustration.setFileHash(fileHash);
        illustration.setStatus("AI_ILLUSTRATION");
        illustration.setIsAiGenerated(true);
        illustration.setCreatedBy(officer);

        CaseIllustration saved = illustrationRepository.save(illustration);

        // 5. Audit log
        recordAudit(officer, "AI_ILLUSTRATION_GENERATED", "SUCCESS", httpRequest);

        return mapToResponse(saved);
    }

    /**
     * Retrieves all illustrations for a given case.
     */
    @Transactional(readOnly = true)
    public List<CaseIllustrationResponse> getIllustrationsByCase(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        getAuthorizedCase(caseId, officer);

        return illustrationRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Loads the raw illustration image for inline viewing.
     */
    @Transactional(readOnly = true)
    public StorageServiceFileResult getIllustrationFile(UUID caseId, UUID illustrationId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        getAuthorizedCase(caseId, officer);
        return getIllustrationFilePublic(caseId, illustrationId);
    }

    /**
     * Loads the raw illustration image for direct browser rendering.
     */
    @Transactional(readOnly = true)
    public StorageServiceFileResult getIllustrationFilePublic(UUID caseId, UUID illustrationId) {
        CaseIllustration illustration = illustrationRepository.findByIdAndCaseEntityId(illustrationId, caseId)
                .orElseThrow(() -> new ResourceNotFoundException("AI illustration not found"));

        Resource resource = storageService.loadEvidenceAsResource(illustration.getStoragePath());
        return new StorageServiceFileResult(resource, illustration.getMimeType(), "illustration_" + illustrationId + ".jpg");
    }

    /**
     * Deletes an AI illustration record and removes underlying storage.
     */
    @Transactional
    public void deleteIllustration(UUID caseId, UUID illustrationId, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        getAuthorizedCase(caseId, officer);

        CaseIllustration illustration = illustrationRepository.findByIdAndCaseEntityId(illustrationId, caseId)
                .orElseThrow(() -> new ResourceNotFoundException("AI illustration not found"));

        storageService.deleteEvidenceFile(illustration.getStoragePath());
        illustrationRepository.delete(illustration);

        recordAudit(officer, "AI_ILLUSTRATION_DELETED", "SUCCESS", httpRequest);
        logger.info("Deleted AI illustration {} from case {}", illustrationId, caseId);
    }

    public static class StorageServiceFileResult {
        private final Resource resource;
        private final String contentType;
        private final String filename;

        public StorageServiceFileResult(Resource resource, String contentType, String filename) {
            this.resource = resource;
            this.contentType = contentType;
            this.filename = filename;
        }

        public Resource getResource() {
            return resource;
        }

        public String getContentType() {
            return contentType;
        }

        public String getFilename() {
            return filename;
        }
    }

    private CaseIllustrationResponse mapToResponse(CaseIllustration i) {
        CaseIllustrationResponse res = new CaseIllustrationResponse();
        res.setId(i.getId());
        res.setCaseId(i.getCaseEntity().getId());
        res.setPrompt(i.getPrompt());
        res.setRevisedPrompt(i.getRevisedPrompt());
        res.setModelName(i.getModelName());
        res.setMimeType(i.getMimeType());
        res.setFileSize(i.getFileSize());
        res.setFileHash(i.getFileHash());
        res.setStatus(i.getStatus());
        res.setIsAiGenerated(i.getIsAiGenerated());
        res.setViewUrl("/api/cases/" + i.getCaseEntity().getId() + "/illustrations/" + i.getId() + "/view");
        if (i.getCreatedBy() != null) {
            res.setCreatedByOfficerName(i.getCreatedBy().getFullName());
            res.setCreatedByPoliceId(i.getCreatedBy().getPoliceId());
        }
        res.setCreatedAt(i.getCreatedAt());
        return res;
    }

    private PoliceOfficer getAuthenticatedOfficer(String policeId) {
        if (!StringUtils.hasText(policeId)) {
            throw new BadCredentialsException("Invalid police authentication token");
        }
        return officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new BadCredentialsException("Police officer profile not found"));
    }

    private Case getAuthorizedCase(UUID caseId, PoliceOfficer officer) {
        Case c = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found"));

        if (!officer.getStation().getId().equals(c.getStation().getId())) {
            throw new AccessDeniedException("Access denied: You are restricted to cases filed under " + officer.getStation().getStationName());
        }
        return c;
    }

    private void recordAudit(PoliceOfficer officer, String action, String status, HttpServletRequest request) {
        try {
            String ip = request != null ? request.getRemoteAddr() : "127.0.0.1";
            String userAgent = request != null ? request.getHeader("User-Agent") : "Internal System";
            AuditLog log = new AuditLog(UUID.randomUUID(), officer, action, status, ip, userAgent);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to persist audit log for action {}: {}", action, e.getMessage());
        }
    }
}
