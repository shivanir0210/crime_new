package com.crimesphere.backend;

import com.crimesphere.backend.dto.witness.CreateWitnessRequest;
import com.crimesphere.backend.dto.witness.UpdateWitnessRequest;
import com.crimesphere.backend.dto.witness.WitnessResponse;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import com.crimesphere.backend.service.WitnessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WitnessServiceTest {

    @Mock
    private WitnessRepository witnessRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private PoliceOfficerRepository officerRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private WitnessService witnessService;

    private PoliceStation cbeStation;
    private PoliceStation egmStation;
    private PoliceOfficer cbeOfficer;
    private PoliceOfficer egmOfficer;
    private Case cbeCase;

    @BeforeEach
    void setUp() {
        witnessService = new WitnessService(witnessRepository, caseRepository, officerRepository, auditLogRepository);

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
        cbeCase.setStation(cbeStation);
        cbeCase.setStatus("UNDER_INVESTIGATION");
        cbeCase.setPriority("CRITICAL");
        cbeCase.setCaseType("MURDER");
        cbeCase.setCreatedBy(cbeOfficer);
        cbeCase.setCreatedAt(OffsetDateTime.now().minusDays(3));
    }

    @Test
    @DisplayName("Create witness successfully when valid data provided")
    void testCreateWitnessSuccess() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));

        CreateWitnessRequest req = new CreateWitnessRequest();
        req.setFullName("R. Natarajan");
        req.setAge(56);
        req.setGender("MALE");
        req.setPhone("9842109876");
        req.setAddress("Gandhi Park, Coimbatore");
        req.setStatementSummary("Saw suspect running towards north gate.");
        req.setWitnessType("EYEWITNESS");
        req.setReliabilityScore("HIGH");

        when(witnessRepository.save(any(Witness.class))).thenAnswer(invocation -> {
            Witness w = invocation.getArgument(0);
            w.setId(UUID.randomUUID());
            return w;
        });

        WitnessResponse response = witnessService.createWitness(cbeCase.getId(), req, "TN-POL-1001", null);

        assertNotNull(response);
        assertEquals("R. Natarajan", response.getFullName());
        assertEquals("EYEWITNESS", response.getWitnessType());
        assertEquals("HIGH", response.getReliabilityScore());
        verify(witnessRepository, times(1)).save(any(Witness.class));
    }

    @Test
    @DisplayName("Reject witness creation when full name is empty")
    void testCreateWitnessValidationFailure() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));

        CreateWitnessRequest req = new CreateWitnessRequest();
        req.setFullName("   ");

        assertThrows(IllegalArgumentException.class, () -> {
            witnessService.createWitness(cbeCase.getId(), req, "TN-POL-1001", null);
        });
    }

    @Test
    @DisplayName("Enforce station isolation - officer from different station cannot view witnesses")
    void testStationIsolationEnforced() {
        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(egmOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), egmStation.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            witnessService.getWitnessesByCaseId(cbeCase.getId(), "TN-POL-1002");
        });
    }

    @Test
    @DisplayName("Get witnesses by case ID returns all case witnesses")
    void testGetWitnessesByCaseId() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));

        Witness w1 = new Witness();
        w1.setId(UUID.randomUUID());
        w1.setCaseEntity(cbeCase);
        w1.setFullName("Witness 1");
        w1.setWitnessType("EYEWITNESS");

        Witness w2 = new Witness();
        w2.setId(UUID.randomUUID());
        w2.setCaseEntity(cbeCase);
        w2.setFullName("Witness 2");
        w2.setWitnessType("MATERIAL_WITNESS");

        when(witnessRepository.findByCaseEntityIdOrderByCreatedAtAsc(cbeCase.getId())).thenReturn(List.of(w1, w2));

        List<WitnessResponse> list = witnessService.getWitnessesByCaseId(cbeCase.getId(), "TN-POL-1001");

        assertEquals(2, list.size());
        assertEquals("Witness 1", list.get(0).getFullName());
        assertEquals("Witness 2", list.get(1).getFullName());
    }
}
