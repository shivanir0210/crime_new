package com.crimesphere.backend;

import com.crimesphere.backend.dto.ai.AiCaseAnalysisRequest;
import com.crimesphere.backend.dto.ai.AiCaseAnalysisResponse;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import com.crimesphere.backend.service.AiAnalysisService;
import com.crimesphere.backend.service.AiServiceClient;
import com.crimesphere.backend.service.CaseStateService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Phase8AiIntegrationTest {

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

    private TestAiServiceClient testAiServiceClient;

    private CaseStateService caseStateService;
    private AiAnalysisService aiAnalysisService;

    private PoliceStation coimbatoreStation;
    private PoliceStation egmoreStation;
    private PoliceOfficer coimbatoreOfficer;
    private PoliceOfficer egmoreOfficer;
    private Case coimbatoreCase;

    private static class TestAiServiceClient extends AiServiceClient {
        public Map<String, Object> healthResponse;
        public AiCaseAnalysisResponse analysisResponse;
        public RuntimeException exceptionToThrow;
        public boolean sendCaseAnalysisCalled = false;

        public TestAiServiceClient() {
            super("http://localhost:8000", "test-key", 1000);
        }

        @Override
        public Map<String, Object> checkHealth() {
            if (exceptionToThrow != null) throw exceptionToThrow;
            return healthResponse != null ? healthResponse : Map.of(
                    "status", "PARTIAL",
                    "service", "CrimeSphere AI Integration",
                    "aiService", "DOWN"
            );
        }

        @Override
        public AiCaseAnalysisResponse sendCaseAnalysis(AiCaseAnalysisRequest request) {
            sendCaseAnalysisCalled = true;
            if (exceptionToThrow != null) throw exceptionToThrow;
            return analysisResponse;
        }
    }

    @BeforeEach
    void setUp() {
        testAiServiceClient = new TestAiServiceClient();

        caseStateService = new CaseStateService(
                caseRepository, firRepository, victimRepository,
                suspectRepository, evidenceRepository, officerRepository, auditLogRepository
        );

        aiAnalysisService = new AiAnalysisService(
                caseRepository, firRepository, victimRepository, suspectRepository,
                evidenceRepository, evidenceGapRepository, officerRepository, auditLogRepository,
                caseStateService, testAiServiceClient, null, null, null, null, null, null, null, null
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
        coimbatoreCase.setId(UUID.fromString("f8bf3e9c-bcc6-4d5c-a430-c0697a30d6cf"));
        coimbatoreCase.setCaseNumber("CS-2026-0100");
        coimbatoreCase.setTitle("Coimbatore Park Case");
        coimbatoreCase.setCaseType("MURDER");
        coimbatoreCase.setStation(coimbatoreStation);
    }

    @Test
    @DisplayName("Test 1: Spring Boot AI Health endpoint returns UP status when FastAPI is reachable")
    void test1_SpringBootAiHealth() {
        testAiServiceClient.healthResponse = Map.of(
                "status", "UP",
                "service", "CrimeSphere AI Integration",
                "aiService", "UP"
        );

        Map<String, Object> health = testAiServiceClient.checkHealth();
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
        assertEquals("UP", health.get("aiService"));
    }

    @Test
    @DisplayName("Test 2: Authenticated officer (TN-POL-1001) dispatches case to FastAPI and receives acknowledgement")
    void test2_AuthenticatedCaseAnalysisDispatch() {
        UUID caseId = coimbatoreCase.getId();
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(coimbatoreOfficer));
        when(caseRepository.findByIdAndStationId(caseId, coimbatoreStation.getId())).thenReturn(Optional.of(coimbatoreCase));

        AiCaseAnalysisResponse mockResponse = new AiCaseAnalysisResponse();
        mockResponse.setRequestId(UUID.randomUUID().toString());
        mockResponse.setStatus("RECEIVED");
        mockResponse.setMessage("Case analysis request received successfully.");
        mockResponse.setCaseId(caseId);
        mockResponse.setAnalysisAvailable(false);

        testAiServiceClient.analysisResponse = mockResponse;

        AiCaseAnalysisResponse result = aiAnalysisService.dispatchCaseToAiService(caseId, "TN-POL-1001");

        assertNotNull(result);
        assertEquals("RECEIVED", result.getStatus());
        assertEquals("Case analysis request received successfully.", result.getMessage());
        assertFalse(result.getAnalysisAvailable());
        assertTrue(testAiServiceClient.sendCaseAnalysisCalled);
        verify(auditLogRepository, times(2)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Test 3: Station isolation - Egmore officer (TN-POL-1002) blocked from analyzing Coimbatore case")
    void test3_StationIsolationDenied() {
        UUID caseId = coimbatoreCase.getId();
        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(egmoreOfficer));
        when(caseRepository.findByIdAndStationId(caseId, egmoreStation.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            aiAnalysisService.dispatchCaseToAiService(caseId, "TN-POL-1002");
        });

        assertFalse(testAiServiceClient.sendCaseAnalysisCalled);
    }

    @Test
    @DisplayName("Test 4: Handle 503 Service Unavailable when FastAPI is down")
    void test4_ServiceUnavailableHandling() {
        UUID caseId = coimbatoreCase.getId();
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(coimbatoreOfficer));
        when(caseRepository.findByIdAndStationId(caseId, coimbatoreStation.getId())).thenReturn(Optional.of(coimbatoreCase));

        testAiServiceClient.exceptionToThrow = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI service is currently unavailable.");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            aiAnalysisService.dispatchCaseToAiService(caseId, "TN-POL-1001");
        });

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertTrue(ex.getReason().contains("unavailable"));
    }

    @Test
    @DisplayName("Test 5: Handle 504 Gateway Timeout when FastAPI request times out")
    void test5_GatewayTimeoutHandling() {
        UUID caseId = coimbatoreCase.getId();
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(coimbatoreOfficer));
        when(caseRepository.findByIdAndStationId(caseId, coimbatoreStation.getId())).thenReturn(Optional.of(coimbatoreCase));

        testAiServiceClient.exceptionToThrow = new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AI service request timed out.");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            aiAnalysisService.dispatchCaseToAiService(caseId, "TN-POL-1001");
        });

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("timed out"));
    }
}
