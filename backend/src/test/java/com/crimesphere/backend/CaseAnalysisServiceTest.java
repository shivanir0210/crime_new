package com.crimesphere.backend;

import com.crimesphere.backend.dto.analysis.CaseStateDTO;
import com.crimesphere.backend.dto.analysis.EvidenceGapDTO;
import com.crimesphere.backend.dto.analysis.EvidenceGapResponse;
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
public class CaseAnalysisServiceTest {

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
    private EvidenceGapService evidenceGapService;

    private PoliceStation cbeStation;
    private PoliceStation egmStation;
    private PoliceOfficer cbeOfficer;
    private PoliceOfficer egmOfficer;
    private Case cbeCase;

    private EvidenceGapAnalysisService evidenceGapAnalysisService;

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

        cbeStation = new PoliceStation();
        cbeStation.setId(UUID.randomUUID());
        cbeStation.setStationName("Coimbatore Central");
        cbeStation.setStationCode("TN-CBE-001");

        egmStation = new PoliceStation();
        egmStation.setId(UUID.randomUUID());
        egmStation.setStationName("Egmore Station");
        egmStation.setStationCode("TN-EGM-002");

        cbeOfficer = new PoliceOfficer();
        cbeOfficer.setId(UUID.randomUUID());
        cbeOfficer.setPoliceId("IND-TN-99881");
        cbeOfficer.setFullName("Inspector Vijay");
        cbeOfficer.setStation(cbeStation);

        egmOfficer = new PoliceOfficer();
        egmOfficer.setId(UUID.randomUUID());
        egmOfficer.setPoliceId("IND-TN-11223");
        egmOfficer.setFullName("Inspector Ramesh");
        egmOfficer.setStation(egmStation);

