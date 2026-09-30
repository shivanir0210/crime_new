package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.suspect.CreateSuspectRequest;
import com.crimesphere.backend.dto.suspect.SuspectResponse;
import com.crimesphere.backend.dto.suspect.UpdateSuspectRequest;
import com.crimesphere.backend.service.SuspectService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cases/{caseId}/suspects")
public class SuspectController {

    private final SuspectService suspectService;

    public SuspectController(SuspectService suspectService) {
        this.suspectService = suspectService;
    }

    @PostMapping
    public ResponseEntity<SuspectResponse> createSuspect(
            @PathVariable UUID caseId,
            @RequestBody CreateSuspectRequest request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        SuspectResponse response = suspectService.createSuspect(caseId, request, policeId, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SuspectResponse>> getSuspects(@PathVariable UUID caseId) {
        String policeId = getAuthenticatedPoliceId();
        List<SuspectResponse> response = suspectService.getSuspectsByCaseId(caseId, policeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{suspectId}")
    public ResponseEntity<SuspectResponse> getSuspectById(
            @PathVariable UUID caseId,
            @PathVariable UUID suspectId
    ) {
        String policeId = getAuthenticatedPoliceId();
        SuspectResponse response = suspectService.getSuspectById(caseId, suspectId, policeId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{suspectId}")
    public ResponseEntity<SuspectResponse> updateSuspect(
            @PathVariable UUID caseId,
            @PathVariable UUID suspectId,
            @RequestBody UpdateSuspectRequest request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        SuspectResponse response = suspectService.updateSuspect(caseId, suspectId, request, policeId, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload official photograph for suspect/case record.
     */
    @PostMapping(value = "/{suspectId}/photo", consumes = { org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<SuspectResponse> uploadSuspectPhoto(
            @PathVariable UUID caseId,
            @PathVariable UUID suspectId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        SuspectResponse response = suspectService.uploadSuspectPhoto(caseId, suspectId, file, policeId, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * View/stream official suspect photograph (or fallback to dummy image seamlessly).
     */
    @GetMapping("/{suspectId}/photo")
    public ResponseEntity<org.springframework.core.io.Resource> getSuspectPhoto(
            @PathVariable UUID caseId,
            @PathVariable UUID suspectId
    ) {
        SuspectService.StorageServicePhotoResult result = suspectService.getSuspectPhotoPublic(caseId, suspectId);

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(result.getContentType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + result.getFilename() + "\"")
                .body(result.getResource());
    }

    /**
     * View/stream default fictional dossier portrait for suspect record.
     */
    @GetMapping("/{suspectId}/default-image")
    public ResponseEntity<org.springframework.core.io.Resource> getSuspectDefaultImage(
            @PathVariable UUID caseId,
            @PathVariable UUID suspectId
    ) {
        SuspectService.StorageServicePhotoResult result = suspectService.getSuspectDefaultImagePublic(caseId, suspectId);

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(result.getContentType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + result.getFilename() + "\"")
                .body(result.getResource());
    }

    private String getAuthenticatedPoliceId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.authentication.BadCredentialsException("Unauthenticated request");
        }
        return authentication.getName();
    }
}
