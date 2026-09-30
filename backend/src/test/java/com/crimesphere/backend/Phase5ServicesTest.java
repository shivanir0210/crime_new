package com.crimesphere.backend;

import com.crimesphere.backend.dto.fir.CreateFIRRequest;
import com.crimesphere.backend.dto.fir.FIRResponse;
import com.crimesphere.backend.dto.fir.UpdateFIRRequest;
import com.crimesphere.backend.dto.suspect.CreateSuspectRequest;
import com.crimesphere.backend.dto.suspect.SuspectResponse;
import com.crimesphere.backend.dto.suspect.UpdateSuspectRequest;
import com.crimesphere.backend.dto.victim.CreateVictimRequest;
import com.crimesphere.backend.dto.victim.UpdateVictimRequest;
import com.crimesphere.backend.dto.victim.VictimResponse;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import com.crimesphere.backend.service.FIRService;
import com.crimesphere.backend.service.SuspectService;
import com.crimesphere.backend.service.VictimService;
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
public class Phase5ServicesTest {

    @Mock
    private FIRRepository firRepository;

    @Mock
    private VictimRepository victimRepository;

    @Mock
    private SuspectRepository suspectRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private PoliceOfficerRepository officerRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private FIRService firService;
    private VictimService victimService;
    private SuspectService suspectService;

    private PoliceStation cbeStation;
    private PoliceStation egmStation;
    private PoliceOfficer cbeOfficer;
    private PoliceOfficer egmOfficer;
    private Case cbeCase;

    @BeforeEach
    void setUp() {
        firService = new FIRService(firRepository, caseRepository, officerRepository, auditLogRepository);
        victimService = new VictimService(victimRepository, caseRepository, officerRepository, auditLogRepository);
        suspectService = new SuspectService(suspectRepository, caseRepository, officerRepository, auditLogRepository);

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
        cbeCase.setCaseType("MURDER");
        cbeCase.setPriority("CRITICAL");
        cbeCase.setStatus("OPEN");
        cbeCase.setStation(cbeStation);
        cbeCase.setCreatedBy(cbeOfficer);
        cbeCase.setUpdatedAt(OffsetDateTime.now().minusDays(1));
    }

    /* ==========================================================================
       FIR SERVICE TESTS (1 - 4, 13, 16-18, 20)
       ========================================================================== */

    @Test
    @DisplayName("1. FIR: Should create FIR with FIR-<STATION>-YYYY-XXXX number and update parent case timestamp")
    void testCreateFIR_Success() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(firRepository.existsByCaseEntityId(cbeCase.getId())).thenReturn(false);
        when(firRepository.getNextFirNumberSequence()).thenReturn(101L);
        when(firRepository.save(any(FIR.class))).thenAnswer(invocation -> {
            FIR f = invocation.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });

        CreateFIRRequest request = new CreateFIRRequest();
        request.setComplainantName("S. Ramanathan");
        request.setOffenceType("HOMICIDE");
        request.setSectionsOfLaw("IPC 302, IPC 34");

        FIRResponse response = firService.createFIR(cbeCase.getId(), request, "TN-POL-1001", null);

        assertNotNull(response);
        assertNotNull(response.getFirNumber());
        assertTrue(response.getFirNumber().startsWith("FIR-CBE-2026-"));
        assertEquals("S. Ramanathan", response.getComplainantName());
        assertEquals("TN-POL-1001", response.getCreatedBy().getPoliceId());

        verify(caseRepository, times(1)).save(cbeCase);
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("2. FIR: Should retrieve FIR for authorized case")
    void testGetFIR_Success() {
        FIR fir = new FIR();
        fir.setId(UUID.randomUUID());
        fir.setCaseEntity(cbeCase);
        fir.setFirNumber("FIR-CBE-2026-0001");
        fir.setComplainantName("S. Ramanathan");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(firRepository.findByCaseEntityId(cbeCase.getId())).thenReturn(Optional.of(fir));

        FIRResponse response = firService.getFIRByCaseId(cbeCase.getId(), "TN-POL-1001");

        assertNotNull(response);
        assertEquals("FIR-CBE-2026-0001", response.getFirNumber());
    }

