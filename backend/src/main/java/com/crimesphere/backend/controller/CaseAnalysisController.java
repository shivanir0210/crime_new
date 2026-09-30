package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.analysis.CaseStateDTO;
import com.crimesphere.backend.dto.analysis.EvidenceGapDTO;
import com.crimesphere.backend.dto.analysis.UpdateGapStatusRequest;
import com.crimesphere.backend.service.CaseStateService;
import com.crimesphere.backend.service.EvidenceGapService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class CaseAnalysisController {

    private final CaseStateService caseStateService;
    private final EvidenceGapService evidenceGapService;

    public CaseAnalysisController(CaseStateService caseStateService, EvidenceGapService evidenceGapService) {
        this.caseStateService = caseStateService;
        this.evidenceGapService = evidenceGapService;
    }

    /**
     * Get current dynamic investigation state and completeness metrics for a case.
     */
    @GetMapping({"/api/cases/{caseId}/state", "/api/cases/{caseId}/analysis/state"})
    public ResponseEntity<CaseStateDTO> getCaseState(@PathVariable UUID caseId) {
        String policeId = getAuthenticatedPoliceId();
        CaseStateDTO response = caseStateService.evaluateCaseState(caseId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all detected and recorded evidence gaps for a case.
     */
    @GetMapping({"/api/cases/{caseId}/evidence-gaps", "/api/cases/{caseId}/analysis/gaps"})
    public ResponseEntity<List<EvidenceGapDTO>> getEvidenceGaps(@PathVariable UUID caseId) {
        String policeId = getAuthenticatedPoliceId();
        List<EvidenceGapDTO> response = evidenceGapService.getGapsByCase(caseId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Run deterministic, rule-based evidence gap analysis on current database state.
     */
    @PostMapping({"/api/cases/{caseId}/evidence-gaps/analyze", "/api/cases/{caseId}/analysis/gaps/analyze"})
    public ResponseEntity<List<EvidenceGapDTO>> analyzeEvidenceGaps(@PathVariable UUID caseId) {
        String policeId = getAuthenticatedPoliceId();
        List<EvidenceGapDTO> response = evidenceGapService.runGapAnalysis(caseId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update the status of a specific evidence gap (OPEN, ACKNOWLEDGED, RESOLVED, NOT_APPLICABLE).
     */
    @PatchMapping({"/api/cases/{caseId}/evidence-gaps/{gapId}/status", "/api/cases/{caseId}/analysis/gaps/{gapId}/status"})
    public ResponseEntity<EvidenceGapDTO> updateGapStatus(
            @PathVariable UUID caseId,
            @PathVariable UUID gapId,
            @RequestBody UpdateGapStatusRequest request
    ) {
        String policeId = getAuthenticatedPoliceId();
        EvidenceGapDTO response = evidenceGapService.updateGapStatus(caseId, gapId, request, policeId);
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
