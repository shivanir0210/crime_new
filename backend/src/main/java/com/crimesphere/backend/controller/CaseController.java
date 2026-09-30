package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.case_mgmt.*;
import com.crimesphere.backend.service.CaseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping
    public ResponseEntity<CaseResponse> createCase(@RequestBody CreateCaseRequest request, HttpServletRequest httpRequest) {
        String policeId = getAuthenticatedPoliceId();
        CaseResponse response = caseService.createCase(request, policeId, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<CaseSummaryResponse>> getCases(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String caseType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort
    ) {
        String policeId = getAuthenticatedPoliceId();

        // Parse sorting
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        PageResponse<CaseSummaryResponse> response = caseService.getCases(policeId, search, status, priority, caseType, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CaseResponse> getCaseById(@PathVariable UUID id) {
        String policeId = getAuthenticatedPoliceId();
        CaseResponse response = caseService.getCaseById(id, policeId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CaseResponse> updateCase(
            @PathVariable UUID id,
            @RequestBody UpdateCaseRequest request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        CaseResponse response = caseService.updateCase(id, request, policeId, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CaseResponse> updateStatus(
            @PathVariable UUID id,
            @RequestBody UpdateCaseStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        CaseResponse response = caseService.updateStatus(id, request.getStatus(), policeId, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/priority")
    public ResponseEntity<CaseResponse> updatePriority(
            @PathVariable UUID id,
            @RequestBody UpdateCasePriorityRequest request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        CaseResponse response = caseService.updatePriority(id, request.getPriority(), policeId, httpRequest);
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
