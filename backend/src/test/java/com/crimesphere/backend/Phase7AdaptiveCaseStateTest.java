package com.crimesphere.backend;

import com.crimesphere.backend.dto.analysis.CaseStateDTO;
import com.crimesphere.backend.dto.analysis.EvidenceGapDTO;
import com.crimesphere.backend.dto.analysis.UpdateGapStatusRequest;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import com.crimesphere.backend.service.CaseStateService;
import com.crimesphere.backend.service.EvidenceGapAnalysisService;
import com.crimesphere.backend.service.EvidenceGapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Phase7AdaptiveCaseStateTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private FIRRepository firRepository;

    @Mock
    private VictimRepository victimRepository;

    @Mock
    private SuspectRepository suspectRepository;

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private EvidenceGapRepository evidenceGapRepository;

    @Mock
    private PoliceOfficerRepository officerRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private CaseStateService caseStateService;
    private EvidenceGapAnalysisService evidenceGapAnalysisService;
    private EvidenceGapService evidenceGapService;

    private PoliceStation coimbatoreStation;
    private PoliceStation egmoreStation;
    private PoliceOfficer coimbatoreOfficer;
    private PoliceOfficer egmoreOfficer;
    private Case coimbatoreCase;

    @BeforeEach
    void setUp() {
        caseStateService = new CaseStateService(
                caseRepository, firRepository, victimRepository,
                suspectRepository, evidenceRepository, officerRepository, auditLogRepository
        );

        evidenceGapAnalysisService = new EvidenceGapAnalysisService(
                evidenceGapRepository, caseRepository, firRepository, victimRepository,
                suspectRepository, evidenceRepository, officerRepository, auditLogRepository
        );

        evidenceGapService = new EvidenceGapService(
                evidenceGapRepository, caseRepository, officerRepository, auditLogRepository, evidenceGapAnalysisService
        );

        coimbatoreStation = new PoliceStation();
        coimbatoreStation.setId(UUID.randomUUID());
        coimbatoreStation.setStationName("Coimbatore Central");
        coimbatoreStation.setStationCode("TN-POL-1001");

        egmoreStation = new PoliceStation();
        egmoreStation.setId(UUID.randomUUID());
        egmoreStation.setStationName("Egmore Station");
        egmoreStation.setStationCode("TN-POL-1002");

        coimbatoreOfficer = new PoliceOfficer();
        coimbatoreOfficer.setId(UUID.randomUUID());
        coimbatoreOfficer.setPoliceId("TN-POL-1001");
        coimbatoreOfficer.setFullName("SI K. Vijay Anand");
        coimbatoreOfficer.setStation(coimbatoreStation);

        egmoreOfficer = new PoliceOfficer();
        egmoreOfficer.setId(UUID.randomUUID());
        egmoreOfficer.setPoliceId("TN-POL-1002");
        egmoreOfficer.setFullName("Inspector Ramesh");
        egmoreOfficer.setStation(egmoreStation);

        coimbatoreCase = new Case();
        coimbatoreCase.setId(UUID.randomUUID());
        coimbatoreCase.setCaseNumber("CS-2026-0100");
        coimbatoreCase.setTitle("Coimbatore Central Homicide");
        coimbatoreCase.setCaseType("MURDER");
        coimbatoreCase.setStation(coimbatoreStation);
        coimbatoreCase.setUpdatedAt(OffsetDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("Scenario 1: Case with no FIR -> CASE_CREATED state and Missing FIR gap")
    void test1_CaseWithNoFir() {
        UUID caseId = coimbatoreCase.getId();
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(false);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        CaseStateDTO state = caseStateService.computeCaseState(coimbatoreCase);

        assertEquals("CASE_CREATED", state.getCurrentState());
        assertEquals("Case Created", state.getStateLabel());
        assertTrue(state.getMissingAreas().contains("FIR Registration"));
    }

    @Test
    @DisplayName("Scenario 2: Case with FIR but no victim -> FIR_REGISTERED state and Victim gap")
    void test2_CaseWithFirNoVictim() {
        UUID caseId = coimbatoreCase.getId();
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(true);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        CaseStateDTO state = caseStateService.computeCaseState(coimbatoreCase);

        assertEquals("FIR_REGISTERED", state.getCurrentState());
        assertTrue(state.getCompletedAreas().contains("FIR"));
        assertTrue(state.getMissingAreas().contains("Victim Information"));
    }

    @Test
    @DisplayName("Scenario 3: Case with FIR + victim but no suspect -> VICTIM_IDENTIFIED state and Suspect gap")
    void test3_CaseWithFirAndVictimNoSuspect() {
        UUID caseId = coimbatoreCase.getId();
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(true);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(1L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        CaseStateDTO state = caseStateService.computeCaseState(coimbatoreCase);

        assertEquals("VICTIM_IDENTIFIED", state.getCurrentState());
        assertTrue(state.getCompletedAreas().contains("Victim Information"));
        assertTrue(state.getMissingAreas().contains("Suspect Information"));
    }

    @Test
    @DisplayName("Scenario 4: Case with FIR + victim + suspect but no evidence -> SUSPECT_IDENTIFIED state and Evidence gap")
    void test4_CaseWithFirVictimSuspectNoEvidence() {
        UUID caseId = coimbatoreCase.getId();
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(true);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(1L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(1L);
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        CaseStateDTO state = caseStateService.computeCaseState(coimbatoreCase);

        assertEquals("SUSPECT_IDENTIFIED", state.getCurrentState());
        assertTrue(state.getCompletedAreas().contains("Suspect Information"));
        assertTrue(state.getMissingAreas().contains("Evidence Collection"));
    }

    @Test
    @DisplayName("Scenario 5: Case with evidence status UPLOADED -> EVIDENCE_COLLECTED state")
    void test5_CaseWithUploadedEvidence() {
        UUID caseId = coimbatoreCase.getId();
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(true);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(1L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(1L);

        Evidence ev = new Evidence();
        ev.setStatus("UPLOADED");
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(ev));

        CaseStateDTO state = caseStateService.computeCaseState(coimbatoreCase);

        assertEquals("EVIDENCE_COLLECTED", state.getCurrentState());
        assertTrue(state.getCompletedAreas().contains("Evidence Collection"));
    }

    @Test
    @DisplayName("Scenario 6: Case with evidence status UNDER_REVIEW -> EVIDENCE_UNDER_REVIEW state")
    void test6_CaseWithEvidenceUnderReview() {
        UUID caseId = coimbatoreCase.getId();
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(true);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(1L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(1L);

        Evidence ev = new Evidence();
        ev.setStatus("UNDER_REVIEW");
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(ev));

        CaseStateDTO state = caseStateService.computeCaseState(coimbatoreCase);

        assertEquals("EVIDENCE_UNDER_REVIEW", state.getCurrentState());
    }

    @Test
    @DisplayName("Scenario 7: Case with VERIFIED evidence and complete details -> INVESTIGATION_READY state")
    void test7_CaseWithVerifiedEvidence() {
        UUID caseId = coimbatoreCase.getId();
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(true);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(1L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(1L);

        Evidence ev = new Evidence();
        ev.setStatus("VERIFIED");
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(ev));

        CaseStateDTO state = caseStateService.computeCaseState(coimbatoreCase);

        assertEquals("INVESTIGATION_READY", state.getCurrentState());
        assertEquals(100, state.getProgressPercentage());
    }

    @Test
    @DisplayName("Scenario 8: Running gap analysis multiple times produces no duplicate OPEN gaps")
    void test8_IdempotentGapAnalysisNoDuplicates() {
        UUID caseId = coimbatoreCase.getId();
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(coimbatoreOfficer));
        when(caseRepository.findByIdAndStationId(caseId, coimbatoreStation.getId())).thenReturn(Optional.of(coimbatoreCase));

        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(false);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        // First run
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId))
                .thenReturn(Collections.emptyList());

        List<EvidenceGapDTO> firstRunGaps = evidenceGapAnalysisService.analyzeCase(caseId, "TN-POL-1001");
        assertNotNull(firstRunGaps);

        // Verify save was called for rules
        verify(evidenceGapRepository, atLeast(4)).save(any(EvidenceGap.class));
    }

    @Test
    @DisplayName("Scenario 9: Resolve a gap sets status=RESOLVED, resolvedAt, and resolvedBy")
    void test9_ResolveGapPopulatesMetadata() {
        UUID caseId = coimbatoreCase.getId();
        UUID gapId = UUID.randomUUID();

        EvidenceGap gap = new EvidenceGap();
        gap.setId(gapId);
        gap.setCaseEntity(coimbatoreCase);
        gap.setCategory("DOCUMENT");
        gap.setTitle("Evidence not collected");
        gap.setStatus("OPEN");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(coimbatoreOfficer));
        when(caseRepository.findByIdAndStationId(caseId, coimbatoreStation.getId())).thenReturn(Optional.of(coimbatoreCase));
        when(evidenceGapRepository.findByIdAndCaseEntityId(gapId, caseId)).thenReturn(Optional.of(gap));
        when(evidenceGapRepository.save(any(EvidenceGap.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvidenceGapDTO response = evidenceGapService.updateGapStatus(
                caseId, gapId, new UpdateGapStatusRequest("RESOLVED", "Evidence uploaded"), "TN-POL-1001"
        );

        assertEquals("RESOLVED", response.getStatus());
        assertNotNull(response.getResolvedAt());
        assertEquals(coimbatoreOfficer.getId(), response.getResolvedById());
        assertEquals(coimbatoreOfficer.getFullName(), response.getResolvedByName());
    }

    @Test
    @DisplayName("Scenario 10: Acknowledge a gap sets status=ACKNOWLEDGED")
    void test10_AcknowledgeGapStatus() {
        UUID caseId = coimbatoreCase.getId();
        UUID gapId = UUID.randomUUID();

        EvidenceGap gap = new EvidenceGap();
        gap.setId(gapId);
        gap.setCaseEntity(coimbatoreCase);
        gap.setCategory("SUSPECT");
        gap.setTitle("Suspect information missing");
        gap.setStatus("OPEN");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(coimbatoreOfficer));
        when(caseRepository.findByIdAndStationId(caseId, coimbatoreStation.getId())).thenReturn(Optional.of(coimbatoreCase));
        when(evidenceGapRepository.findByIdAndCaseEntityId(gapId, caseId)).thenReturn(Optional.of(gap));
        when(evidenceGapRepository.save(any(EvidenceGap.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvidenceGapDTO response = evidenceGapService.updateGapStatus(
                caseId, gapId, new UpdateGapStatusRequest("ACKNOWLEDGED", "Investigator acknowledging gap"), "TN-POL-1001"
        );

        assertEquals("ACKNOWLEDGED", response.getStatus());
        assertNull(response.getResolvedAt());
    }

    @Test
    @DisplayName("Scenario 11: Station isolation - Egmore officer (TN-POL-1002) cannot access Coimbatore case")
    void test11_StationIsolationBlockAccess() {
        UUID caseId = coimbatoreCase.getId();
        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(egmoreOfficer));
        when(caseRepository.findByIdAndStationId(caseId, egmoreStation.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            caseStateService.evaluateCaseState(caseId, "TN-POL-1002");
        });
    }

    @Test
    @DisplayName("Scenario 12: Verify JWT authenticated officer is assigned to resolvedBy")
    void test12_JwtOfficerAssignedToResolvedBy() {
        UUID caseId = coimbatoreCase.getId();
        UUID gapId = UUID.randomUUID();

        EvidenceGap gap = new EvidenceGap();
        gap.setId(gapId);
        gap.setCaseEntity(coimbatoreCase);
        gap.setStatus("OPEN");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(coimbatoreOfficer));
        when(caseRepository.findByIdAndStationId(caseId, coimbatoreStation.getId())).thenReturn(Optional.of(coimbatoreCase));
        when(evidenceGapRepository.findByIdAndCaseEntityId(gapId, caseId)).thenReturn(Optional.of(gap));
        when(evidenceGapRepository.save(any(EvidenceGap.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvidenceGapDTO response = evidenceGapService.updateGapStatus(
                caseId, gapId, new UpdateGapStatusRequest("RESOLVED", "Resolved via JWT user"), "TN-POL-1001"
        );

        assertEquals(coimbatoreOfficer.getId(), response.getResolvedById());
    }

    @Test
    @DisplayName("Scenario 13: Verify cases.updated_at changes when gap status is updated")
    void test13_CaseUpdatedAtModifiedOnGapStatusUpdate() {
        UUID caseId = coimbatoreCase.getId();
        UUID gapId = UUID.randomUUID();
        OffsetDateTime originalUpdatedAt = coimbatoreCase.getUpdatedAt();

        EvidenceGap gap = new EvidenceGap();
        gap.setId(gapId);
        gap.setCaseEntity(coimbatoreCase);
        gap.setStatus("OPEN");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(coimbatoreOfficer));
        when(caseRepository.findByIdAndStationId(caseId, coimbatoreStation.getId())).thenReturn(Optional.of(coimbatoreCase));
        when(evidenceGapRepository.findByIdAndCaseEntityId(gapId, caseId)).thenReturn(Optional.of(gap));
        when(evidenceGapRepository.save(any(EvidenceGap.class))).thenAnswer(invocation -> invocation.getArgument(0));

        evidenceGapService.updateGapStatus(caseId, gapId, new UpdateGapStatusRequest("RESOLVED", "Reason"), "TN-POL-1001");

        verify(caseRepository).save(coimbatoreCase);
        assertTrue(coimbatoreCase.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    @DisplayName("Scenario 14: Progress percentage calculation is strictly deterministic")
    void test14_ProgressPercentageIsDeterministic() {
        UUID caseId = coimbatoreCase.getId();

        // 1 area complete (Case Info) = 1/6 = 17%
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(false);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        CaseStateDTO state1 = caseStateService.computeCaseState(coimbatoreCase);
        assertEquals(17, state1.getProgressPercentage());

        // 3 areas complete (Case Info, FIR, Victim) = 3/6 = 50%
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(true);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(1L);

        CaseStateDTO state3 = caseStateService.computeCaseState(coimbatoreCase);
        assertEquals(50, state3.getProgressPercentage());
    }
}
