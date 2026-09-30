package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.CaseReportSummaryDTO;
import com.crimesphere.backend.entity.CaseReportGeneration;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import com.crimesphere.backend.service.CaseReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cases/{caseId}/report")
public class CaseReportController {

    private final CaseReportService caseReportService;
    private final PoliceOfficerRepository officerRepository;

    public CaseReportController(CaseReportService caseReportService, PoliceOfficerRepository officerRepository) {
        this.caseReportService = caseReportService;
        this.officerRepository = officerRepository;
    }

    /**
     * GET /api/cases/{caseId}/report/pdf
     * Generates and downloads the official PDF investigation case report.
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdfReport(@PathVariable UUID caseId) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        byte[] pdfBytes = caseReportService.generateCaseReportPdf(caseId, officer.getPoliceId());

        CaseReportSummaryDTO summary = caseReportService.getReportSummary(caseId, officer.getPoliceId());
        String fileName = summary.getReportFileName() != null ? summary.getReportFileName() : "case-report.pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * GET /api/cases/{caseId}/report/summary
     * Optional endpoint returning the structured report data summary in JSON format.
     */
    @GetMapping("/summary")
    public ResponseEntity<CaseReportSummaryDTO> getReportSummary(@PathVariable UUID caseId) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        CaseReportSummaryDTO summary = caseReportService.getReportSummary(caseId, officer.getPoliceId());
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/cases/{caseId}/report/history
     * Returns the report generation audit history for this case.
     */
    @GetMapping("/history")
    public ResponseEntity<List<CaseReportGeneration>> getReportHistory(@PathVariable UUID caseId) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        List<CaseReportGeneration> history = caseReportService.getReportHistory(caseId, officer.getPoliceId());
        return ResponseEntity.ok(history);
    }

    private PoliceOfficer getAuthenticatedOfficer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BadCredentialsException("Unauthenticated request. Valid officer JWT required.");
        }
        String policeId = auth.getName();
        return officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new BadCredentialsException("Officer profile not found for Police ID: " + policeId));
    }
}
