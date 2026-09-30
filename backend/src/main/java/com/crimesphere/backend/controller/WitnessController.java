package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.witness.CreateWitnessRequest;
import com.crimesphere.backend.dto.witness.UpdateWitnessRequest;
import com.crimesphere.backend.dto.witness.WitnessResponse;
import com.crimesphere.backend.service.WitnessService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cases/{caseId}/witnesses")
public class WitnessController {

    private final WitnessService witnessService;

    public WitnessController(WitnessService witnessService) {
        this.witnessService = witnessService;
    }

    @PostMapping
    public ResponseEntity<WitnessResponse> createWitness(
            @PathVariable UUID caseId,
            @RequestBody CreateWitnessRequest request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        WitnessResponse response = witnessService.createWitness(caseId, request, policeId, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WitnessResponse>> getWitnesses(@PathVariable UUID caseId) {
        String policeId = getAuthenticatedPoliceId();
        List<WitnessResponse> response = witnessService.getWitnessesByCaseId(caseId, policeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{witnessId}")
    public ResponseEntity<WitnessResponse> getWitnessById(
            @PathVariable UUID caseId,
            @PathVariable UUID witnessId
    ) {
        String policeId = getAuthenticatedPoliceId();
        WitnessResponse response = witnessService.getWitnessById(caseId, witnessId, policeId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{witnessId}")
    public ResponseEntity<WitnessResponse> updateWitness(
            @PathVariable UUID caseId,
            @PathVariable UUID witnessId,
            @RequestBody UpdateWitnessRequest request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        WitnessResponse response = witnessService.updateWitness(caseId, witnessId, request, policeId, httpRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{witnessId}")
    public ResponseEntity<Void> deleteWitness(
            @PathVariable UUID caseId,
            @PathVariable UUID witnessId,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        witnessService.deleteWitness(caseId, witnessId, policeId, httpRequest);
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
