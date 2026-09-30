package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.evidence.*;
import com.crimesphere.backend.dto.illustration.GenerateIllustrationRequest;
import com.crimesphere.backend.service.EvidenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cases/{caseId}/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;
    private final ObjectMapper objectMapper;

    public EvidenceController(EvidenceService evidenceService, ObjectMapper objectMapper) {
        this.evidenceService = evidenceService;
        this.objectMapper = objectMapper;
    }

    /**
     * Upload new evidence file with metadata, or create evidence entry with default forensic illustration.
     */
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<EvidenceResponse> uploadEvidence(
            @PathVariable UUID caseId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "metadata", required = false) String metadataJson,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "evidenceType", required = false) String evidenceType,
            @RequestParam(value = "collectedAt", required = false) String collectedAtStr,
            @RequestParam(value = "collectedLocation", required = false) String collectedLocation,
            @RequestParam(value = "collectedBy", required = false) String collectedBy,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "confirmDuplicate", required = false) Boolean confirmDuplicate
    ) {
        String policeId = getAuthenticatedPoliceId();
        CreateEvidenceMetadata metadata = new CreateEvidenceMetadata();

        if (metadataJson != null && !metadataJson.isBlank()) {
            try {
                metadata = objectMapper.readValue(metadataJson, CreateEvidenceMetadata.class);
            } catch (Exception e) {
                // Ignore JSON parse error and fallback to query params
            }
        }

        // Apply parameters if not set from JSON
        if (metadata.getTitle() == null && title != null) metadata.setTitle(title);
        if (metadata.getDescription() == null && description != null) metadata.setDescription(description);
        if (metadata.getEvidenceType() == null && evidenceType != null) metadata.setEvidenceType(evidenceType);
        if (metadata.getCollectedLocation() == null && collectedLocation != null) metadata.setCollectedLocation(collectedLocation);
        if (metadata.getCollectedBy() == null && collectedBy != null) metadata.setCollectedBy(collectedBy);
        if (metadata.getNotes() == null && notes != null) metadata.setNotes(notes);
        if (metadata.getConfirmDuplicate() == null && confirmDuplicate != null) metadata.setConfirmDuplicate(confirmDuplicate);
        if (metadata.getCollectedAt() == null && collectedAtStr != null && !collectedAtStr.isBlank()) {
            try {
                metadata.setCollectedAt(OffsetDateTime.parse(collectedAtStr));
            } catch (Exception e) {
                metadata.setCollectedAt(OffsetDateTime.now());
            }
        }

        EvidenceResponse response = evidenceService.uploadEvidence(caseId, file, metadata, policeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Upload real physical evidence file replacing default AI illustration.
     */
    @PostMapping(value = {"/{evidenceId}/replace-image", "/{evidenceId}/upload"}, consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<EvidenceResponse> replaceWithRealEvidence(
            @PathVariable UUID caseId,
            @PathVariable UUID evidenceId,
            @RequestParam("file") MultipartFile file
    ) {
        String policeId = getAuthenticatedPoliceId();
        EvidenceResponse response = evidenceService.replaceWithRealEvidenceFile(caseId, evidenceId, file, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all evidence belonging to a case.
     */
    @GetMapping
    public ResponseEntity<List<EvidenceResponse>> getEvidenceByCase(@PathVariable UUID caseId) {
        String policeId = getAuthenticatedPoliceId();
        List<EvidenceResponse> response = evidenceService.getEvidenceByCase(caseId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get evidence details by ID.
     */
    @GetMapping("/{evidenceId}")
    public ResponseEntity<EvidenceResponse> getEvidenceDetails(
            @PathVariable UUID caseId,
            @PathVariable UUID evidenceId
    ) {
        String policeId = getAuthenticatedPoliceId();
        EvidenceResponse response = evidenceService.getEvidenceDetails(caseId, evidenceId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update editable evidence metadata.
     */
    @PutMapping("/{evidenceId}")
    public ResponseEntity<EvidenceResponse> updateEvidence(
            @PathVariable UUID caseId,
            @PathVariable UUID evidenceId,
            @RequestBody UpdateEvidenceRequest request
    ) {
        String policeId = getAuthenticatedPoliceId();
        EvidenceResponse response = evidenceService.updateEvidence(caseId, evidenceId, request, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update evidence status.
     */
    @PatchMapping("/{evidenceId}/status")
    public ResponseEntity<EvidenceResponse> updateEvidenceStatus(
            @PathVariable UUID caseId,
            @PathVariable UUID evidenceId,
            @RequestBody UpdateEvidenceStatusRequest request
    ) {
        String policeId = getAuthenticatedPoliceId();
        EvidenceResponse response = evidenceService.updateEvidenceStatus(caseId, evidenceId, request, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Securely download or view evidence file.
     */
    @GetMapping("/{evidenceId}/download")
    public ResponseEntity<Resource> downloadEvidence(
            @PathVariable UUID caseId,
            @PathVariable UUID evidenceId
    ) {
        EvidenceService.DownloadFileInfo fileInfo = evidenceService.downloadEvidenceFilePublic(caseId, evidenceId);

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (fileInfo.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(fileInfo.getContentType());
            } catch (Exception ignored) {
            }
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileInfo.getFilename() + "\"")
                .body(fileInfo.getResource());
    }

    /**
     * Inline image/file viewing endpoint for preview.
     */
    @GetMapping("/{evidenceId}/view")
    public ResponseEntity<Resource> viewEvidence(
            @PathVariable UUID caseId,
            @PathVariable UUID evidenceId
    ) {
        EvidenceService.DownloadFileInfo fileInfo = evidenceService.downloadEvidenceFilePublic(caseId, evidenceId);

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (fileInfo.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(fileInfo.getContentType());
            } catch (Exception ignored) {
            }
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileInfo.getFilename() + "\"")
                .body(fileInfo.getResource());
    }

    /**
     * Generate an AI scene illustration for a specific evidence item.
     */
    @PostMapping("/{evidenceId}/illustration")
    public ResponseEntity<EvidenceResponse> generateAiIllustration(
            @PathVariable UUID caseId,
            @PathVariable UUID evidenceId,
            @RequestBody GenerateIllustrationRequest request
    ) {
        String policeId = getAuthenticatedPoliceId();
        EvidenceResponse response = evidenceService.generateAiIllustrationForEvidence(
                caseId,
                evidenceId,
                request.getPrompt(),
                policeId
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get audit trail for a specific evidence item.
     */
    @GetMapping("/{evidenceId}/audit")
    public ResponseEntity<List<EvidenceAuditLogResponse>> getEvidenceAuditLogs(
            @PathVariable UUID caseId,
            @PathVariable UUID evidenceId
    ) {
        String policeId = getAuthenticatedPoliceId();
        List<EvidenceAuditLogResponse> logs = evidenceService.getEvidenceAuditLogs(caseId, evidenceId, policeId);
        return ResponseEntity.ok(logs);
    }

    private String getAuthenticatedPoliceId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.authentication.BadCredentialsException("Unauthenticated request");
        }
        return authentication.getName();
    }
}
