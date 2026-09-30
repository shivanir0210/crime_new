package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.evidence.*;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.Evidence;
import com.crimesphere.backend.entity.EvidenceAuditLog;
import com.crimesphere.backend.entity.NotificationType;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.entity.PoliceStation;
import com.crimesphere.backend.exception.CrossCaseDuplicateEvidenceException;
import com.crimesphere.backend.exception.DuplicateEvidenceException;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.EvidenceAuditLogRepository;
import com.crimesphere.backend.repository.EvidenceRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EvidenceService {

    private static final Logger logger = LoggerFactory.getLogger(EvidenceService.class);

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "DOCUMENT", "IMAGE", "VIDEO", "AUDIO", "FORENSIC_REPORT",
            "CCTV", "CALL_RECORD", "DIGITAL_DEVICE", "OTHER"
    );

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "UPLOADED", "UNDER_REVIEW", "VERIFIED", "REJECTED", "ARCHIVED", "AI_ILLUSTRATION"
    );

    private final EvidenceRepository evidenceRepository;
    private final EvidenceAuditLogRepository auditLogRepository;
    private final CaseRepository caseRepository;
    private final PoliceOfficerRepository officerRepository;
    private final StorageService storageService;
    private final NotificationService notificationService;
    private final GeminiImageService geminiImageService;

    public EvidenceService(EvidenceRepository evidenceRepository,
                           EvidenceAuditLogRepository auditLogRepository,
                           CaseRepository caseRepository,
                           PoliceOfficerRepository officerRepository,
                           StorageService storageService) {
        this(evidenceRepository, auditLogRepository, caseRepository, officerRepository, storageService, null, null);
    }

    public EvidenceService(EvidenceRepository evidenceRepository,
                           EvidenceAuditLogRepository auditLogRepository,
                           CaseRepository caseRepository,
                           PoliceOfficerRepository officerRepository,
                           StorageService storageService,
                           NotificationService notificationService) {
        this(evidenceRepository, auditLogRepository, caseRepository, officerRepository, storageService, notificationService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EvidenceService(EvidenceRepository evidenceRepository,
                           EvidenceAuditLogRepository auditLogRepository,
                           CaseRepository caseRepository,
                           PoliceOfficerRepository officerRepository,
                           StorageService storageService,
                           NotificationService notificationService,
                           GeminiImageService geminiImageService) {
        this.evidenceRepository = evidenceRepository;
        this.auditLogRepository = auditLogRepository;
        this.caseRepository = caseRepository;
        this.officerRepository = officerRepository;
        this.storageService = storageService;
        this.notificationService = notificationService;
        this.geminiImageService = geminiImageService;
    }

    @Transactional
    public EvidenceResponse uploadEvidence(UUID caseId,
                                           MultipartFile file,
                                           CreateEvidenceMetadata metadata,
                                           String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        // 1. Validate metadata
        String type = metadata.getEvidenceType() != null ? metadata.getEvidenceType().trim().toUpperCase() : "OTHER";
        if (!ALLOWED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Invalid evidence type: " + type + ". Allowed types: " + ALLOWED_TYPES);
        }

        // 2. Generate unique evidence number
        PoliceStation station = parentCase.getStation();
        String stationCodeShort = extractStationShortCode(station);
        String evidenceNumber = generateEvidenceNumber(stationCodeShort);

        UUID evidenceId = UUID.randomUUID();
        String storagePath = null;
        String originalFileName = null;
        String mimeType = null;
        long fileSize = 0L;
        String fileHash = null;
        String category = "UPLOADED_EVIDENCE";
        boolean isAiGenerated = false;
        String status = "UPLOADED";

        try {
            boolean hasRealFile = file != null && !file.isEmpty();

            String confirmedDuplicateCaseNumber = null;
            if (hasRealFile) {
                storageService.validateFile(file);
                fileHash = storageService.calculateSha256(file);

                // Duplicate Check
                if (evidenceRepository.existsByCaseEntityIdAndFileHash(caseId, fileHash)) {
                    throw new DuplicateEvidenceException("This evidence file already exists in this case.");
                }

                List<Evidence> otherCaseEvidences = evidenceRepository.findByFileHashAndCaseEntityIdNot(fileHash, caseId);
                if (!otherCaseEvidences.isEmpty()) {
                    String otherCaseNumber = otherCaseEvidences.get(0).getCaseEntity().getCaseNumber();
                    if (!Boolean.TRUE.equals(metadata.getConfirmDuplicate())) {
                        throw new CrossCaseDuplicateEvidenceException(
                                otherCaseNumber,
                                "This file already exists in another case: " + otherCaseNumber + ". It may be relevant to multiple investigations."
                        );
                    }
                    confirmedDuplicateCaseNumber = otherCaseNumber;
                }

                storagePath = storageService.storeEvidenceFile(caseId, evidenceId, file);
                originalFileName = file.getOriginalFilename();
                mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
                fileSize = file.getSize();

                category = "UPLOADED_EVIDENCE";
                isAiGenerated = false;
                status = "UPLOADED";
            } else {
                // No real file uploaded: Generate default contextual forensic illustration
                String prompt = (geminiImageService != null)
                        ? geminiImageService.buildEvidenceIllustrationPrompt(type, metadata.getTitle(), metadata.getDescription(), parentCase.getTitle(), parentCase.getDescription())
                        : ("Default forensic illustration for " + type + " - " + metadata.getTitle());

                GeminiImageService.GeneratedImageResult genResult = (geminiImageService != null)
                        ? geminiImageService.generateIllustration(prompt, "1:1", type, metadata.getTitle())
                        : null;

                byte[] imageBytes = genResult != null ? genResult.getImageBytes() : new byte[0];
                storagePath = storageService.storeIllustrationBytes(caseId, evidenceId, imageBytes, "image/jpeg");
                fileHash = storageService.calculateSha256(imageBytes);
                originalFileName = "ai-illustration-" + evidenceNumber.toLowerCase().replace("-", "_") + ".jpg";
                mimeType = "image/jpeg";
                fileSize = imageBytes.length;
                category = "AI_ILLUSTRATION";
                isAiGenerated = true;
                status = "AI_ILLUSTRATION";
            }

            // Save Evidence Entity
            Evidence evidence = new Evidence();
            evidence.setId(evidenceId);
            evidence.setCaseEntity(parentCase);
            evidence.setEvidenceNumber(evidenceNumber);
            evidence.setTitle(metadata.getTitle() != null ? metadata.getTitle().trim() : "Untitled Evidence");
            evidence.setDescription(metadata.getDescription() != null ? metadata.getDescription().trim() : "");
            evidence.setEvidenceType(type);
            evidence.setStatus(status);
            evidence.setOriginalFileName(originalFileName);
            evidence.setStoragePath(storagePath);
            evidence.setMimeType(mimeType);
            evidence.setFileSize(fileSize);
            evidence.setFileHash(fileHash);
            evidence.setCollectedAt(metadata.getCollectedAt() != null ? metadata.getCollectedAt() : OffsetDateTime.now());
            evidence.setCollectedLocation(metadata.getCollectedLocation() != null ? metadata.getCollectedLocation().trim() : "");
            evidence.setCollectedBy(metadata.getCollectedBy() != null ? metadata.getCollectedBy().trim() : officer.getFullName());
            evidence.setNotes(metadata.getNotes() != null ? metadata.getNotes().trim() : "");
            evidence.setCategory(category);
            evidence.setIsAiGenerated(isAiGenerated);
            evidence.setCreatedBy(officer);

            Evidence savedEvidence = evidenceRepository.save(evidence);

            // Log Audit
            String auditDetail;
            if (isAiGenerated) {
                auditDetail = "Created evidence record (" + evidenceNumber + ") with default AI illustration specimen.";
            } else if (confirmedDuplicateCaseNumber != null) {
                auditDetail = "Cross-case duplicate confirmed from case " + confirmedDuplicateCaseNumber + ". Uploaded evidence file '" + originalFileName + "' (" + evidenceNumber + ") with SHA-256: " + fileHash;
            } else {
                auditDetail = "Uploaded evidence file '" + originalFileName + "' (" + evidenceNumber + ") with SHA-256: " + fileHash;
            }
            recordAuditLog(savedEvidence, parentCase, officer, "EVIDENCE_CREATED", auditDetail);

            // Dispatch Case Notification
            if (notificationService != null) {
                try {
                    String clueHeader = "New evidence logged for case " + parentCase.getCaseNumber() + ": " + savedEvidence.getTitle();
                    String details = String.format("New evidence logged for case %s: %s (Logged by %s)",
                            parentCase.getCaseNumber(), savedEvidence.getTitle(), officer.getFullName());

                    notificationService.dispatchCaseNotification(
                            parentCase,
                            NotificationType.EVIDENCE_UPLOADED,
                            clueHeader,
                            details,
                            "Review newly logged evidence in the case dossier.",
                            officer
                    );
                } catch (Exception e) {
                    logger.error("EVIDENCE_UPLOADED notification dispatch error for Evidence ID: {}: {}", savedEvidence.getId(), e.getMessage());
                }
            }

            // Update Parent Case Timestamp
            parentCase.setUpdatedAt(OffsetDateTime.now());
            caseRepository.save(parentCase);

            logger.info("Evidence {} successfully cataloged for case: {} by officer: {}",
                    evidenceNumber, parentCase.getCaseNumber(), policeId);

            return mapToResponse(savedEvidence);
        } catch (DuplicateEvidenceException | CrossCaseDuplicateEvidenceException e) {
            if (storagePath != null && !isAiGenerated) {
                storageService.deleteEvidenceFile(storagePath);
            }
            throw e;
        } catch (Exception e) {
            if (storagePath != null && !isAiGenerated) {
                storageService.deleteEvidenceFile(storagePath);
            }
            logger.error("Failed to upload/create evidence: {}", e.getMessage());
            throw new RuntimeException("Evidence creation failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public EvidenceResponse replaceWithRealEvidenceFile(UUID caseId,
                                                        UUID evidenceId,
                                                        MultipartFile file,
                                                        String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Real evidence file must be provided.");
        }

        storageService.validateFile(file);
        String fileHash = storageService.calculateSha256(file);

        Evidence evidence = evidenceRepository.findByIdAndCaseEntityId(evidenceId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Evidence record not found"));

        String newStoragePath = storageService.storeEvidenceFile(caseId, evidenceId, file);

        evidence.setStoragePath(newStoragePath);
        evidence.setOriginalFileName(file.getOriginalFilename());
        evidence.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        evidence.setFileSize(file.getSize());
        evidence.setFileHash(fileHash);
        evidence.setCategory("UPLOADED_EVIDENCE");
        evidence.setIsAiGenerated(false);
        evidence.setStatus("UPLOADED");

        Evidence saved = evidenceRepository.save(evidence);

        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(saved, parentCase, officer, "EVIDENCE_UPDATED",
                "Uploaded real forensic evidence image '" + file.getOriginalFilename() + "' replacing default AI illustration. Sealed with SHA-256: " + fileHash);

        logger.info("Replaced AI illustration with real evidence file for {} in case {}", evidence.getEvidenceNumber(), caseId);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getEvidenceByCase(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        List<Evidence> evidenceList = evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(parentCase.getId());
        return evidenceList.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public EvidenceResponse getEvidenceDetails(UUID caseId, UUID evidenceId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Evidence evidence = evidenceRepository.findByIdAndCaseEntityId(evidenceId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found or does not belong to this case"));

        recordAuditLog(evidence, parentCase, officer, "EVIDENCE_VIEWED",
                "Viewed evidence metadata for " + evidence.getEvidenceNumber());

        return mapToResponse(evidence);
    }

    @Transactional
    public EvidenceResponse updateEvidence(UUID caseId,
                                           UUID evidenceId,
                                           UpdateEvidenceRequest request,
                                           String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Evidence evidence = evidenceRepository.findByIdAndCaseEntityId(evidenceId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found or does not belong to this case"));

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            evidence.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            evidence.setDescription(request.getDescription().trim());
        }
        if (request.getEvidenceType() != null && !request.getEvidenceType().trim().isEmpty()) {
            String type = request.getEvidenceType().trim().toUpperCase();
            if (!ALLOWED_TYPES.contains(type)) {
                throw new IllegalArgumentException("Invalid evidence type: " + type);
            }
            evidence.setEvidenceType(type);
        }
        if (request.getCollectedAt() != null) {
            evidence.setCollectedAt(request.getCollectedAt());
        }
        if (request.getCollectedLocation() != null) {
            evidence.setCollectedLocation(request.getCollectedLocation().trim());
        }
        if (request.getCollectedBy() != null) {
            evidence.setCollectedBy(request.getCollectedBy().trim());
        }
        if (request.getNotes() != null) {
            evidence.setNotes(request.getNotes().trim());
        }

        Evidence updated = evidenceRepository.save(evidence);

        // Update Parent Case updated_at
        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(updated, parentCase, officer, "EVIDENCE_UPDATED",
                "Updated metadata for evidence " + evidence.getEvidenceNumber());

        logger.info("Evidence {} metadata updated by officer: {}", evidence.getEvidenceNumber(), policeId);

        return mapToResponse(updated);
    }

    @Transactional
    public EvidenceResponse updateEvidenceStatus(UUID caseId,
                                                 UUID evidenceId,
                                                 UpdateEvidenceStatusRequest request,
                                                 String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Evidence evidence = evidenceRepository.findByIdAndCaseEntityId(evidenceId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found or does not belong to this case"));

        String newStatus = request.getStatus() != null ? request.getStatus().trim().toUpperCase() : "";
        if (!ALLOWED_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status: " + newStatus + ". Allowed: " + ALLOWED_STATUSES);
        }

        if ("VERIFIED".equalsIgnoreCase(newStatus) && (Boolean.TRUE.equals(evidence.getIsAiGenerated()) || "AI_ILLUSTRATION".equalsIgnoreCase(evidence.getCategory()))) {
            throw new IllegalArgumentException("AI-generated synthetic illustrations cannot be marked as verified forensic evidence.");
        }

        String oldStatus = evidence.getStatus();
        evidence.setStatus(newStatus);
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            evidence.setNotes(evidence.getNotes() != null ? evidence.getNotes() + "\n" + request.getNotes() : request.getNotes());
        }

        Evidence saved = evidenceRepository.save(evidence);

        // Update Parent Case updated_at
        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(saved, parentCase, officer, "EVIDENCE_STATUS_UPDATED",
                "Status transitioned from " + oldStatus + " to " + newStatus +
                        (request.getNotes() != null ? " (Note: " + request.getNotes() + ")" : ""));

        if (!newStatus.equalsIgnoreCase(oldStatus) && notificationService != null) {
            try {
                notificationService.dispatchCaseNotification(
                        parentCase,
                        NotificationType.EVIDENCE_STATUS_CHANGED,
                        "CrimeSphere AI — Evidence Status Updated: " + saved.getEvidenceNumber(),
                        String.format("Evidence item %s status transitioned from %s to %s for case %s.",
                                saved.getEvidenceNumber(), oldStatus, newStatus, parentCase.getCaseNumber()),
                        "Review evidence custody log in case dossier.",
                        officer
                );
            } catch (Exception e) {
                logger.warn("EVIDENCE_STATUS_CHANGED notification failed: {}", e.getMessage());
            }
        }

        logger.info("Evidence {} status updated: {} -> {} by officer: {}",
                evidence.getEvidenceNumber(), oldStatus, newStatus, policeId);

        return mapToResponse(saved);
    }

    @Transactional
    public DownloadFileInfo downloadEvidenceFile(UUID caseId, UUID evidenceId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Evidence evidence = evidenceRepository.findByIdAndCaseEntityId(evidenceId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found or does not belong to this case"));

        Resource fileResource = storageService.loadEvidenceAsResource(evidence.getStoragePath());

        recordAuditLog(evidence, parentCase, officer, "EVIDENCE_DOWNLOADED",
                "Securely accessed/downloaded evidence file: " + evidence.getOriginalFileName());

        return new DownloadFileInfo(
                fileResource,
                evidence.getOriginalFileName(),
                evidence.getMimeType(),
                evidence.getFileSize()
        );
    }

    @Transactional(readOnly = true)
    public DownloadFileInfo downloadEvidenceFilePublic(UUID caseId, UUID evidenceId) {
        Evidence evidence = evidenceRepository.findByIdAndCaseEntityId(evidenceId, caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found or does not belong to this case"));

        Resource fileResource = storageService.loadEvidenceAsResource(evidence.getStoragePath());

        return new DownloadFileInfo(
                fileResource,
                evidence.getOriginalFileName(),
                evidence.getMimeType(),
                evidence.getFileSize()
        );
    }

    @Transactional
    public EvidenceResponse generateAiIllustrationForEvidence(UUID caseId,
                                                              UUID evidenceId,
                                                              String prompt,
                                                              String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        if (prompt == null || prompt.trim().isBlank()) {
            throw new IllegalArgumentException("Illustration prompt cannot be empty.");
        }

        Evidence evidence = evidenceRepository.findByIdAndCaseEntityId(evidenceId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found or does not belong to this case"));

        if (this.geminiImageService == null) {
            throw new IllegalStateException("Gemini Image Generation Service is not initialized");
        }
        GeminiImageService.GeneratedImageResult genResult = this.geminiImageService.generateIllustration(prompt.trim(), "1:1");
        byte[] imageBytes = genResult.getImageBytes();
        String storagePath = storageService.storeIllustrationBytes(caseId, evidenceId, imageBytes, genResult.getMimeType());
        String hash = storageService.calculateSha256(imageBytes);

        evidence.setStoragePath(storagePath);
        evidence.setMimeType("image/jpeg");
        evidence.setFileSize((long) imageBytes.length);
        evidence.setFileHash(hash);
        evidence.setCategory("AI_ILLUSTRATION");
        evidence.setIsAiGenerated(true);
        evidence.setStatus("AI_ILLUSTRATION");
        evidence.setOriginalFileName("ai-illustration-" + evidence.getEvidenceNumber().toLowerCase().replace("-", "_") + ".jpg");
        evidence.setNotes(evidence.getNotes() != null && !evidence.getNotes().isBlank() 
                ? evidence.getNotes() + "\nAI Scenario Prompt: " + prompt.trim() 
                : "AI Scenario Prompt: " + prompt.trim());

        Evidence saved = evidenceRepository.save(evidence);

        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(saved, parentCase, officer, "AI_ILLUSTRATION_GENERATED",
                "Generated synthetic AI scene illustration for hypothesis visualization (NOT verified evidence)");

        logger.info("Generated AI illustration for evidence {} in case {} by officer {}", evidence.getEvidenceNumber(), caseId, policeId);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EvidenceAuditLogResponse> getEvidenceAuditLogs(UUID caseId, UUID evidenceId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        // Verify evidence belongs to this case
        evidenceRepository.findByIdAndCaseEntityId(evidenceId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found or does not belong to this case"));

        List<EvidenceAuditLog> logs = auditLogRepository.findByEvidenceIdOrderByCreatedAtDesc(evidenceId);
        return logs.stream().map(log -> {
            EvidenceAuditLogResponse res = new EvidenceAuditLogResponse();
            res.setId(log.getId());
            res.setEvidenceId(log.getEvidence().getId());
            res.setCaseId(log.getCaseEntity().getId());
            res.setOfficerName(log.getPoliceOfficer() != null ? log.getPoliceOfficer().getFullName() : "System");
            res.setOfficerBadge(log.getPoliceOfficer() != null ? log.getPoliceOfficer().getPoliceId() : "N/A");
            res.setAction(log.getAction());
            res.setDetails(log.getDetails());
            res.setCreatedAt(log.getCreatedAt());
            return res;
        }).collect(Collectors.toList());
    }

    private synchronized String generateEvidenceNumber(String stationCodeShort) {
        int currentYear = Year.now().getValue();
        try {
            Long nextSeq = evidenceRepository.getNextEvidenceNumberSequence();
            return String.format("EVD-%s-%d-%04d", stationCodeShort, currentYear, nextSeq);
        } catch (Exception e) {
            long fallback = System.currentTimeMillis() % 10000;
            return String.format("EVD-%s-%d-%04d", stationCodeShort, currentYear, fallback);
        }
    }

    private String extractStationShortCode(PoliceStation station) {
        if (station == null || station.getStationCode() == null) return "POL";
        String code = station.getStationCode().toUpperCase();
        if (code.contains("-")) {
            String[] parts = code.split("-");
            return parts[parts.length - 1];
        }
        return code.length() > 3 ? code.substring(0, 3) : code;
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

    private void recordAuditLog(Evidence evidence, Case parentCase, PoliceOfficer officer, String action, String details) {
        try {
            EvidenceAuditLog log = new EvidenceAuditLog(evidence, parentCase, officer, action, details);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to record evidence audit log for {}: {}", action, e.getMessage());
        }
    }

    private EvidenceResponse mapToResponse(Evidence evidence) {
        EvidenceResponse res = new EvidenceResponse();
        res.setId(evidence.getId());
        res.setCaseId(evidence.getCaseEntity().getId());
        res.setCaseNumber(evidence.getCaseEntity().getCaseNumber());
        res.setEvidenceNumber(evidence.getEvidenceNumber());
        res.setTitle(evidence.getTitle());
        res.setDescription(evidence.getDescription());
        res.setEvidenceType(evidence.getEvidenceType());
        res.setStatus(evidence.getStatus());
        res.setOriginalFileName(evidence.getOriginalFileName());
        res.setStoragePath(evidence.getStoragePath());
        res.setMimeType(evidence.getMimeType());
        res.setFileSize(evidence.getFileSize());
        res.setFileHash(evidence.getFileHash());
        res.setCollectedAt(evidence.getCollectedAt());
        res.setCollectedLocation(evidence.getCollectedLocation());
        res.setCollectedBy(evidence.getCollectedBy());
        res.setNotes(evidence.getNotes());
        res.setCategory(evidence.getCategory() != null ? evidence.getCategory() : "UPLOADED_EVIDENCE");
        res.setIsAiGenerated(Boolean.TRUE.equals(evidence.getIsAiGenerated()));
        res.setViewUrl("/api/cases/" + evidence.getCaseEntity().getId() + "/evidence/" + evidence.getId() + "/view");
        res.setCreatedById(evidence.getCreatedBy() != null ? evidence.getCreatedBy().getId() : null);
        res.setCreatedByName(evidence.getCreatedBy() != null ? evidence.getCreatedBy().getFullName() : "N/A");
        res.setCreatedAt(evidence.getCreatedAt());
        res.setUpdatedAt(evidence.getUpdatedAt());
        return res;
    }

    public static class DownloadFileInfo {
        private final Resource resource;
        private final String filename;
        private final String contentType;
        private final Long contentLength;

        public DownloadFileInfo(Resource resource, String filename, String contentType, Long contentLength) {
            this.resource = resource;
            this.filename = filename;
            this.contentType = contentType;
            this.contentLength = contentLength;
        }

        public Resource getResource() {
            return resource;
        }

        public String getFilename() {
            return filename;
        }

        public String getContentType() {
            return contentType;
        }

        public Long getContentLength() {
            return contentLength;
        }
    }
}
