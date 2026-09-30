package com.crimesphere.backend;

import com.crimesphere.backend.dto.location.CaseLocationDTO;
import com.crimesphere.backend.dto.location.CreateCaseLocationRequest;
import com.crimesphere.backend.dto.location.LocationIntelligenceSummaryDTO;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.CaseLocation;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.entity.PoliceStation;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.CaseLocationRepository;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import com.crimesphere.backend.service.CaseLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CaseLocationServiceTest {

    @Mock
    private CaseLocationRepository caseLocationRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private PoliceOfficerRepository officerRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private CaseLocationService locationService;

    private PoliceStation cbeStation;
    private PoliceOfficer cbeOfficer;
    private Case cbeCase;

    @BeforeEach
    void setUp() {
        locationService = new CaseLocationService(
                caseLocationRepository,
                caseRepository,
                officerRepository,
                auditLogRepository
        );

        cbeStation = new PoliceStation();
        cbeStation.setId(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));
        cbeStation.setStationName("Coimbatore Central Police Station");

        cbeOfficer = new PoliceOfficer();
        cbeOfficer.setId(UUID.fromString("d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44"));
        cbeOfficer.setPoliceId("TN-POL-1001");
        cbeOfficer.setFullName("SI K. Vijay Anand");
        cbeOfficer.setStation(cbeStation);

        cbeCase = new Case();
        cbeCase.setId(UUID.fromString("a1111111-9c0b-4ef8-bb6d-6bb9bd380001"));
        cbeCase.setCaseNumber("CS-2026-0001");
        cbeCase.setTitle("Homicide at Gandhi Park Area");
        cbeCase.setStatus("UNDER_INVESTIGATION");
        cbeCase.setPriority("HIGH");
        cbeCase.setStation(cbeStation);
        cbeCase.setCreatedBy(cbeOfficer);
    }

    @Test
    @DisplayName("getLocationIntelligence: Returns calculated distance, travel time, and categorized events")
    void testGetLocationIntelligence_Success() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));

        CaseLocation loc1 = new CaseLocation();
        loc1.setId(UUID.randomUUID());
        loc1.setCaseEntity(cbeCase);
        loc1.setEntityType("VICTIM");
        loc1.setEntityName("K. Senthil Kumar");
        loc1.setLatitude(11.0168);
        loc1.setLongitude(76.9558); // Coimbatore Central
        loc1.setLocationName("Coimbatore Central");
        loc1.setLocationStatus("HISTORICAL");
        loc1.setVerificationStatus("VERIFIED");
        loc1.setSource("Authorized Location Data");
        loc1.setRecordedAt(OffsetDateTime.now().minusHours(4));
        loc1.setSequenceOrder(1);

        CaseLocation loc2 = new CaseLocation();
        loc2.setId(UUID.randomUUID());
        loc2.setCaseEntity(cbeCase);
        loc2.setEntityType("CRIME_SCENE");
        loc2.setEntityName("Pollachi Crime Scene");
        loc2.setLatitude(10.6580);
        loc2.setLongitude(77.0090); // Pollachi (~40-42 km distance)
        loc2.setLocationName("Pollachi Outskirts");
        loc2.setLocationStatus("HISTORICAL");
        loc2.setVerificationStatus("VERIFIED");
        loc2.setSource("FIR Record");
        loc2.setRecordedAt(OffsetDateTime.now().minusHours(2));
        loc2.setSequenceOrder(2);

        when(caseLocationRepository.findByCaseEntityIdOrderBySequenceOrderAscRecordedAtAsc(cbeCase.getId()))
                .thenReturn(List.of(loc1, loc2));

        LocationIntelligenceSummaryDTO result = locationService.getLocationIntelligence(cbeCase.getId(), "TN-POL-1001");

        assertNotNull(result);
        assertEquals(cbeCase.getId(), result.getCaseId());
        assertEquals("CS-2026-0001", result.getCaseNumber());
        assertEquals(2, result.getTotalLocations());
        assertEquals(2, result.getTotalEvents());
        assertTrue(result.getTotalDistanceKm() > 35.0 && result.getTotalDistanceKm() < 45.0, "Calculated distance should be ~40-42km");
        assertTrue(result.getEstimatedTravelTimeMinutes() > 45, "Estimated travel time should be > 45 mins");
        assertEquals(2, result.getHistoricalCount());
        assertEquals(0, result.getLiveCount());
    }

    @Test
    @DisplayName("getLocationIntelligence: Returns empty summary when case has no location data")
    void testGetLocationIntelligence_EmptyLocations() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(caseLocationRepository.findByCaseEntityIdOrderBySequenceOrderAscRecordedAtAsc(cbeCase.getId()))
                .thenReturn(Collections.emptyList());

        LocationIntelligenceSummaryDTO result = locationService.getLocationIntelligence(cbeCase.getId(), "TN-POL-1001");

        assertNotNull(result);
        assertEquals(0, result.getTotalLocations());
        assertEquals(0, result.getTotalEvents());
        assertEquals("N/A", result.getFormattedDistance());
        assertEquals("0m", result.getFormattedTravelTime());
    }

    @Test
    @DisplayName("getLocationIntelligence: Throws ResourceNotFoundException for cross-station access attempt")
    void testGetLocationIntelligence_StationIsolationBlocked() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        UUID foreignCaseId = UUID.fromString("b2222222-9c0b-4ef8-bb6d-6bb9bd380001");
        when(caseRepository.findByIdAndStationId(foreignCaseId, cbeStation.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                locationService.getLocationIntelligence(foreignCaseId, "TN-POL-1001"));
    }

    @Test
    @DisplayName("createLocation: Successfully saves location point and returns DTO")
    void testCreateLocation_Success() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));

        CreateCaseLocationRequest req = new CreateCaseLocationRequest();
        req.setEntityType("VICTIM");
        req.setEntityName("K. Senthil Kumar");
        req.setLatitude(11.0168);
        req.setLongitude(76.9680);
        req.setLocationName("Cross Cut Road");
        req.setSource("Authorized Location Data");
        req.setLocationStatus("HISTORICAL");
        req.setVerificationStatus("VERIFIED");

        when(caseLocationRepository.save(any(CaseLocation.class))).thenAnswer(inv -> {
            CaseLocation loc = inv.getArgument(0);
            loc.setId(UUID.randomUUID());
            return loc;
        });

        CaseLocationDTO saved = locationService.createLocation(cbeCase.getId(), req, "TN-POL-1001", null);

        assertNotNull(saved);
        assertEquals("VICTIM", saved.getEntityType());
        assertEquals("Cross Cut Road", saved.getLocationName());
        verify(caseLocationRepository, times(1)).save(any(CaseLocation.class));
    }
}
