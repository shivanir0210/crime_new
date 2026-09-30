package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.CaseReportSummaryDTO;
import com.crimesphere.backend.entity.AuditLog;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.CaseReportGeneration;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.CaseReportGenerationRepository;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CaseReportService {

    private static final Logger logger = LoggerFactory.getLogger(CaseReportService.class);

    private final CaseReportDataBuilder dataBuilder;
    private final PdfReportGenerator pdfGenerator;
    private final CaseReportGenerationRepository reportGenerationRepository;
    private final PoliceOfficerRepository officerRepository;
    private final CaseRepository caseRepository;
    private final AuditLogRepository auditLogRepository;

    public CaseReportService(CaseReportDataBuilder dataBuilder,
                             PdfReportGenerator pdfGenerator,
                             CaseReportGenerationRepository reportGenerationRepository,
                             PoliceOfficerRepository officerRepository,
                             CaseRepository caseRepository,
                             AuditLogRepository auditLogRepository) {
        this.dataBuilder = dataBuilder;
        this.pdfGenerator = pdfGenerator;
        this.reportGenerationRepository = reportGenerationRepository;
        this.officerRepository = officerRepository;
        this.caseRepository = caseRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public byte[] generateCaseReportPdf(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        CaseReportSummaryDTO reportData = dataBuilder.buildReportData(caseId, officer);

        byte[] pdfBytes = pdfGenerator.generatePdfReport(reportData);

        // Record CaseReportGeneration audit history
        Case crimeCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + caseId));

        CaseReportGeneration generationRecord = new CaseReportGeneration(
                null,
                crimeCase,
                officer,
                reportData.getReportVersion(),
                reportData.getReportFileName()
        );
        reportGenerationRepository.save(generationRecord);

        // Audit Log
        recordAuditLog(officer, "CASE_REPORT_GENERATED", "SUCCESS",
                "Generated PDF Report version " + reportData.getReportVersion() + " (" + pdfBytes.length + " bytes) for case " + crimeCase.getCaseNumber());

        logger.info("Case report PDF generated successfully for case {} by officer TN-POL-{}", crimeCase.getCaseNumber(), officer.getPoliceId());

        return pdfBytes;
    }

    @Transactional(readOnly = true)
    public CaseReportSummaryDTO getReportSummary(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        return dataBuilder.buildReportData(caseId, officer);
    }

    @Transactional(readOnly = true)
    public List<CaseReportGeneration> getReportHistory(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case crimeCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + caseId));

        if (officer.getStation() != null && crimeCase.getStation() != null &&
                !officer.getStation().getId().equals(crimeCase.getStation().getId())) {
            throw new AccessDeniedException("Access Denied: Officer is not authorized to view report history for cases outside assigned station.");
        }

        return reportGenerationRepository.findByCrimeCase_IdOrderByCreatedAtDesc(caseId);
    }

    private PoliceOfficer getAuthenticatedOfficer(String policeId) {
        return officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated officer profile not found for Police ID: " + policeId));
    }

    private void recordAuditLog(PoliceOfficer officer, String action, String status, String details) {
        try {
            AuditLog log = new AuditLog(null, officer, action, status, null, details);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to record audit log for report generation: {}", e.getMessage());
        }
    }
}