    @Test
    @DisplayName("3. FIR: Should update FIR details")
    void testUpdateFIR_Success() {
        FIR fir = new FIR();
        fir.setId(UUID.randomUUID());
        fir.setCaseEntity(cbeCase);
        fir.setFirNumber("FIR-CBE-2026-0001");
        fir.setComplainantName("S. Ramanathan");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(firRepository.findByCaseEntityId(cbeCase.getId())).thenReturn(Optional.of(fir));
        when(firRepository.save(any(FIR.class))).thenReturn(fir);

        UpdateFIRRequest request = new UpdateFIRRequest();
        request.setComplainantName("Updated Ramanathan");
        request.setDescription("Updated description");

        FIRResponse response = firService.updateFIR(cbeCase.getId(), request, "TN-POL-1001", null);

        assertNotNull(response);
        assertEquals("Updated Ramanathan", response.getComplainantName());
        verify(caseRepository, times(1)).save(cbeCase);
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("4. FIR: Reject duplicate FIR creation on the same case (1:1 constraint)")
    void testCreateFIR_DuplicateRejection() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(firRepository.existsByCaseEntityId(cbeCase.getId())).thenReturn(true);

        CreateFIRRequest request = new CreateFIRRequest();
        request.setComplainantName("Duplicate Complainant");

        assertThrows(IllegalArgumentException.class, () -> {
            firService.createFIR(cbeCase.getId(), request, "TN-POL-1001", null);
        });
    }