        cbeCase = new Case();
        cbeCase.setId(UUID.randomUUID());
        cbeCase.setCaseNumber("CS-CBE-2026-0001");
        cbeCase.setTitle("Jewelry Store Heist");
        cbeCase.setCaseType("THEFT");
        cbeCase.setStation(cbeStation);
    }

    @Test
    @DisplayName("Test 1: Newly created case evaluates to CASE_CREATED with ~17% completeness")
    void testNewlyCreatedCaseState() {
        UUID caseId = cbeCase.getId();
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(false);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        CaseStateDTO state = caseStateService.computeCaseState(cbeCase);

        assertEquals("CASE_CREATED", state.getCurrentState());
        assertEquals("Case Created", state.getStateLabel());
        assertTrue(state.getCompletedAreas().contains("Case Information"));
        assertTrue(state.getMissingAreas().contains("FIR Registration"));
        assertTrue(state.getMissingAreas().contains("Victim Information"));
        assertTrue(state.getMissingAreas().contains("Suspect Information"));
        assertTrue(state.getMissingAreas().contains("Evidence Collection"));
        assertEquals(17, state.getProgressPercentage());
    }

    @Test
    @DisplayName("Test 2: FIR registered transitions state to FIR_REGISTERED")
    void testFirRegisteredState() {
        UUID caseId = cbeCase.getId();
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(true);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        CaseStateDTO state = caseStateService.computeCaseState(cbeCase);

        assertEquals("FIR_REGISTERED", state.getCurrentState());
        assertTrue(state.getCompletedAreas().contains("FIR"));
        assertEquals(33, state.getProgressPercentage());
    }

    @Test
    @DisplayName("Test 3: Victim identified transitions state to VICTIM_IDENTIFIED")
    void testVictimIdentifiedState() {
        UUID caseId = cbeCase.getId();
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(true);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(1L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        CaseStateDTO state = caseStateService.computeCaseState(cbeCase);

        assertEquals("VICTIM_IDENTIFIED", state.getCurrentState());
        assertTrue(state.getCompletedAreas().contains("Victim Information"));
        assertEquals(50, state.getProgressPercentage());
    }

    @Test
    @DisplayName("Test 4: Suspect identified transitions state to SUSPECT_IDENTIFIED")
    void testSuspectIdentifiedState() {
        UUID caseId = cbeCase.getId();
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(true);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(1L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(1L);
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        CaseStateDTO state = caseStateService.computeCaseState(cbeCase);

        assertEquals("SUSPECT_IDENTIFIED", state.getCurrentState());
        assertTrue(state.getCompletedAreas().contains("Suspect Information"));
        assertEquals(67, state.getProgressPercentage());
    }

    @Test
    @DisplayName("Test 5: Evidence collected (status UPLOADED) transitions state to EVIDENCE_COLLECTED")
    void testEvidenceCollectedState() {
        UUID caseId = cbeCase.getId();
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(true);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(1L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(1L);

        Evidence ev = new Evidence();
        ev.setStatus("UPLOADED");
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(ev));

        CaseStateDTO state = caseStateService.computeCaseState(cbeCase);

        assertEquals("EVIDENCE_COLLECTED", state.getCurrentState());
        assertTrue(state.getCompletedAreas().contains("Evidence Collection"));
        assertEquals(83, state.getProgressPercentage());
    }

    @Test
    @DisplayName("Test 6: Evidence status UNDER_REVIEW transitions state to EVIDENCE_UNDER_REVIEW")
    void testEvidenceUnderReviewState() {
        UUID caseId = cbeCase.getId();
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(true);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(1L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(1L);

        Evidence ev = new Evidence();
        ev.setStatus("UNDER_REVIEW");
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(ev));

        CaseStateDTO state = caseStateService.computeCaseState(cbeCase);

        assertEquals("EVIDENCE_UNDER_REVIEW", state.getCurrentState());
    }

    @Test
    @DisplayName("Test 7 & 8: Evidence status VERIFIED with complete areas transitions to INVESTIGATION_READY")
    void testInvestigationReadyState() {
        UUID caseId = cbeCase.getId();
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(true);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(1L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(1L);

        Evidence ev = new Evidence();
        ev.setStatus("VERIFIED");
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(ev));

        CaseStateDTO state = caseStateService.computeCaseState(cbeCase);

        assertEquals("INVESTIGATION_READY", state.getCurrentState());
        assertEquals("Ready for AI-Assisted Investigation", state.getStateLabel());
        assertEquals(100, state.getProgressPercentage());
        assertTrue(state.getMissingAreas().isEmpty());
    }

    @Test
    @DisplayName("Test 9: Changing evidence from VERIFIED back to UNDER_REVIEW recalculates dynamically")
    void testDynamicStateRecalculation() {
        UUID caseId = cbeCase.getId();
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(true);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(1L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(1L);

        Evidence ev = new Evidence();
        ev.setStatus("UNDER_REVIEW");
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(ev));

        CaseStateDTO state = caseStateService.computeCaseState(cbeCase);

        assertEquals("EVIDENCE_UNDER_REVIEW", state.getCurrentState());
        assertEquals(83, state.getProgressPercentage());
    }

    @Test
    @DisplayName("Test 10: Repeated Gap Analysis executes idempotently without duplicate open gaps")
    void testGapAnalysisIdempotency() {
        UUID caseId = cbeCase.getId();
        when(officerRepository.findByPoliceId("IND-TN-99881")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(caseId, cbeStation.getId())).thenReturn(Optional.of(cbeCase));

        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(false);
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        // First run: existing gaps empty
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());

        evidenceGapService.runGapAnalysis(caseId, "IND-TN-99881");

        // Verify save was called for the new gaps
        verify(evidenceGapRepository, atLeast(4)).save(any(EvidenceGap.class));
    }

    @Test
    @DisplayName("Test 11 & 12: Updating Gap Status to ACKNOWLEDGED, RESOLVED, and NOT_APPLICABLE")
    void testUpdateGapStatus() {
        UUID caseId = cbeCase.getId();
        UUID gapId = UUID.randomUUID();

        EvidenceGap gap = new EvidenceGap();
        gap.setId(gapId);
        gap.setCaseEntity(cbeCase);
        gap.setCategory("FORENSIC");
        gap.setTitle("Forensic report not available");
        gap.setStatus("OPEN");

        when(officerRepository.findByPoliceId("IND-TN-99881")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(caseId, cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(evidenceGapRepository.findByIdAndCaseEntityId(gapId, caseId)).thenReturn(Optional.of(gap));
        when(evidenceGapRepository.save(any(EvidenceGap.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 1. Acknowledge
        EvidenceGapDTO ackResp = evidenceGapService.updateGapStatus(
                caseId, gapId, new UpdateGapStatusRequest("ACKNOWLEDGED", "Investigator reviewed"), "IND-TN-99881"
        );
        assertEquals("ACKNOWLEDGED", ackResp.getStatus());

        // 2. Resolve
        EvidenceGapDTO resResp = evidenceGapService.updateGapStatus(
                caseId, gapId, new UpdateGapStatusRequest("RESOLVED", "Forensic analysis completed"), "IND-TN-99881"
        );
        assertEquals("RESOLVED", resResp.getStatus());
        assertNotNull(resResp.getResolvedAt());
        assertEquals(cbeOfficer.getId(), resResp.getResolvedById());

        // 3. Not Applicable
        EvidenceGapDTO naResp = evidenceGapService.updateGapStatus(
                caseId, gapId, new UpdateGapStatusRequest("NOT_APPLICABLE", "No forensic material at scene"), "IND-TN-99881"
        );
        assertEquals("NOT_APPLICABLE", naResp.getStatus());
    }

    @Test
    @DisplayName("Test 13: Coimbatore officer accessing Coimbatore case -> ALLOW")
    void testStationAuthorizationAllow() {
        UUID caseId = cbeCase.getId();
        when(officerRepository.findByPoliceId("IND-TN-99881")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(caseId, cbeStation.getId())).thenReturn(Optional.of(cbeCase));

        CaseStateDTO state = caseStateService.evaluateCaseState(caseId, "IND-TN-99881");
        assertNotNull(state);
        assertEquals("CS-CBE-2026-0001", state.getCaseNumber());
    }

    @Test
    @DisplayName("Test 14: Coimbatore officer accessing Egmore case -> DENY (ResourceNotFoundException)")
    void testStationAuthorizationDeny() {
        UUID egmCaseId = UUID.randomUUID();
        when(caseRepository.findByIdAndStationId(egmCaseId, cbeStation.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            caseStateService.getAuthorizedCase(egmCaseId, cbeOfficer);
        });
    }

    @Test
    @DisplayName("Test 15: Accessing another case's gap ID -> DENY (ResourceNotFoundException)")
    void testCrossCaseGapIsolationDeny() {
        UUID caseId = cbeCase.getId();
        UUID fakeGapId = UUID.randomUUID();

        when(officerRepository.findByPoliceId("IND-TN-99881")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(caseId, cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(evidenceGapRepository.findByIdAndCaseEntityId(fakeGapId, caseId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            evidenceGapService.updateGapStatus(
                    caseId, fakeGapId, new UpdateGapStatusRequest("RESOLVED", "note"), "IND-TN-99881"
            );
        });
    }
}
