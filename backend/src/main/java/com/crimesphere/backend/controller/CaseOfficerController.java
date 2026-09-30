package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.CaseOfficerDto;
import com.crimesphere.backend.service.CaseOfficerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CaseOfficerController {

    private final CaseOfficerService caseOfficerService;

    public CaseOfficerController(CaseOfficerService caseOfficerService) {
        this.caseOfficerService = caseOfficerService;
    }

    /**
     * Assign an officer to a case.
     */
    @PostMapping("/cases/{caseId}/officers/{officerId}")
    public ResponseEntity<CaseOfficerDto> assignOfficerToCase(
            @PathVariable UUID caseId,
            @PathVariable UUID officerId
    ) {
        String policeId = getAuthenticatedPoliceId();
        CaseOfficerDto response = caseOfficerService.assignOfficerToCase(caseId, officerId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove/deactivate an officer from a case.
     */
    @DeleteMapping("/cases/{caseId}/officers/{officerId}")
    public ResponseEntity<Map<String, Object>> removeOfficerFromCase(
            @PathVariable UUID caseId,
            @PathVariable UUID officerId
    ) {
        String policeId = getAuthenticatedPoliceId();
        caseOfficerService.removeOfficerFromCase(caseId, officerId, policeId);
        return ResponseEntity.ok(Map.of(
                "message", "Officer removed successfully from case",
                "caseId", caseId,
                "officerId", officerId
        ));
    }

    /**
     * List officers assigned to a case.
     */
    @GetMapping("/cases/{caseId}/officers")
    public ResponseEntity<List<CaseOfficerDto>> getAssignedOfficers(
            @PathVariable UUID caseId
    ) {
        String policeId = getAuthenticatedPoliceId();
        List<CaseOfficerDto> response = caseOfficerService.getAssignedOfficers(caseId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * List station officers available to be assigned to a case.
     */
    @GetMapping("/cases/{caseId}/available-officers")
    public ResponseEntity<List<CaseOfficerDto>> getAvailableOfficers(
            @PathVariable UUID caseId
    ) {
        String policeId = getAuthenticatedPoliceId();
        List<CaseOfficerDto> response = caseOfficerService.getAvailableStationOfficers(caseId, policeId);
        return ResponseEntity.ok(response);
    }

    private String getAuthenticatedPoliceId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.authentication.BadCredentialsException("Unauthenticated request");
        }
        return authentication.getName();
    }
}
