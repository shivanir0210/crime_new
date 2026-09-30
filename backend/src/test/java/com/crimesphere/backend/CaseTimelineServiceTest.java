package com.crimesphere.backend;

import com.crimesphere.backend.dto.timeline.TimelineEventDTO;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.repository.*;
import com.crimesphere.backend.service.CaseTimelineService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CaseTimelineServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private PoliceOfficerRepository officerRepository;

    @Mock
    private FIRRepository firRepository;

    @Mock
    private VictimRepository victimRepository;

    @Mock
    private SuspectRepository suspectRepository;

    @Mock
    private WitnessRepository witnessRepository;

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private CaseLocationRepository caseLocationRepository;

    private CaseTimelineService timelineService;

    private PoliceStation cbeStation;
    private PoliceOfficer cbeOfficer;
    private Case cbeCase;

    @BeforeEach
    void setUp() {
        timelineService = new CaseTimelineService(
                caseRepository,
                officerRepository,
                firRepository,
                victimRepository,
                suspectRepository,
                witnessRepository,
                evidenceRepository,
                caseLocationRepository
        );

        cbeStation = new PoliceStation();
        cbeStation.setId(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));
        cbeStation.setStationCode("TN-POLICE-CBE");
        cbeStation.setStationName("Coimbatore Central Police Station");

        cbeOfficer = new PoliceOfficer();
        cbeOfficer.setId(UUID.randomUUID());
        cbeOfficer.setPoliceId("TN-POL-1001");
        cbeOfficer.setFullName("Officer Raman");
        cbeOfficer.setStation(cbeStation);

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
    @DisplayName("Aggregates FIR, Victim, Suspect, Witness, and Evidence into chronological timeline")
    void testGetCaseTimelineAggregatesAllEntities() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));

        // FIR
        FIR fir = new FIR();
        fir.setId(UUID.randomUUID());
        fir.setCaseEntity(cbeCase);
        fir.setFirNumber("FIR-CBE-2026-0001");
        fir.setIncidentDate(OffsetDateTime.now().minusDays(3).minusHours(4));
        fir.setRegistrationDate(OffsetDateTime.now().minusDays(3));
        fir.setIncidentLocation("Gandhi Park, Coimbatore");
        fir.setCreatedBy(cbeOfficer);
        when(firRepository.findByCaseEntityId(cbeCase.getId())).thenReturn(Optional.of(fir));

        // Victim
        Victim v = new Victim();
        v.setId(UUID.randomUUID());
        v.setCaseEntity(cbeCase);
        v.setFullName("K. Senthil Kumar");
        v.setStatus("DECEASED");
        v.setCreatedAt(OffsetDateTime.now().minusDays(3));
        when(victimRepository.findByCaseEntityIdOrderByCreatedAtAsc(cbeCase.getId())).thenReturn(List.of(v));

        // Suspect
        Suspect s = new Suspect();
        s.setId(UUID.randomUUID());
        s.setCaseEntity(cbeCase);
        s.setFullName("M. Rajesh");
        s.setInvestigationStatus("UNDER_INVESTIGATION");
        s.setCreatedAt(OffsetDateTime.now().minusDays(2));
        when(suspectRepository.findByCaseEntityIdOrderByCreatedAtAsc(cbeCase.getId())).thenReturn(List.of(s));

        // Witness
        Witness w = new Witness();
        w.setId(UUID.randomUUID());
        w.setCaseEntity(cbeCase);
        w.setFullName("R. Natarajan");
        w.setWitnessType("EYEWITNESS");
        w.setInterviewDate(OffsetDateTime.now().minusDays(2).plusHours(2));
        when(witnessRepository.findByCaseEntityIdOrderByCreatedAtAsc(cbeCase.getId())).thenReturn(List.of(w));

        // Evidence
        Evidence e = new Evidence();
        e.setId(UUID.randomUUID());
        e.setCaseEntity(cbeCase);
        e.setTitle("Wooden Log Weapon");
        e.setEvidenceType("PHYSICAL");
        e.setStatus("VERIFIED");
        e.setCollectedAt(OffsetDateTime.now().minusDays(2).plusHours(4));
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(cbeCase.getId())).thenReturn(List.of(e));

        List<TimelineEventDTO> timeline = timelineService.getCaseTimeline(cbeCase.getId(), "TN-POL-1001");

        assertNotNull(timeline);
        assertTrue(timeline.size() >= 5);
        // Verify chronological order (most recent first)
        for (int i = 0; i < timeline.size() - 1; i++) {
            OffsetDateTime t1 = timeline.get(i).getTimestamp();
            OffsetDateTime t2 = timeline.get(i + 1).getTimestamp();
            if (t1 != null && t2 != null) {
                assertTrue(t1.isAfter(t2) || t1.isEqual(t2));
            }
        }
    }

    @Test
    @DisplayName("Enforce station isolation - officer from different station cannot view case timeline")
    void testTimelineStationIsolation() {
        PoliceStation egmStation = new PoliceStation();
        egmStation.setId(UUID.randomUUID());
        egmStation.setStationCode("TN-POLICE-EGM");

        PoliceOfficer egmOfficer = new PoliceOfficer();
        egmOfficer.setId(UUID.randomUUID());
        egmOfficer.setPoliceId("TN-POL-1002");
        egmOfficer.setStation(egmStation);

        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(egmOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), egmStation.getId())).thenReturn(Optional.empty());

        assertThrows(com.crimesphere.backend.exception.ResourceNotFoundException.class, () -> {
            timelineService.getCaseTimeline(cbeCase.getId(), "TN-POL-1002");
        });
    }
}
