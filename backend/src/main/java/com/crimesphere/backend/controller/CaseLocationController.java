package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.location.CaseLocationDTO;
import com.crimesphere.backend.dto.location.CreateCaseLocationRequest;
import com.crimesphere.backend.dto.location.LocationIntelligenceSummaryDTO;
import com.crimesphere.backend.service.CaseLocationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cases/{caseId}/locations")
public class CaseLocationController {

    private final CaseLocationService caseLocationService;

    public CaseLocationController(CaseLocationService caseLocationService) {
        this.caseLocationService = caseLocationService;
    }

    @GetMapping
    public ResponseEntity<LocationIntelligenceSummaryDTO> getLocationIntelligence(@PathVariable UUID caseId) {
        String policeId = getAuthenticatedPoliceId();
        LocationIntelligenceSummaryDTO response = caseLocationService.getLocationIntelligence(caseId, policeId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CaseLocationDTO> createLocation(
            @PathVariable UUID caseId,
            @RequestBody CreateCaseLocationRequest request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        CaseLocationDTO response = caseLocationService.createLocation(caseId, request, policeId, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> deleteLocation(
            @PathVariable UUID caseId,
            @PathVariable UUID locationId,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        caseLocationService.deleteLocation(caseId, locationId, policeId, httpRequest);
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
