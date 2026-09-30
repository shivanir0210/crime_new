package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.illustration.CaseIllustrationResponse;
import com.crimesphere.backend.dto.illustration.GenerateIllustrationRequest;
import com.crimesphere.backend.service.IllustrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cases/{caseId}/illustrations")
public class IllustrationController {

    private final IllustrationService illustrationService;

    public IllustrationController(IllustrationService illustrationService) {
        this.illustrationService = illustrationService;
    }

    /**
     * Generate an AI scene illustration for the case using Gemini Image Generation.
     */
    @PostMapping("/generate")
    public ResponseEntity<CaseIllustrationResponse> generateIllustration(
            @PathVariable UUID caseId,
            @RequestBody GenerateIllustrationRequest request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        CaseIllustrationResponse response = illustrationService.generateIllustration(caseId, request, policeId, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve all AI illustrations for the case.
     */
    @GetMapping
    public ResponseEntity<List<CaseIllustrationResponse>> getIllustrations(
            @PathVariable UUID caseId
    ) {
        String policeId = getAuthenticatedPoliceId();
        List<CaseIllustrationResponse> responses = illustrationService.getIllustrationsByCase(caseId, policeId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Inline image viewing endpoint for AI illustrations.
     */
    @GetMapping("/{illustrationId}/view")
    public ResponseEntity<Resource> viewIllustration(
            @PathVariable UUID caseId,
            @PathVariable UUID illustrationId
    ) {
        IllustrationService.StorageServiceFileResult fileResult = illustrationService.getIllustrationFilePublic(caseId, illustrationId);

        MediaType mediaType = MediaType.IMAGE_JPEG;
        if (fileResult.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(fileResult.getContentType());
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileResult.getFilename() + "\"")
                .body(fileResult.getResource());
    }

    /**
     * Delete an AI illustration.
     */
    @DeleteMapping("/{illustrationId}")
    public ResponseEntity<Void> deleteIllustration(
            @PathVariable UUID caseId,
            @PathVariable UUID illustrationId,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        illustrationService.deleteIllustration(caseId, illustrationId, policeId, httpRequest);
        return ResponseEntity.noContent().build();
    }

    private String getAuthenticatedPoliceId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.authentication.BadCredentialsException("Unauthenticated request");
        }
        return authentication.getName();
    }
}
