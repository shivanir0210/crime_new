package com.crimesphere.backend;

import com.crimesphere.backend.dto.case_mgmt.*;
import com.crimesphere.backend.dto.dashboard.DashboardStatsResponse;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.entity.PoliceStation;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import com.crimesphere.backend.service.CaseService;
import com.crimesphere.backend.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CaseServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private PoliceOfficerRepository officerRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private CaseService caseService;
    private DashboardService dashboardService;

    private PoliceStation cbeStation;
    private PoliceStation egmStation;
    private PoliceOfficer cbeOfficer;
    private PoliceOfficer egmOfficer;
    private Case cbeCase;

    @BeforeEach
    void setUp() {
        caseService = new CaseService(caseRepository, officerRepository, auditLogRepository);
        dashboardService = new DashboardService(caseRepository, officerRepository);

        cbeStation = new PoliceStation();
        cbeStation.setId(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));
        cbeStation.setStationCode("TN-POLICE-CBE");
        cbeStation.setStationName("Coimbatore Central Police Station");

        egmStation = new PoliceStation();
        egmStation.setId(UUID.fromString("b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22"));
        egmStation.setStationCode("TN-POLICE-EGM");
        egmStation.setStationName("Egmore Police Station");

        cbeOfficer = new PoliceOfficer();
        cbeOfficer.setId(UUID.randomUUID());
        cbeOfficer.setPoliceId("TN-POL-1001");
        cbeOfficer.setFullName("Development Officer One");
        cbeOfficer.setStation(cbeStation);
        cbeOfficer.setRole("OFFICER");
        cbeOfficer.setIsActive(true);

        egmOfficer = new PoliceOfficer();
        egmOfficer.setId(UUID.randomUUID());
        egmOfficer.setPoliceId("TN-POL-1002");
        egmOfficer.setFullName("Development Officer Two");
        egmOfficer.setStation(egmStation);
        egmOfficer.setRole("OFFICER");
        egmOfficer.setIsActive(true);

        cbeCase = new Case();
        cbeCase.setId(UUID.fromString("a1111111-9c0b-4ef8-bb6d-6bb9bd380001"));
        cbeCase.setCaseNumber("CS-2026-0001");
        cbeCase.setTitle("Homicide at Gandhi Park Area");
        cbeCase.setDescription("Initial incident details");
        cbeCase.setCaseType("MURDER");
        cbeCase.setPriority("CRITICAL");
        cbeCase.setStatus("OPEN");
        cbeCase.setStation(cbeStation);
        cbeCase.setCreatedBy(cbeOfficer);
    }

    @Test
    @DisplayName("1. Authenticated officer can create a case with auto-generated case number, OPEN status, and station binding")
    void testCreateCaseSuccess() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.getNextCaseNumberSequence()).thenReturn(101L);
        when(caseRepository.save(any(Case.class))).thenAnswer(invocation -> {
            Case arg = invocation.getArgument(0);
            arg.setId(UUID.randomUUID());
            return arg;
        });

        CreateCaseRequest request = new CreateCaseRequest(
                "Suspicious incident in RS Puram",
                "Detailed overview",
                "MURDER",
                "HIGH"
        );

        CaseResponse response = caseService.createCase(request, "TN-POL-1001", null);

        assertNotNull(response);
        assertNotNull(response.getCaseNumber());
        assertTrue(response.getCaseNumber().startsWith("CS-2026-"));
        assertEquals("OPEN", response.getStatus());
        assertEquals("HIGH", response.getPriority());
        assertEquals("MURDER", response.getCaseType());
        assertEquals("TN-POLICE-CBE", response.getStation().getStationCode());
        assertEquals("TN-POL-1001", response.getCreatedBy().getPoliceId());

        verify(caseRepository, times(1)).save(any(Case.class));
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("2. Authenticated officer can list cases with pagination and station filtering")
    void testGetCasesPagination() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        Page<Case> pagedCases = new PageImpl<>(List.of(cbeCase), PageRequest.of(0, 10), 1);
        when(caseRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagedCases);

        PageResponse<CaseSummaryResponse> response = caseService.getCases(
                "TN-POL-1001", null, "OPEN", "CRITICAL", "MURDER", PageRequest.of(0, 10)
        );

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("CS-2026-0001", response.getContent().get(0).getCaseNumber());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    @DisplayName("3. Station Isolation: Coimbatore officer can access Coimbatore case details")
    void testGetCaseById_AuthorizedStation() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId()))
                .thenReturn(Optional.of(cbeCase));

        CaseResponse response = caseService.getCaseById(cbeCase.getId(), "TN-POL-1001");
        assertNotNull(response);
        assertEquals("CS-2026-0001", response.getCaseNumber());
        assertEquals("Coimbatore Central Police Station", response.getStation().getStationName());
    }

    @Test
    @DisplayName("4. Station Isolation: Egmore officer accessing Coimbatore case receives ResourceNotFoundException (404/Forbidden)")
    void testGetCaseById_UnauthorizedStation() {
        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(egmOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), egmStation.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            caseService.getCaseById(cbeCase.getId(), "TN-POL-1002");
        });
    }

    @Test
    @DisplayName("5. Authenticated officer can update case details and record audit log")
    void testUpdateCase() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId()))
                .thenReturn(Optional.of(cbeCase));
        when(caseRepository.save(any(Case.class))).thenReturn(cbeCase);

        UpdateCaseRequest request = new UpdateCaseRequest("Updated Title", "Updated Description", "MURDER", "CRITICAL", "UNDER_INVESTIGATION");
        CaseResponse response = caseService.updateCase(cbeCase.getId(), request, "TN-POL-1001", null);

        assertNotNull(response);
        assertEquals("Updated Title", response.getTitle());
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("6. Station Isolation: Officer from Station A cannot modify Station B case")
    void testUpdateCase_UnauthorizedStation() {
        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(egmOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), egmStation.getId()))
                .thenReturn(Optional.empty());

        UpdateCaseRequest request = new UpdateCaseRequest("Hacked Title", "Hacked Description", "MURDER", "LOW", "CLOSED");

        assertThrows(ResourceNotFoundException.class, () -> {
            caseService.updateCase(cbeCase.getId(), request, "TN-POL-1002", null);
        });
    }

    @Test
    @DisplayName("7. Should update case status and record audit log")
    void testUpdateCaseStatus() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId()))
                .thenReturn(Optional.of(cbeCase));
        when(caseRepository.save(any(Case.class))).thenReturn(cbeCase);

        CaseResponse response = caseService.updateStatus(cbeCase.getId(), "UNDER_INVESTIGATION", "TN-POL-1001", null);

        assertEquals("UNDER_INVESTIGATION", response.getStatus());
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("8. Invalid case status transition throws IllegalArgumentException")
    void testInvalidStatusTransition() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));

        assertThrows(IllegalArgumentException.class, () -> {
            caseService.updateStatus(cbeCase.getId(), "INVALID_STATUS_XYZ", "TN-POL-1001", null);
        });
    }

    @Test
    @DisplayName("9. Should update case priority and record audit log")
    void testUpdateCasePriority() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId()))
                .thenReturn(Optional.of(cbeCase));
        when(caseRepository.save(any(Case.class))).thenReturn(cbeCase);

        CaseResponse response = caseService.updatePriority(cbeCase.getId(), "CRITICAL", "TN-POL-1001", null);

        assertEquals("CRITICAL", response.getPriority());
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("10. Invalid case priority throws IllegalArgumentException")
    void testInvalidPriority() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));

        assertThrows(IllegalArgumentException.class, () -> {
            caseService.updatePriority(cbeCase.getId(), "ULTRA_URGENT", "TN-POL-1001", null);
        });
    }

    @Test
    @DisplayName("11. Should return station-scoped dashboard statistics")
    void testGetDashboardStats() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.countByStationIdAndStatusNot(cbeStation.getId(), "CLOSED")).thenReturn(5L);
        when(caseRepository.countByStationIdAndPriorityAndStatusNot(cbeStation.getId(), "HIGH", "CLOSED")).thenReturn(2L);
        when(caseRepository.countByStationIdAndPriorityAndStatusNot(cbeStation.getId(), "CRITICAL", "CLOSED")).thenReturn(1L);
        when(caseRepository.countByStationIdAndStatus(cbeStation.getId(), "PENDING_REVIEW")).thenReturn(1L);
        when(caseRepository.countByStationIdAndStatus(cbeStation.getId(), "ON_HOLD")).thenReturn(1L);
        when(caseRepository.countByStationId(cbeStation.getId())).thenReturn(6L);

        DashboardStatsResponse stats = dashboardService.getDashboardStats("TN-POL-1001");

        assertNotNull(stats);
        assertEquals(5L, stats.getActiveCases());
        assertEquals(2L, stats.getHighPriorityCases());
        assertEquals(1L, stats.getCriticalCases());
        assertEquals(1L, stats.getPendingReview());
        assertEquals(1L, stats.getOnHoldCases());
        assertEquals(6L, stats.getTotalCases());
        assertEquals("Coimbatore Central Police Station", stats.getStationName());
    }
}
