package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.dashboard.DashboardStatsResponse;
import com.crimesphere.backend.dto.dashboard.DashboardStatsResponse.AttentionCaseDto;
import com.crimesphere.backend.dto.dashboard.DashboardStatsResponse.RecentActivityDto;
import com.crimesphere.backend.entity.AuditLog;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.entity.PoliceStation;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.EvidenceGapRepository;
import com.crimesphere.backend.repository.EvidenceRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {

    private final CaseRepository caseRepository;
    private final PoliceOfficerRepository officerRepository;
    private EvidenceRepository evidenceRepository;
    private EvidenceGapRepository evidenceGapRepository;
    private AuditLogRepository auditLogRepository;

    public DashboardService(CaseRepository caseRepository, PoliceOfficerRepository officerRepository) {
        this.caseRepository = caseRepository;
        this.officerRepository = officerRepository;
    }

    @Autowired
    public DashboardService(
            CaseRepository caseRepository,
            PoliceOfficerRepository officerRepository,
            EvidenceRepository evidenceRepository,
            EvidenceGapRepository evidenceGapRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.caseRepository = caseRepository;
        this.officerRepository = officerRepository;
        this.evidenceRepository = evidenceRepository;
        this.evidenceGapRepository = evidenceGapRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(String policeId) {
        PoliceOfficer officer = officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated police officer record not found"));

        PoliceStation station = officer.getStation();
        if (station == null) {
            throw new IllegalStateException("Police officer is not assigned to any station");
        }

        UUID stationId = station.getId();

        long activeCases = caseRepository.countByStationIdAndStatusNot(stationId, "CLOSED");
        long highPriorityCases = caseRepository.countByStationIdAndPriorityAndStatusNot(stationId, "HIGH", "CLOSED");
        long criticalCases = caseRepository.countByStationIdAndPriorityAndStatusNot(stationId, "CRITICAL", "CLOSED");
        long pendingReview = caseRepository.countByStationIdAndStatus(stationId, "PENDING_REVIEW");
        long onHoldCases = caseRepository.countByStationIdAndStatus(stationId, "ON_HOLD");
        long totalCases = caseRepository.countByStationId(stationId);

        DashboardStatsResponse response = new DashboardStatsResponse(
                activeCases,
                highPriorityCases,
                criticalCases,
                pendingReview,
                onHoldCases,
                totalCases,
                station.getStationName(),
                station.getStationCode()
        );

        // Case Workload Breakdown
        long underInvestigation = caseRepository.countByStationIdAndStatus(stationId, "UNDER_INVESTIGATION");
        long filed = caseRepository.countByStationIdAndStatus(stationId, "FILED");
        long closed = caseRepository.countByStationIdAndStatus(stationId, "CLOSED");
        response.setUnderInvestigationCases(underInvestigation);
        response.setFiledCases(filed);
        response.setClosedCases(closed);

        // Evidence Overview Breakdown
        if (evidenceRepository != null) {
            long totalEv = evidenceRepository.countByStationId(stationId);
            long verifiedEv = evidenceRepository.countByStationIdAndStatus(stationId, "VERIFIED");
            long pendingEv = evidenceRepository.countByStationIdAndStatus(stationId, "PENDING");
            response.setTotalEvidence(totalEv);
            response.setVerifiedEvidence(verifiedEv);
            response.setPendingEvidence(pendingEv);
        }

        if (evidenceGapRepository != null) {
            long openGaps = evidenceGapRepository.countByStationIdAndStatus(stationId, "OPEN");
            response.setEvidenceGaps(openGaps);
        }

        // Recent Activity
        if (auditLogRepository != null) {
            try {
                List<AuditLog> recentLogs = auditLogRepository.findRecentByStationId(stationId, PageRequest.of(0, 8));
                List<RecentActivityDto> activityDtos = new ArrayList<>();
                for (AuditLog log : recentLogs) {
                    String actorName = log.getPoliceOfficer() != null ? log.getPoliceOfficer().getFullName() : "Officer";
                    String actorBadge = log.getPoliceOfficer() != null ? log.getPoliceOfficer().getPoliceId() : "";
                    String action = log.getAction();
                    String details = log.getUserAgent() != null && !log.getUserAgent().isEmpty() ? log.getUserAgent() : log.getAction();
                    activityDtos.add(new RecentActivityDto(
                            log.getId(),
                            action,
                            actorName,
                            actorBadge,
                            details,
                            log.getStatus(),
                            log.getCreatedAt()
                    ));
                }
                response.setRecentActivities(activityDtos);
            } catch (Exception e) {
                // Ignore gracefully if logs query fails
            }
        }

        // Cases Requiring Attention (Critical, Pending Review, High Priority)
        try {
            List<Case> attentionCaseEntities = caseRepository.findByStationIdAndPriorityOrStatus(
                    stationId, "CRITICAL", "PENDING_REVIEW", PageRequest.of(0, 6)
            );
            List<AttentionCaseDto> attentionDtos = new ArrayList<>();
            for (Case c : attentionCaseEntities) {
                String reason = "CRITICAL".equalsIgnoreCase(c.getPriority())
                        ? "Critical emergency priority investigation"
                        : "PENDING_REVIEW".equalsIgnoreCase(c.getStatus())
                        ? "Case file awaiting supervisory sign-off"
                        : "High priority action required";

                attentionDtos.add(new AttentionCaseDto(
                        c.getId(),
                        c.getCaseNumber(),
                        c.getTitle(),
                        c.getPriority(),
                        c.getStatus(),
                        reason,
                        c.getUpdatedAt() != null ? c.getUpdatedAt() : c.getCreatedAt()
                ));
            }
            response.setAttentionCases(attentionDtos);
        } catch (Exception e) {
            // fallback
        }

        return response;
    }
}
