package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.fir.CreateFIRRequest;
import com.crimesphere.backend.dto.fir.FIRResponse;
import com.crimesphere.backend.dto.fir.UpdateFIRRequest;
import com.crimesphere.backend.service.FIRService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cases/{caseId}/fir")
public class FIRController {

    private final FIRService firService;

    public FIRController(FIRService firService) {
        this.firService = firService;
    }

    @PostMapping
    public ResponseEntity<FIRResponse> createFIR(
            @PathVariable UUID caseId,
            @RequestBody CreateFIRRequest request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        FIRResponse response = firService.createFIR(caseId, request, policeId, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<FIRResponse> getFIRByCaseId(@PathVariable UUID caseId) {
        String policeId = getAuthenticatedPoliceId();
        FIRResponse response = firService.getFIRByCaseId(caseId, policeId);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<FIRResponse> updateFIR(
            @PathVariable UUID caseId,
            @RequestBody UpdateFIRRequest request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        FIRResponse response = firService.updateFIR(caseId, request, policeId, httpRequest);
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
