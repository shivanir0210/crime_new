package com.crimesphere.backend;

import com.crimesphere.backend.dto.ai.*;
import com.crimesphere.backend.dto.analysis.CaseStateDTO;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import com.crimesphere.backend.service.AiAnalysisService;
import com.crimesphere.backend.service.AiRetrievalService;
import com.crimesphere.backend.service.AiServiceClient;
import com.crimesphere.backend.service.CaseStateService;
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
public class AiIntegrationTest {

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

    @Mock
    private AiRecommendationRepository recommendationRepository;

    @Mock
    private AiRecommendationItemRepository recommendationItemRepository;

    @Mock
    private AiRecommendationEvidenceRepository recommendationEvidenceRepository;

    @Mock
    private RecommendationVerificationRepository verificationRepository;

    @Mock
    private RecommendationVerificationEvidenceRepository verificationEvidenceRepository;

    private CaseStateService caseStateService;
    private TestAiServiceClient testAiServiceClient;
    private TestAiRetrievalService testAiRetrievalService;
    private AiAnalysisService aiAnalysisService;

    private PoliceStation cbeStation;
    private PoliceStation egmStation;
    private PoliceOfficer cbeOfficer;
    private Case cbeCase;

    static class TestAiServiceClient extends AiServiceClient {
        AiCaseAnalysisResponse nextResponse;
        RuntimeException nextException;
        AiCaseAnalysisRequest capturedRequest;

        public TestAiServiceClient() {
            super("http://localhost:8000", "test-key", 1000);
        }

        @Override
        public Map<String, Object> checkHealth() {
            if (nextException != null) {
                return Map.of("status", "DOWN", "error", nextException.getMessage());
            }
            return Map.of("status", "UP", "service", "CrimeSphere AI Service");
        }

        @Override
        public AiCaseAnalysisResponse sendCaseAnalysis(AiCaseAnalysisRequest request) {
            this.capturedRequest = request;
            if (nextException != null) {
                throw nextException;
            }
            return nextResponse != null ? nextResponse : new AiCaseAnalysisResponse();
        }
    }

    static class TestAiRetrievalService extends AiRetrievalService {
        AiRetrievalResponse nextResponse;

        public TestAiRetrievalService() {
            super(null, null, null, null, null, null, null, null);
        }

        @Override
        public AiRetrievalResponse retrieveSimilarKnowledge(UUID caseId, String queryOverride, int topK, double minSimilarity, String policeId) {
            return nextResponse;
        }
    }

    @BeforeEach
    void setUp() {
        caseStateService = new CaseStateService(
                caseRepository, firRepository, victimRepository,
                suspectRepository, evidenceRepository, officerRepository, auditLogRepository
        );

        testAiServiceClient = new TestAiServiceClient();
        testAiRetrievalService = new TestAiRetrievalService();

        aiAnalysisService = new AiAnalysisService(
                caseRepository, firRepository, victimRepository,
                suspectRepository, evidenceRepository, evidenceGapRepository,
                officerRepository, auditLogRepository, caseStateService, testAiServiceClient,
                testAiRetrievalService,
                recommendationRepository, recommendationItemRepository, recommendationEvidenceRepository,
                verificationRepository, verificationEvidenceRepository
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

        cbeCase = new Case();
        cbeCase.setId(UUID.randomUUID());
        cbeCase.setCaseNumber("CS-CBE-2026-0001");
        cbeCase.setTitle("Gold Heist Investigation");
        cbeCase.setCaseType("THEFT");
        cbeCase.setStatus("UNDER_INVESTIGATION");
        cbeCase.setStation(cbeStation);
    }

    @Test
    @DisplayName("Test 1: Build Case Analysis Payload performs full mapping and data minimization")
    void testBuildCaseAnalysisPayload() {
        UUID caseId = cbeCase.getId();

        // Mock FIR
        FIR fir = new FIR();
        fir.setId(UUID.randomUUID());
        fir.setFirNumber("FIR-CBE-2026-0001");
        fir.setPoliceStation("Coimbatore Central");
        fir.setOffenceType("THEFT");
        when(firRepository.findByCaseEntityId(caseId)).thenReturn(Optional.of(fir));
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(true);

        // Mock Victim
        Victim victim = new Victim();
        victim.setId(UUID.randomUUID());
        victim.setFullName("R. Sundaram");
        victim.setRelationshipToCase("Complainant");
        victim.setStatus("ACTIVE");
        when(victimRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId)).thenReturn(List.of(victim));
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(1L);

        // Mock Suspect
        Suspect suspect = new Suspect();
        suspect.setId(UUID.randomUUID());
        suspect.setFullName("K. Mani");
        suspect.setAlias("Minnal");
        suspect.setInvestigationStatus("UNDER_INVESTIGATION");
        when(suspectRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId)).thenReturn(List.of(suspect));
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(1L);

