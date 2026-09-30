package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.victim.CreateVictimRequest;
import com.crimesphere.backend.dto.victim.UpdateVictimRequest;
import com.crimesphere.backend.dto.victim.VictimResponse;
import com.crimesphere.backend.service.SuspectService;
import com.crimesphere.backend.service.VictimService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cases/{caseId}/victims")
public class VictimController {

    private final VictimService victimService;

    public VictimController(VictimService victimService) {
        this.victimService = victimService;
    }

    @PostMapping
    public ResponseEntity<VictimResponse> createVictim(
            @PathVariable UUID caseId,
            @RequestBody CreateVictimRequest request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        VictimResponse response = victimService.createVictim(caseId, request, policeId, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<VictimResponse>> getVictims(@PathVariable UUID caseId) {
        String policeId = getAuthenticatedPoliceId();
        List<VictimResponse> response = victimService.getVictimsByCaseId(caseId, policeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{victimId}")
    public ResponseEntity<VictimResponse> getVictimById(
            @PathVariable UUID caseId,
            @PathVariable UUID victimId
    ) {
        String policeId = getAuthenticatedPoliceId();
        VictimResponse response = victimService.getVictimById(caseId, victimId, policeId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{victimId}")
    public ResponseEntity<VictimResponse> updateVictim(
            @PathVariable UUID caseId,
            @PathVariable UUID victimId,
            @RequestBody UpdateVictimRequest request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        VictimResponse response = victimService.updateVictim(caseId, victimId, request, policeId, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload official photograph for victim/case record.
     */
    @PostMapping(value = "/{victimId}/photo", consumes = { org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<VictimResponse> uploadVictimPhoto(
            @PathVariable UUID caseId,
            @PathVariable UUID victimId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        VictimResponse response = victimService.uploadVictimPhoto(caseId, victimId, file, policeId, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * View/stream official victim photograph (or fallback to dummy image seamlessly).
     */
    @GetMapping("/{victimId}/photo")
    public ResponseEntity<org.springframework.core.io.Resource> getVictimPhoto(
            @PathVariable UUID caseId,
            @PathVariable UUID victimId
    ) {
        SuspectService.StorageServicePhotoResult result = victimService.getVictimPhotoPublic(caseId, victimId);

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(result.getContentType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + result.getFilename() + "\"")
                .body(result.getResource());
    }

    /**
     * View/stream default fictional dossier portrait for victim record.
     */
    @GetMapping("/{victimId}/default-image")
    public ResponseEntity<org.springframework.core.io.Resource> getVictimDefaultImage(
            @PathVariable UUID caseId,
            @PathVariable UUID victimId
    ) {
        SuspectService.StorageServicePhotoResult result = victimService.getVictimDefaultImagePublic(caseId, victimId);

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
