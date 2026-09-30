package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.timeline.TimelineEventDTO;
import com.crimesphere.backend.service.CaseTimelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cases/{caseId}/timeline")
public class CaseTimelineController {

    private final CaseTimelineService timelineService;

    public CaseTimelineController(CaseTimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping
    public ResponseEntity<List<TimelineEventDTO>> getCaseTimeline(@PathVariable UUID caseId) {
        String policeId = getAuthenticatedPoliceId();
        List<TimelineEventDTO> response = timelineService.getCaseTimeline(caseId, policeId);
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
