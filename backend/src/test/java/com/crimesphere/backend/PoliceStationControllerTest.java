package com.crimesphere.backend;

import com.crimesphere.backend.controller.PoliceStationController;
import com.crimesphere.backend.dto.PoliceStationDto;
import com.crimesphere.backend.entity.PoliceStation;
import com.crimesphere.backend.repository.PoliceStationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PoliceStationControllerTest {

    @Mock
    private PoliceStationRepository policeStationRepository;

    private PoliceStationController policeStationController;

    private PoliceStation egmoreStation;
    private PoliceStation coimbatoreStation;
    private PoliceStation salemStation;

    @BeforeEach
    void setUp() {
        policeStationController = new PoliceStationController(policeStationRepository);

        egmoreStation = new PoliceStation(
                UUID.fromString("b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22"),
                "TN-POLICE-EGM",
                "Egmore Police Station",
                "Chennai",
                "Tamil Nadu",
                "Egmore Station Head",
                "head.egm@example.com"
        );

        coimbatoreStation = new PoliceStation(
                UUID.fromString("c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a33"),
                "TN-POLICE-CBE",
                "Coimbatore Central Police Station",
                "Coimbatore",
                "Tamil Nadu",
                "Coimbatore Station Head",
                "head.cbe@example.com"
        );

        salemStation = new PoliceStation(
                UUID.fromString("d2eebc99-9c0b-4ef8-bb6d-6bb9bd380a44"),
                "TN-POLICE-SLM",
                "Salem Central Police Station",
                "Salem",
                "Tamil Nadu",
                "Salem Station Head",
                "head.slm@example.com"
        );
    }

    @Test
    @DisplayName("GET /api/police-stations returns all database stations with valid UUIDs and fields")
    void testGetAllPoliceStations() {
        // Given
        when(policeStationRepository.findAllByOrderByStationNameAsc())
                .thenReturn(Arrays.asList(coimbatoreStation, egmoreStation, salemStation));

        // When
        ResponseEntity<List<PoliceStationDto>> response = policeStationController.getAllPoliceStations();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<PoliceStationDto> stations = response.getBody();
        assertNotNull(stations);
        assertEquals(3, stations.size());

        // Verify Coimbatore
        PoliceStationDto cbe = stations.get(0);
        assertEquals(UUID.fromString("c1eebc99-9c0b-4ef8-bb6d-6bb9bd380a33"), cbe.getId());
        assertEquals("Coimbatore Central Police Station", cbe.getStationName());
        assertEquals("TN-POLICE-CBE", cbe.getStationCode());
        assertEquals("Coimbatore", cbe.getDistrict());
        assertEquals("Tamil Nadu", cbe.getState());

        // Verify Egmore
        PoliceStationDto egm = stations.get(1);
        assertEquals(UUID.fromString("b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22"), egm.getId());
        assertEquals("Egmore Police Station", egm.getStationName());
        assertEquals("TN-POLICE-EGM", egm.getStationCode());
        assertEquals("Chennai", egm.getDistrict());
        assertEquals("Tamil Nadu", egm.getState());

        // Verify Salem
        PoliceStationDto slm = stations.get(2);
        assertEquals(UUID.fromString("d2eebc99-9c0b-4ef8-bb6d-6bb9bd380a44"), slm.getId());
        assertEquals("Salem Central Police Station", slm.getStationName());
        assertEquals("TN-POLICE-SLM", slm.getStationCode());
        assertEquals("Salem", slm.getDistrict());
        assertEquals("Tamil Nadu", slm.getState());

        verify(policeStationRepository, times(1)).findAllByOrderByStationNameAsc();
    }

    @Test
    @DisplayName("GET /api/police-stations does NOT expose station head email in public listing")
    void testHeadEmailNotExposedInPublicListing() {
        // Given
        when(policeStationRepository.findAllByOrderByStationNameAsc())
                .thenReturn(Arrays.asList(egmoreStation, coimbatoreStation, salemStation));

        // When
        ResponseEntity<List<PoliceStationDto>> response = policeStationController.getAllPoliceStations();

        // Then
        List<PoliceStationDto> stations = response.getBody();
        assertNotNull(stations);
        for (PoliceStationDto dto : stations) {
            assertNull(dto.getHeadEmail(), "headEmail MUST NOT be exposed in public police-stations listing");
        }
    }
}