        // Mock Evidence
        Evidence evidence = new Evidence();
        evidence.setId(UUID.randomUUID());
        evidence.setEvidenceNumber("EVD-CBE-2026-0001");
        evidence.setTitle("CCTV Footage");
        evidence.setEvidenceType("CCTV");
        evidence.setStatus("UNDER_REVIEW");
        evidence.setFileHash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(evidence));

        // Mock Evidence Gap
        EvidenceGap gap = new EvidenceGap();
        gap.setId(UUID.randomUUID());
        gap.setCategory("FORENSIC");
        gap.setTitle("Forensic report not available");
        gap.setSeverity("MEDIUM");
        gap.setStatus("OPEN");
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(gap));

        AiCaseAnalysisRequest payload = aiAnalysisService.buildCaseAnalysisPayload(cbeCase);

        assertNotNull(payload);
        assertEquals(caseId, payload.getCaseId());
        assertEquals("CS-CBE-2026-0001", payload.getCaseNumber());
        assertEquals("THEFT", payload.getCaseType());
        assertEquals("EVIDENCE_UNDER_REVIEW", payload.getInvestigationState());
        assertEquals(83, payload.getInvestigationCompleteness());

        assertNotNull(payload.getFir());
        assertEquals("FIR-CBE-2026-0001", payload.getFir().getFirNumber());

        assertEquals(1, payload.getVictims().size());
        assertEquals("R. Sundaram", payload.getVictims().get(0).getFullName());

        assertEquals(1, payload.getSuspects().size());
        assertEquals("K. Mani", payload.getSuspects().get(0).getFullName());

        assertEquals(1, payload.getEvidence().size());
        assertEquals("EVD-CBE-2026-0001", payload.getEvidence().get(0).getEvidenceNumber());

        assertEquals(1, payload.getEvidenceGaps().size());
        assertEquals("FORENSIC", payload.getEvidenceGaps().get(0).getCategory());
    }

    @Test
    @DisplayName("Test 2: Dispatch Case to AI Service with valid station authorization persists recommendation")
    void testDispatchCaseToAiServiceSuccess() {
        UUID caseId = cbeCase.getId();

        when(officerRepository.findByPoliceId("IND-TN-99881")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(caseId, cbeStation.getId())).thenReturn(Optional.of(cbeCase));

        when(firRepository.findByCaseEntityId(caseId)).thenReturn(Optional.empty());
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(false);
        when(victimRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId)).thenReturn(Collections.emptyList());
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(suspectRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId)).thenReturn(Collections.emptyList());
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        AiCaseAnalysisResponse mockResponse = new AiCaseAnalysisResponse();
        mockResponse.setCaseId(caseId);
        mockResponse.setCaseNumber("CS-CBE-2026-0001");
        mockResponse.setAnalysisVersion("phase-9");
        mockResponse.setModelName("groq/compound-mini");
        mockResponse.setPromptVersion("v1");
        mockResponse.setStatus("REVIEW_REQUIRED");
        mockResponse.setSummary("Structured investigative synthesis.");
        mockResponse.getKeyObservations().add(new AiKeyObservationDto("CCTV spotted suspect", List.of("EVD-CBE-2026-0001"), "Relevant lead"));
        testAiServiceClient.nextResponse = mockResponse;

        AiRecommendation mockSavedRec = new AiRecommendation();
        mockSavedRec.setId(UUID.randomUUID());
        mockSavedRec.setCreatedAt(OffsetDateTime.now());
        when(recommendationRepository.save(any(AiRecommendation.class))).thenReturn(mockSavedRec);

        AiRecommendationItem mockSavedItem = new AiRecommendationItem();
        mockSavedItem.setId(UUID.randomUUID());
        when(recommendationItemRepository.save(any(AiRecommendationItem.class))).thenReturn(mockSavedItem);

        AiCaseAnalysisResponse response = aiAnalysisService.dispatchCaseToAiService(caseId, "IND-TN-99881");

        assertNotNull(response);
        assertTrue(response.getAnalysisAvailable());
        assertEquals("phase-9", response.getAnalysisVersion());
        assertEquals("REVIEW_REQUIRED", response.getStatus());
        assertNotNull(testAiServiceClient.capturedRequest);
        assertEquals("CS-CBE-2026-0001", testAiServiceClient.capturedRequest.getCaseNumber());
        verify(recommendationRepository, times(1)).save(any(AiRecommendation.class));
        verify(auditLogRepository, times(2)).save(any(AuditLog.class)); // REQUESTED and COMPLETED
    }

    @Test
    @DisplayName("Test 3: Cross-station access to dispatch case is denied")
    void testCrossStationDispatchDenied() {
        UUID egmCaseId = UUID.randomUUID();

        when(officerRepository.findByPoliceId("IND-TN-99881")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(egmCaseId, cbeStation.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            aiAnalysisService.dispatchCaseToAiService(egmCaseId, "IND-TN-99881");
        });
    }

    @Test
    @DisplayName("Test 4: AI Service connection error propagates gracefully and logs failure")
    void testAiServiceCommunicationFailure() {
        UUID caseId = cbeCase.getId();

        when(officerRepository.findByPoliceId("IND-TN-99881")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(caseId, cbeStation.getId())).thenReturn(Optional.of(cbeCase));

        when(firRepository.findByCaseEntityId(caseId)).thenReturn(Optional.empty());
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(false);
        when(victimRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId)).thenReturn(Collections.emptyList());
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(suspectRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId)).thenReturn(Collections.emptyList());
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        testAiServiceClient.nextException = new IllegalStateException("AI Service communication failure: Connection refused");

        assertThrows(IllegalStateException.class, () -> {
            aiAnalysisService.dispatchCaseToAiService(caseId, "IND-TN-99881");
        });

        verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Test 5: History retrieval returns chronological list for authorized officer")
    void testGetCaseAnalysisHistory() {
        UUID caseId = cbeCase.getId();

        when(officerRepository.findByPoliceId("IND-TN-99881")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(caseId, cbeStation.getId())).thenReturn(Optional.of(cbeCase));

        AiRecommendation rec = new AiRecommendation();
        rec.setId(UUID.randomUUID());
        rec.setAnalysisVersion("phase-9");
        rec.setSummary("History summary");
        rec.setModelName("groq/compound-mini");
        rec.setPromptVersion("v1");
        rec.setStatus("REVIEW_REQUIRED");
        rec.setCreatedAt(OffsetDateTime.now());
        rec.setCreatedBy(cbeOfficer);

        when(recommendationRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(rec));

        List<AiRecommendationHistoryDto> history = aiAnalysisService.getCaseAnalysisHistory(caseId, "IND-TN-99881");

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("phase-9", history.get(0).getAnalysisVersion());
        assertEquals("Inspector Vijay", history.get(0).getCreatedByOfficer());
    }

    @Test
    @DisplayName("Test 6: RAG retrieval context is NOT invoked during Phase 8 dispatch")
    void testRAGRetrievalNotInjectedDuringPhase8Dispatch() {
        UUID caseId = cbeCase.getId();

        when(officerRepository.findByPoliceId("IND-TN-99881")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(caseId, cbeStation.getId())).thenReturn(Optional.of(cbeCase));

        when(firRepository.findByCaseEntityId(caseId)).thenReturn(Optional.empty());
        when(firRepository.existsByCaseEntityId(caseId)).thenReturn(false);
        when(victimRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId)).thenReturn(Collections.emptyList());
        when(victimRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(suspectRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId)).thenReturn(Collections.emptyList());
        when(suspectRepository.countByCaseEntityId(caseId)).thenReturn(0L);
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        AiCaseAnalysisResponse mockResponse = new AiCaseAnalysisResponse();
        mockResponse.setCaseId(caseId);
        mockResponse.setCaseNumber("CS-CBE-2026-0001");
        mockResponse.setStatus("RECEIVED");
        mockResponse.setMessage("Case analysis request received successfully.");
        testAiServiceClient.nextResponse = mockResponse;

        AiCaseAnalysisResponse response = aiAnalysisService.dispatchCaseToAiService(caseId, "IND-TN-99881");

        assertNotNull(response);
        assertNotNull(testAiServiceClient.capturedRequest);
        assertEquals(0, testAiServiceClient.capturedRequest.getRetrievedContext().size());
    }
}
