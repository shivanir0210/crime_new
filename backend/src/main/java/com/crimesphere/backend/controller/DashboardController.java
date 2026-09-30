package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.dashboard.DashboardStatsResponse;
import com.crimesphere.backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String policeId = authentication != null ? authentication.getName() : null;
        DashboardStatsResponse stats = dashboardService.getDashboardStats(policeId);
        return ResponseEntity.ok(stats);
    }
}
