package com.crimesphere.backend;

import com.crimesphere.backend.dto.CaseOfficerDto;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import com.crimesphere.backend.service.CaseOfficerService;
import com.crimesphere.backend.service.NotificationService;
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
public class CaseOfficerServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private PoliceOfficerRepository policeOfficerRepository;

    @Mock
    private CaseOfficerRepository caseOfficerRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private NotificationService notificationService;
    private CaseOfficerService caseOfficerService;

    private PoliceStation station;
    private PoliceOfficer officer1;
    private PoliceOfficer officer2;
    private Case testCase;

    private static class TestNotificationService extends NotificationService {
        @Override
        public void dispatchCaseNotification(Case caseEntity, NotificationType notificationType, String subject, String message, String actionRequired, PoliceOfficer triggerOfficer) {
            // no-op test double
        }
        @Override
        public Notification createAndSendNotification(Case caseEntity, PoliceOfficer recipientOfficer, NotificationChannel channel, NotificationType notificationType, String subject, String message, String actionRequired, String recipientEmail, String recipientPhone) {
            return new Notification();
        }
    }

    @BeforeEach
    void setUp() {
        notificationService = new TestNotificationService();
        caseOfficerService = new CaseOfficerService(
                caseRepository,
                policeOfficerRepository,
                caseOfficerRepository,
                auditLogRepository,
                notificationService
        );

        station = new PoliceStation();
        station.setId(UUID.randomUUID());
        station.setStationCode("PS-COIMB-01");
        station.setStationName("Coimbatore Central PS");

        officer1 = new PoliceOfficer();
        officer1.setId(UUID.randomUUID());
        officer1.setPoliceId("TN-1001");
        officer1.setFullName("Insp. V. Rathore");
        officer1.setRole("INVESTIGATOR");
        officer1.setStation(station);

        officer2 = new PoliceOfficer();
        officer2.setId(UUID.randomUUID());
        officer2.setPoliceId("TN-1002");
        officer2.setFullName("Sub-Insp. K. Sharma");
        officer2.setRole("OFFICER");
        officer2.setStation(station);

        testCase = new Case();
        testCase.setId(UUID.randomUUID());
        testCase.setCaseNumber("CS-2026-0001");
        testCase.setTitle("Homicide Investigation");
        testCase.setStation(station);
        testCase.setCreatedBy(officer1);
    }

    @Test
    @DisplayName("Should assign officer to case within same police station")
    void testAssignOfficerSuccess() {
        when(policeOfficerRepository.findByPoliceId("TN-1001")).thenReturn(Optional.of(officer1));
        when(caseRepository.findById(testCase.getId())).thenReturn(Optional.of(testCase));
        when(policeOfficerRepository.findById(officer2.getId())).thenReturn(Optional.of(officer2));
        when(caseOfficerRepository.findByCaseEntityIdAndPoliceOfficerId(testCase.getId(), officer2.getId()))
                .thenReturn(Optional.empty());

        CaseOfficer savedCo = new CaseOfficer(testCase, officer2, officer1);
        savedCo.setId(UUID.randomUUID());
        when(caseOfficerRepository.save(any(CaseOfficer.class))).thenReturn(savedCo);

        CaseOfficerDto result = caseOfficerService.assignOfficerToCase(testCase.getId(), officer2.getId(), "TN-1001");

        assertNotNull(result);
        assertEquals(officer2.getId(), result.getOfficerId());
        assertEquals("Sub-Insp. K. Sharma", result.getFullName());
        verify(caseOfficerRepository, times(1)).save(any(CaseOfficer.class));
    }

    @Test
    @DisplayName("Should enforce station isolation when assigning officers")
    void testAssignOfficerStationIsolation() {
        PoliceStation otherStation = new PoliceStation();
        otherStation.setId(UUID.randomUUID());
        otherStation.setStationName("Chennai Central PS");

        PoliceOfficer outsider = new PoliceOfficer();
        outsider.setId(UUID.randomUUID());
        outsider.setPoliceId("TN-9999");
        outsider.setFullName("Insp. Outsider");
        outsider.setStation(otherStation);
        outsider.setRole("OFFICER");

        when(policeOfficerRepository.findByPoliceId("TN-1001")).thenReturn(Optional.of(officer1));
        when(caseRepository.findById(testCase.getId())).thenReturn(Optional.of(testCase));
        when(policeOfficerRepository.findById(outsider.getId())).thenReturn(Optional.of(outsider));

        assertThrows(IllegalArgumentException.class, () -> {
            caseOfficerService.assignOfficerToCase(testCase.getId(), outsider.getId(), "TN-1001");
        });
    }

    @Test
    @DisplayName("Should remove officer assignment from case")
    void testRemoveOfficerFromCase() {
        CaseOfficer co = new CaseOfficer(testCase, officer2, officer1);
        when(policeOfficerRepository.findByPoliceId("TN-1001")).thenReturn(Optional.of(officer1));
        when(caseRepository.findById(testCase.getId())).thenReturn(Optional.of(testCase));
        when(caseOfficerRepository.findByCaseEntityIdAndPoliceOfficerId(testCase.getId(), officer2.getId()))
                .thenReturn(Optional.of(co));

        caseOfficerService.removeOfficerFromCase(testCase.getId(), officer2.getId(), "TN-1001");

        assertFalse(co.isActive());
        verify(caseOfficerRepository, times(1)).save(co);
    }

    @Test
    @DisplayName("Should list assigned officers for a case including case creator")
    void testGetAssignedOfficers() {
        CaseOfficer co = new CaseOfficer(testCase, officer2, officer1);
        when(policeOfficerRepository.findByPoliceId("TN-1001")).thenReturn(Optional.of(officer1));
        when(caseRepository.findById(testCase.getId())).thenReturn(Optional.of(testCase));
        when(caseOfficerRepository.findByCaseEntityIdAndIsActiveTrue(testCase.getId()))
                .thenReturn(List.of(co));

        List<CaseOfficerDto> list = caseOfficerService.getAssignedOfficers(testCase.getId(), "TN-1001");

        assertNotNull(list);
        assertTrue(list.size() >= 1);
    }
}
