package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.interstation.CreateInterStationRequestDto;
import com.crimesphere.backend.dto.interstation.InterStationRequestResponseDto;
import com.crimesphere.backend.dto.interstation.RespondInterStationRequestDto;
import com.crimesphere.backend.service.InterStationRequestService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inter-station-requests")
public class InterStationRequestController {

    private final InterStationRequestService interStationRequestService;

    public InterStationRequestController(InterStationRequestService interStationRequestService) {
        this.interStationRequestService = interStationRequestService;
    }

    @PostMapping
    public ResponseEntity<InterStationRequestResponseDto> createRequest(
            @RequestBody CreateInterStationRequestDto request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        InterStationRequestResponseDto response = interStationRequestService.createRequest(request, policeId, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/sent")
    public ResponseEntity<List<InterStationRequestResponseDto>> getSentRequests() {
        String policeId = getAuthenticatedPoliceId();
        List<InterStationRequestResponseDto> response = interStationRequestService.getSentRequests(policeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/received")
    public ResponseEntity<List<InterStationRequestResponseDto>> getReceivedRequests() {
        String policeId = getAuthenticatedPoliceId();
        List<InterStationRequestResponseDto> response = interStationRequestService.getReceivedRequests(policeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterStationRequestResponseDto> getRequestById(@PathVariable UUID id) {
        String policeId = getAuthenticatedPoliceId();
        InterStationRequestResponseDto response = interStationRequestService.getRequestById(id, policeId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<InterStationRequestResponseDto> acceptRequest(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        InterStationRequestResponseDto response = interStationRequestService.acceptRequest(id, policeId, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<InterStationRequestResponseDto> rejectRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) RespondInterStationRequestDto request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        InterStationRequestResponseDto response = interStationRequestService.rejectRequest(id, request, policeId, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<InterStationRequestResponseDto> respondToRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) RespondInterStationRequestDto request,
            HttpServletRequest httpRequest
    ) {
        String policeId = getAuthenticatedPoliceId();
        InterStationRequestResponseDto response = interStationRequestService.respondToRequest(id, request, policeId, httpRequest);
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