    @Test
    @DisplayName("5. FIR: Cross-station FIR access attempt by Egmore officer is denied (404)")
    void testGetFIR_CrossStationDenied() {
        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(egmOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), egmStation.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            firService.getFIRByCaseId(cbeCase.getId(), "TN-POL-1002");
        });
    }

    /* ==========================================================================
       VICTIM SERVICE TESTS (5 - 8, 14, 18-19)
       ========================================================================== */

    @Test
    @DisplayName("6. Victim: Should create victim record and update parent case timestamp")
    void testCreateVictim_Success() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(victimRepository.save(any(Victim.class))).thenAnswer(invocation -> {
            Victim v = invocation.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        CreateVictimRequest request = new CreateVictimRequest();
        request.setFullName("K. Senthil Kumar");
        request.setAge(42);
        request.setGender("MALE");
        request.setStatus("DECEASED");

        VictimResponse response = victimService.createVictim(cbeCase.getId(), request, "TN-POL-1001", null);

        assertNotNull(response);
        assertEquals("K. Senthil Kumar", response.getFullName());
        assertEquals("DECEASED", response.getStatus());
        assertEquals("TN-POL-1001", response.getCreatedBy().getPoliceId());

        verify(caseRepository, times(1)).save(cbeCase);
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("7. Victim: List victims for case")
    void testListVictims_Success() {
        Victim v = new Victim();
        v.setId(UUID.randomUUID());
        v.setFullName("K. Senthil Kumar");
        v.setStatus("DECEASED");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(victimRepository.findByCaseEntityIdOrderByCreatedAtAsc(cbeCase.getId())).thenReturn(List.of(v));

        List<VictimResponse> victims = victimService.getVictimsByCaseId(cbeCase.getId(), "TN-POL-1001");

        assertNotNull(victims);
        assertEquals(1, victims.size());
        assertEquals("K. Senthil Kumar", victims.get(0).getFullName());
    }

    @Test
    @DisplayName("8. Victim: Get victim by ID")
    void testGetVictimById_Success() {
        UUID victimId = UUID.randomUUID();
        Victim v = new Victim();
        v.setId(victimId);
        v.setFullName("K. Senthil Kumar");
        v.setStatus("ACTIVE");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(victimRepository.findByIdAndCaseEntityId(victimId, cbeCase.getId())).thenReturn(Optional.of(v));

        VictimResponse response = victimService.getVictimById(cbeCase.getId(), victimId, "TN-POL-1001");

        assertNotNull(response);
        assertEquals("K. Senthil Kumar", response.getFullName());
    }

    @Test
    @DisplayName("9. Victim: Update victim record")
    void testUpdateVictim_Success() {
        UUID victimId = UUID.randomUUID();
        Victim v = new Victim();
        v.setId(victimId);
        v.setFullName("K. Senthil Kumar");
        v.setStatus("ACTIVE");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(victimRepository.findByIdAndCaseEntityId(victimId, cbeCase.getId())).thenReturn(Optional.of(v));
        when(victimRepository.save(any(Victim.class))).thenReturn(v);

        UpdateVictimRequest request = new UpdateVictimRequest();
        request.setFullName("K. Senthil Kumar Updated");
        request.setStatus("IDENTIFIED");

        VictimResponse response = victimService.updateVictim(cbeCase.getId(), victimId, request, "TN-POL-1001", null);

        assertNotNull(response);
        assertEquals("K. Senthil Kumar Updated", response.getFullName());
        assertEquals("IDENTIFIED", response.getStatus());
        verify(caseRepository, times(1)).save(cbeCase);
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("10. Victim: Cross-station victim access attempt is denied")
    void testGetVictims_CrossStationDenied() {
        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(egmOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), egmStation.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            victimService.getVictimsByCaseId(cbeCase.getId(), "TN-POL-1002");
        });
    }

    /* ==========================================================================
       SUSPECT SERVICE TESTS (9 - 12, 15, 18-19)
       ========================================================================== */

    @Test
    @DisplayName("11. Suspect: Should create suspect record with valid status")
    void testCreateSuspect_Success() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(suspectRepository.save(any(Suspect.class))).thenAnswer(invocation -> {
            Suspect s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        CreateSuspectRequest request = new CreateSuspectRequest();
        request.setFullName("M. Rajesh");
        request.setAlias("Snake Rajesh");
        request.setInvestigationStatus("UNDER_INVESTIGATION");

        SuspectResponse response = suspectService.createSuspect(cbeCase.getId(), request, "TN-POL-1001", null);

        assertNotNull(response);
        assertEquals("M. Rajesh", response.getFullName());
        assertEquals("UNDER_INVESTIGATION", response.getInvestigationStatus());
        assertEquals("TN-POL-1001", response.getCreatedBy().getPoliceId());

        verify(caseRepository, times(1)).save(cbeCase);
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("12. Suspect: List suspects for case")
    void testListSuspects_Success() {
        Suspect s = new Suspect();
        s.setId(UUID.randomUUID());
        s.setFullName("M. Rajesh");
        s.setInvestigationStatus("UNDER_INVESTIGATION");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(suspectRepository.findByCaseEntityIdOrderByCreatedAtAsc(cbeCase.getId())).thenReturn(List.of(s));

        List<SuspectResponse> suspects = suspectService.getSuspectsByCaseId(cbeCase.getId(), "TN-POL-1001");

        assertNotNull(suspects);
        assertEquals(1, suspects.size());
        assertEquals("M. Rajesh", suspects.get(0).getFullName());
    }

    @Test
    @DisplayName("13. Suspect: Get suspect by ID")
    void testGetSuspectById_Success() {
        UUID suspectId = UUID.randomUUID();
        Suspect s = new Suspect();
        s.setId(suspectId);
        s.setFullName("M. Rajesh");
        s.setInvestigationStatus("PERSON_OF_INTEREST");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(suspectRepository.findByIdAndCaseEntityId(suspectId, cbeCase.getId())).thenReturn(Optional.of(s));

        SuspectResponse response = suspectService.getSuspectById(cbeCase.getId(), suspectId, "TN-POL-1001");

        assertNotNull(response);
        assertEquals("M. Rajesh", response.getFullName());
    }

    @Test
    @DisplayName("14. Suspect: Update suspect record")
    void testUpdateSuspect_Success() {
        UUID suspectId = UUID.randomUUID();
        Suspect s = new Suspect();
        s.setId(suspectId);
        s.setFullName("M. Rajesh");
        s.setInvestigationStatus("PERSON_OF_INTEREST");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(suspectRepository.findByIdAndCaseEntityId(suspectId, cbeCase.getId())).thenReturn(Optional.of(s));
        when(suspectRepository.save(any(Suspect.class))).thenReturn(s);

        UpdateSuspectRequest request = new UpdateSuspectRequest();
        request.setFullName("M. Rajesh Updated");
        request.setInvestigationStatus("CLEARED");

        SuspectResponse response = suspectService.updateSuspect(cbeCase.getId(), suspectId, request, "TN-POL-1001", null);

        assertNotNull(response);
        assertEquals("M. Rajesh Updated", response.getFullName());
        assertEquals("CLEARED", response.getInvestigationStatus());
        verify(caseRepository, times(1)).save(cbeCase);
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("15. Suspect: Rejects invalid or guilt-implying suspect statuses (GUILTY, CRIMINAL, CONVICTED)")
    void testCreateSuspect_InvalidStatusRejection() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));

        CreateSuspectRequest request = new CreateSuspectRequest();
        request.setFullName("M. Rajesh");
        request.setInvestigationStatus("GUILTY"); // Non-compliant status

        assertThrows(IllegalArgumentException.class, () -> {
            suspectService.createSuspect(cbeCase.getId(), request, "TN-POL-1001", null);
        });
    }

    @Test
    @DisplayName("16. Suspect: Cross-station suspect access attempt is denied")
    void testGetSuspects_CrossStationDenied() {
        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(egmOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), egmStation.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            suspectService.getSuspectsByCaseId(cbeCase.getId(), "TN-POL-1002");
        });
    }

    @Test
    @DisplayName("17. Suspect: Initial suspect defaults to SYNTHETIC_DEMO imageSource")
    void testCreateSuspect_DefaultsToSyntheticDemo() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(suspectRepository.save(any(Suspect.class))).thenAnswer(invocation -> {
            Suspect saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        CreateSuspectRequest request = new CreateSuspectRequest();
        request.setFullName("M. Rajesh");
        request.setAlias("Snake Rajesh");
        request.setAge(31);
        request.setGender("MALE");

        SuspectResponse response = suspectService.createSuspect(cbeCase.getId(), request, "TN-POL-1001", null);

        assertNotNull(response);
        assertEquals("SYNTHETIC_DEMO", response.getImageSource());
        assertFalse(response.getHasRealPhoto());
        assertNotNull(response.getDefaultImageUrl());
    }

    @Test
    @DisplayName("18. Victim: Initial victim defaults to SYNTHETIC_DEMO imageSource")
    void testCreateVictim_DefaultsToSyntheticDemo() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(victimRepository.save(any(Victim.class))).thenAnswer(invocation -> {
            Victim saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        CreateVictimRequest request = new CreateVictimRequest();
        request.setFullName("K. Senthil Kumar");
        request.setAge(42);
        request.setGender("MALE");

        VictimResponse response = victimService.createVictim(cbeCase.getId(), request, "TN-POL-1001", null);

        assertNotNull(response);
        assertEquals("SYNTHETIC_DEMO", response.getImageSource());
        assertFalse(response.getHasRealPhoto());
        assertNotNull(response.getDefaultImageUrl());
    }

    @Test
    @DisplayName("19. Suspect: Default synthetic dummy portrait generation succeeds")
    void testGetSuspectDefaultImage_GeneratesDummyPortrait() {
        UUID suspectId = UUID.randomUUID();
        Suspect s = new Suspect();
        s.setId(suspectId);
        s.setFullName("M. Rajesh");
        s.setAlias("Snake Rajesh");
        s.setAge(31);
        s.setGender("MALE");
        s.setCaseEntity(cbeCase);
        s.setImageSource("SYNTHETIC_DEMO");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(suspectRepository.findByIdAndCaseEntityId(suspectId, cbeCase.getId())).thenReturn(Optional.of(s));

        SuspectService.StorageServicePhotoResult result = suspectService.getSuspectDefaultImage(cbeCase.getId(), suspectId, "TN-POL-1001");

        assertNotNull(result);
        assertNotNull(result.getResource());
        assertEquals("image/jpeg", result.getContentType());
        assertTrue(result.getFilename().contains("suspect_dummy_"));
    }

    @Test
    @DisplayName("20. Victim: Default synthetic dummy portrait generation succeeds")
    void testGetVictimDefaultImage_GeneratesDummyPortrait() {
        UUID victimId = UUID.randomUUID();
        Victim v = new Victim();
        v.setId(victimId);
        v.setFullName("K. Senthil Kumar");
        v.setAge(42);
        v.setGender("MALE");
        v.setRelationshipToCase("Primary Deceased Victim");
        v.setCaseEntity(cbeCase);
        v.setImageSource("SYNTHETIC_DEMO");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(victimRepository.findByIdAndCaseEntityId(victimId, cbeCase.getId())).thenReturn(Optional.of(v));

        SuspectService.StorageServicePhotoResult result = victimService.getVictimDefaultImage(cbeCase.getId(), victimId, "TN-POL-1001");

        assertNotNull(result);
        assertNotNull(result.getResource());
        assertEquals("image/jpeg", result.getContentType());
        assertTrue(result.getFilename().contains("victim_dummy_"));
    }
}
