package com.crimesphere.backend;

import com.crimesphere.backend.dto.interstation.CreateInterStationRequestDto;
import com.crimesphere.backend.dto.interstation.InterStationRequestResponseDto;
import com.crimesphere.backend.dto.interstation.RespondInterStationRequestDto;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import com.crimesphere.backend.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InterStationRequestServiceTest {

    @Mock
    private InterStationRequestRepository requestRepository;

    @Mock
    private PoliceStationRepository stationRepository;

    @Mock
    private PoliceOfficerRepository officerRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private VictimRepository victimRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private HttpServletRequest httpRequest;

    private NotificationService notificationService;
    private TestResendEmailService resendEmailService;
    private InterStationRequestService service;

    private PoliceStation sourceStation;
    private PoliceStation destStation;
    private PoliceStation thirdStation;
    private PoliceOfficer sourceOfficer;
    private PoliceOfficer destOfficer;
    private PoliceOfficer thirdOfficer;
    private Case testCase;
    private Victim testVictim;

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

    private static class TestResendEmailService extends ResendEmailService {
        private NotificationDispatchResult resultToReturn = NotificationDispatchResult.success("re_msg_egm_12345");
        private int sendCount = 0;
        private String lastToEmail;

        public TestResendEmailService() {
            super(null, null);
        }

        public void setResultToReturn(NotificationDispatchResult result) {
            this.resultToReturn = result;
        }

        public int getSendCount() {
            return sendCount;
        }

        public String getLastToEmail() {
            return lastToEmail;
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public NotificationDispatchResult sendEmail(String toEmail, String subject, String textBody, String htmlBody) {
            this.sendCount++;
            this.lastToEmail = toEmail;
            return resultToReturn;
        }
    }

    @BeforeEach
    void setUp() {
        notificationService = new TestNotificationService();
        resendEmailService = new TestResendEmailService();

        service = new InterStationRequestService(
                requestRepository,
                stationRepository,
                officerRepository,
                caseRepository,
                victimRepository,
                auditLogRepository,
                resendEmailService,
                notificationService
        );

        sourceStation = new PoliceStation();
        sourceStation.setId(UUID.randomUUID());
        sourceStation.setStationCode("TN-POLICE-CBE");
        sourceStation.setStationName("Coimbatore Central Police Station");
        sourceStation.setDistrict("Coimbatore");
        sourceStation.setState("Tamil Nadu");
        sourceStation.setHeadName("CBE Station Head");
        sourceStation.setHeadEmail("head.cbe@example.com");

        destStation = new PoliceStation();
        destStation.setId(UUID.randomUUID());
        destStation.setStationCode("TN-POLICE-EGM");
        destStation.setStationName("Egmore Police Station");
        destStation.setDistrict("Chennai");
        destStation.setState("Tamil Nadu");
        destStation.setHeadName("Egmore Station Head");
        destStation.setHeadEmail("head.egm@example.com");

        thirdStation = new PoliceStation();
        thirdStation.setId(UUID.randomUUID());
        thirdStation.setStationCode("TN-POLICE-SLM");
        thirdStation.setStationName("Salem Central Police Station");
        thirdStation.setDistrict("Salem");
        thirdStation.setState("Tamil Nadu");
        thirdStation.setHeadName("Salem Station Head");
        thirdStation.setHeadEmail("head.slm@example.com");

        sourceOfficer = new PoliceOfficer();
        sourceOfficer.setId(UUID.randomUUID());
        sourceOfficer.setPoliceId("TN-POL-1001");
        sourceOfficer.setFullName("Development Officer One");
        sourceOfficer.setEmail("officer1@example.com");
        sourceOfficer.setStation(sourceStation);

        destOfficer = new PoliceOfficer();
        destOfficer.setId(UUID.randomUUID());
        destOfficer.setPoliceId("TN-POL-1002");
        destOfficer.setFullName("Development Officer Two");
        destOfficer.setEmail("officer2@example.com");
        destOfficer.setStation(destStation);

        thirdOfficer = new PoliceOfficer();
        thirdOfficer.setId(UUID.randomUUID());
        thirdOfficer.setPoliceId("TN-POL-1004");
        thirdOfficer.setFullName("Development Officer Three");
        thirdOfficer.setEmail("officer3@example.com");
        thirdOfficer.setStation(thirdStation);

        testCase = new Case();
        testCase.setId(UUID.randomUUID());
        testCase.setCaseNumber("CASE-1024");
        testCase.setTitle("Murder Investigation");
        testCase.setStation(sourceStation);
        testCase.setCreatedBy(sourceOfficer);
        testCase.setDescription("Suspicious incident in jurisdiction");
        testCase.setPriority("HIGH");
        testCase.setStatus("OPEN");

        testVictim = new Victim();
        testVictim.setId(UUID.randomUUID());
        testVictim.setFullName("John Doe");
        testVictim.setAge(35);
        testVictim.setGender("MALE");
        testVictim.setAddress("42 Marina Beach Road, Chennai");
        testVictim.setStatus("ACTIVE");
        testVictim.setCaseEntity(testCase);
    }

    @Test
    @DisplayName("1. Successfully creates inter-station request and dispatches Resend email")
    void testCreateRequestSuccess() {
        CreateInterStationRequestDto dto = new CreateInterStationRequestDto();
        dto.setCaseId(testCase.getId());
        dto.setVictimId(testVictim.getId());
        dto.setDestinationStationId(destStation.getId());
        dto.setReason("Victim's primary residence is within Egmore jurisdiction.");
        dto.setShareCaseReference(true);
        dto.setShareVictimDetails(true);
        dto.setShareIncidentSummary(true);

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(sourceOfficer));
        when(caseRepository.findByIdAndStationId(testCase.getId(), sourceStation.getId())).thenReturn(Optional.of(testCase));
        when(victimRepository.findByIdAndCaseEntityId(testVictim.getId(), testCase.getId())).thenReturn(Optional.of(testVictim));
        when(stationRepository.findById(destStation.getId())).thenReturn(Optional.of(destStation));
        when(requestRepository.save(any(InterStationRequest.class))).thenAnswer(i -> {
            InterStationRequest req = i.getArgument(0);
            if (req.getId() == null) req.setId(UUID.randomUUID());
            return req;
        });

        InterStationRequestResponseDto response = service.createRequest(dto, "TN-POL-1001", httpRequest);

        assertNotNull(response);
        assertEquals("SENT", response.getStatus());
        assertEquals("SENT", response.getEmailDeliveryStatus());
        assertEquals("re_msg_egm_12345", response.getProviderMessageId());
        assertEquals("Egmore Police Station", response.getDestinationStationName());
        assertEquals("Coimbatore Central Police Station", response.getSourceStationName());
        assertEquals("John Doe", response.getVictimName());

        assertEquals(1, resendEmailService.getSendCount());
        assertEquals("head.egm@example.com", resendEmailService.getLastToEmail());
        verify(auditLogRepository, atLeast(2)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("2. Destination station lookup: throws 404 for invalid destination station")
    void testInvalidDestinationStation() {
        UUID randomStationId = UUID.randomUUID();
        CreateInterStationRequestDto dto = new CreateInterStationRequestDto();
        dto.setCaseId(testCase.getId());
        dto.setDestinationStationId(randomStationId);
        dto.setReason("Inquiry");
        dto.setShareCaseReference(true);

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(sourceOfficer));
        when(caseRepository.findByIdAndStationId(testCase.getId(), sourceStation.getId())).thenReturn(Optional.of(testCase));
        when(stationRepository.findById(randomStationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            service.createRequest(dto, "TN-POL-1001", httpRequest);
        });
    }

    @Test
    @DisplayName("3. Destination station head_email missing: marks DELIVERY_FAILED without calling Resend")
    void testMissingDestinationHeadEmail() {
        destStation.setHeadEmail(null);

        CreateInterStationRequestDto dto = new CreateInterStationRequestDto();
        dto.setCaseId(testCase.getId());
        dto.setDestinationStationId(destStation.getId());
        dto.setReason("Jurisdiction inquiry");
        dto.setShareCaseReference(true);

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(sourceOfficer));
        when(caseRepository.findByIdAndStationId(testCase.getId(), sourceStation.getId())).thenReturn(Optional.of(testCase));
        when(stationRepository.findById(destStation.getId())).thenReturn(Optional.of(destStation));
        when(requestRepository.save(any(InterStationRequest.class))).thenAnswer(i -> {
            InterStationRequest req = i.getArgument(0);
            if (req.getId() == null) req.setId(UUID.randomUUID());
            return req;
        });

        InterStationRequestResponseDto response = service.createRequest(dto, "TN-POL-1001", httpRequest);

        assertNotNull(response);
        assertEquals("DELIVERY_FAILED", response.getStatus());
        assertEquals("FAILED", response.getEmailDeliveryStatus());
        assertTrue(response.getEmailDeliveryError().contains("Destination station head email is not configured"));
        assertEquals(0, resendEmailService.getSendCount());
    }

    @Test
    @DisplayName("4. Resend failure marks request DELIVERY_FAILED without losing record")
    void testResendFailureHandling() {
        resendEmailService.setResultToReturn(NotificationDispatchResult.failure("Resend dispatch failure (HTTP 401): API key invalid"));

        CreateInterStationRequestDto dto = new CreateInterStationRequestDto();
        dto.setCaseId(testCase.getId());
        dto.setDestinationStationId(destStation.getId());
        dto.setReason("Jurisdiction inquiry");
        dto.setShareCaseReference(true);

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(sourceOfficer));
        when(caseRepository.findByIdAndStationId(testCase.getId(), sourceStation.getId())).thenReturn(Optional.of(testCase));
        when(stationRepository.findById(destStation.getId())).thenReturn(Optional.of(destStation));
        when(requestRepository.save(any(InterStationRequest.class))).thenAnswer(i -> {
            InterStationRequest req = i.getArgument(0);
            if (req.getId() == null) req.setId(UUID.randomUUID());
            return req;
        });

        InterStationRequestResponseDto response = service.createRequest(dto, "TN-POL-1001", httpRequest);

        assertNotNull(response);
        assertEquals("DELIVERY_FAILED", response.getStatus());
        assertEquals("FAILED", response.getEmailDeliveryStatus());
        assertTrue(response.getEmailDeliveryError().contains("API key invalid"));
    }

    @Test
    @DisplayName("5. Station Isolation: Unauthorized third-party station access throws AccessDeniedException (403)")
    void testStationIsolationThirdPartyAccessDenied() {
        InterStationRequest req = new InterStationRequest();
        req.setId(UUID.randomUUID());
        req.setSourceStation(sourceStation);
        req.setDestinationStation(destStation);
        req.setCaseEntity(testCase);
        req.setRequestedByOfficer(sourceOfficer);
        req.setStatus("SENT");

        when(officerRepository.findByPoliceId("TN-POL-1004")).thenReturn(Optional.of(thirdOfficer));
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        assertThrows(AccessDeniedException.class, () -> {
            service.getRequestById(req.getId(), "TN-POL-1004");
        });
    }

    @Test
    @DisplayName("6. Station Isolation: Destination station can view received requests")
    void testDestinationStationCanViewReceivedRequest() {
        InterStationRequest req = new InterStationRequest();
        req.setId(UUID.randomUUID());
        req.setSourceStation(sourceStation);
        req.setDestinationStation(destStation);
        req.setCaseEntity(testCase);
        req.setRequestedByOfficer(sourceOfficer);
        req.setStatus("SENT");

        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(destOfficer));
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        InterStationRequestResponseDto response = service.getRequestById(req.getId(), "TN-POL-1002");
        assertNotNull(response);
        assertEquals(req.getId(), response.getId());
    }

    @Test
    @DisplayName("7. Station Isolation: Source station can view sent requests")
    void testSourceStationCanViewSentRequest() {
        InterStationRequest req = new InterStationRequest();
        req.setId(UUID.randomUUID());
        req.setSourceStation(sourceStation);
        req.setDestinationStation(destStation);
        req.setCaseEntity(testCase);
        req.setRequestedByOfficer(sourceOfficer);
        req.setStatus("SENT");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(sourceOfficer));
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        InterStationRequestResponseDto response = service.getRequestById(req.getId(), "TN-POL-1001");
        assertNotNull(response);
        assertEquals(req.getId(), response.getId());
    }

    @Test
    @DisplayName("8. Destination station accepts request: updates status to ACCEPTED and creates audit")
    void testAcceptRequest() {
        InterStationRequest req = new InterStationRequest();
        req.setId(UUID.randomUUID());
        req.setSourceStation(sourceStation);
        req.setDestinationStation(destStation);
        req.setCaseEntity(testCase);
        req.setStatus("SENT");

        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(destOfficer));
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));
        when(requestRepository.save(any(InterStationRequest.class))).thenAnswer(i -> i.getArgument(0));

        InterStationRequestResponseDto response = service.acceptRequest(req.getId(), "TN-POL-1002", httpRequest);

        assertEquals("ACCEPTED", response.getStatus());
        assertEquals("TN-POL-1002", response.getRespondedByPoliceId());
        assertNotNull(response.getRespondedAt());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("9. Source station cannot accept its own sent request (403 Access Denied)")
    void testSourceStationCannotAcceptRequest() {
        InterStationRequest req = new InterStationRequest();
        req.setId(UUID.randomUUID());
        req.setSourceStation(sourceStation);
        req.setDestinationStation(destStation);
        req.setCaseEntity(testCase);
        req.setStatus("SENT");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(sourceOfficer));
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        assertThrows(AccessDeniedException.class, () -> {
            service.acceptRequest(req.getId(), "TN-POL-1001", httpRequest);
        });
    }

    @Test
    @DisplayName("10. Destination station rejects request: updates status to REJECTED with notes")
    void testRejectRequest() {
        InterStationRequest req = new InterStationRequest();
        req.setId(UUID.randomUUID());
        req.setSourceStation(sourceStation);
        req.setDestinationStation(destStation);
        req.setCaseEntity(testCase);
        req.setStatus("SENT");

        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(destOfficer));
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));
        when(requestRepository.save(any(InterStationRequest.class))).thenAnswer(i -> i.getArgument(0));

        RespondInterStationRequestDto dto = new RespondInterStationRequestDto("Address belongs to Mylapore PS jurisdiction.");
        InterStationRequestResponseDto response = service.rejectRequest(req.getId(), dto, "TN-POL-1002", httpRequest);

        assertEquals("REJECTED", response.getStatus());
        assertEquals("Address belongs to Mylapore PS jurisdiction.", response.getResponseNotes());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("11. Destination station responds/completes request: updates status to COMPLETED")
    void testRespondRequest() {
        InterStationRequest req = new InterStationRequest();
        req.setId(UUID.randomUUID());
        req.setSourceStation(sourceStation);
        req.setDestinationStation(destStation);
        req.setCaseEntity(testCase);
        req.setStatus("ACCEPTED");

        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(destOfficer));
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));
        when(requestRepository.save(any(InterStationRequest.class))).thenAnswer(i -> i.getArgument(0));

        RespondInterStationRequestDto dto = new RespondInterStationRequestDto("Victim statement verified and forwarded.");
        InterStationRequestResponseDto response = service.respondToRequest(req.getId(), dto, "TN-POL-1002", httpRequest);

        assertEquals("COMPLETED", response.getStatus());
        assertEquals("Victim statement verified and forwarded.", response.getResponseNotes());
        verify(auditLogRepository, times(2)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("12. Cannot send coordination request to own station")
    void testCannotSendToOwnStation() {
        CreateInterStationRequestDto dto = new CreateInterStationRequestDto();
        dto.setCaseId(testCase.getId());
        dto.setDestinationStationId(sourceStation.getId()); // same station
        dto.setReason("Test");
        dto.setShareCaseReference(true);

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(sourceOfficer));
        when(caseRepository.findByIdAndStationId(testCase.getId(), sourceStation.getId())).thenReturn(Optional.of(testCase));
        when(stationRepository.findById(sourceStation.getId())).thenReturn(Optional.of(sourceStation));

        assertThrows(IllegalArgumentException.class, () -> {
            service.createRequest(dto, "TN-POL-1001", httpRequest);
        });
    }

    @Test
    @DisplayName("13. Case not belonging to officer's station throws 404")
    void testCaseNotAuthorizedForStation() {
        CreateInterStationRequestDto dto = new CreateInterStationRequestDto();
        dto.setCaseId(testCase.getId());
        dto.setDestinationStationId(destStation.getId());
        dto.setReason("Test");
        dto.setShareCaseReference(true);

        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(destOfficer));
        when(caseRepository.findByIdAndStationId(testCase.getId(), destStation.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            service.createRequest(dto, "TN-POL-1002", httpRequest);
        });
    }
}
