package com.crimesphere.backend;

import com.crimesphere.backend.dto.CaseReportSummaryDTO;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import com.crimesphere.backend.service.CaseReportDataBuilder;
import com.crimesphere.backend.service.CaseReportService;
import com.crimesphere.backend.service.CaseStateService;
import com.crimesphere.backend.service.PdfReportGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CaseReportServiceTest {

    @Mock private CaseRepository caseRepository;
    @Mock private FIRRepository firRepository;
    @Mock private VictimRepository victimRepository;
    @Mock private SuspectRepository suspectRepository;
    @Mock private EvidenceRepository evidenceRepository;
    @Mock private EvidenceGapRepository evidenceGapRepository;
    @Mock private AiRecommendationRepository aiRecommendationRepository;
    @Mock private RecommendationVerificationRepository recommendationVerificationRepository;
    @Mock private InvestigatorReviewRepository investigatorReviewRepository;
    @Mock private PublicInformationItemRepository publicInformationItemRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private CaseReportGenerationRepository reportGenerationRepository;
    @Mock private PoliceOfficerRepository officerRepository;
    @Mock private AuditLogRepository auditLogRepository;

    private CaseStateService caseStateService;
    private CaseReportDataBuilder dataBuilder;
    private PdfReportGenerator pdfGenerator;
    private CaseReportService caseReportService;

    private UUID caseId;
    private UUID stationId;
    private PoliceStation station;
    private PoliceOfficer officer;
    private Case crimeCase;

    @BeforeEach
    public void setUp() {
        caseStateService = new CaseStateService(
                caseRepository, firRepository, victimRepository,
                suspectRepository, evidenceRepository, officerRepository, auditLogRepository
        );

        dataBuilder = new CaseReportDataBuilder(
                caseRepository, firRepository, victimRepository, suspectRepository,
                evidenceRepository, evidenceGapRepository, caseStateService,
                aiRecommendationRepository, recommendationVerificationRepository,
                investigatorReviewRepository, publicInformationItemRepository,
                notificationRepository
        );

        pdfGenerator = new PdfReportGenerator();

        caseReportService = new CaseReportService(
                dataBuilder, pdfGenerator, reportGenerationRepository,
                officerRepository, caseRepository, auditLogRepository
        );

        caseId = UUID.randomUUID();
        stationId = UUID.randomUUID();

        station = new PoliceStation();
        station.setId(stationId);
        station.setStationName("Peelamedu PS");
        station.setDistrict("Coimbatore");
        station.setStationCode("TN-CBE-04");

        officer = new PoliceOfficer();
        officer.setId(UUID.randomUUID());
        officer.setPoliceId("TN-POL-1001");
        officer.setFullName("Inspector Vijay");
        officer.setStation(station);

        crimeCase = new Case();
        crimeCase.setId(caseId);
        crimeCase.setCaseNumber("CS-2026-0001");
        crimeCase.setTitle("Jewelry Store Heist Investigation");
        crimeCase.setDescription("Armed robbery reported at Gold Plaza");
        crimeCase.setCaseType("THEFT");
        crimeCase.setPriority("HIGH");
        crimeCase.setStatus("UNDER_INVESTIGATION");
        crimeCase.setStation(station);
        crimeCase.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    public void testGenerateCaseReportPdf_Success() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(officer));
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(crimeCase));
        when(firRepository.findByCaseEntityId(caseId)).thenReturn(Optional.empty());
        when(victimRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId)).thenReturn(Collections.emptyList());
        when(suspectRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId)).thenReturn(Collections.emptyList());
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        when(aiRecommendationRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(investigatorReviewRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(publicInformationItemRepository.findByCrimeCaseIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(notificationRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        byte[] pdfBytes = caseReportService.generateCaseReportPdf(caseId, "TN-POL-1001");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);

        verify(reportGenerationRepository, times(1)).save(any(CaseReportGeneration.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    public void testGenerateCaseReportPdf_MissingCase_ThrowsException() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(officer));
        when(caseRepository.findById(caseId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            caseReportService.generateCaseReportPdf(caseId, "TN-POL-1001");
        });
    }

    @Test
    public void testGenerateCaseReportPdf_UnauthorizedStation_ThrowsAccessDenied() {
        PoliceStation otherStation = new PoliceStation();
        otherStation.setId(UUID.randomUUID());
        otherStation.setStationName("Madurai Central PS");
        crimeCase.setStation(otherStation);

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(officer));
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(crimeCase));

        assertThrows(AccessDeniedException.class, () -> {
            caseReportService.generateCaseReportPdf(caseId, "TN-POL-1001");
        });
    }

    @Test
    public void testReportData_IncludesFirVictimsSuspectsAndEvidence() {
        FIR fir = new FIR();
        fir.setFirNumber("FIR-CBE-2026-0101");
        fir.setComplainantName("S. Ramanathan");
        fir.setComplainantContact("9876543210");
        fir.setOffenceType("ARMED_ROBBERY");
        fir.setSectionsOfLaw("Section 392/397 IPC");
        fir.setRegistrationDate(OffsetDateTime.now());
        when(firRepository.findByCaseEntityId(caseId)).thenReturn(Optional.of(fir));

        Victim v = new Victim();
        v.setFullName("K. Sundaram");
        v.setAge(45);
        v.setGender("MALE");
        v.setOccupation("Store Manager");
        v.setStatus("ACTIVE");
        when(victimRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId)).thenReturn(List.of(v));

        Suspect s = new Suspect();
        s.setFullName("R. Vikram");
        s.setAlias("Vicky");
        s.setAge(32);
        s.setGender("MALE");
        s.setInvestigationStatus("UNDER_INVESTIGATION");
        when(suspectRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId)).thenReturn(List.of(s));

        Evidence e = new Evidence();
        e.setEvidenceNumber("EVD-2026-0001");
        e.setTitle("CCTV Security Footage");
        e.setOriginalFileName("cam_01.mp4");
        e.setMimeType("video/mp4");
        e.setFileSize(15420000L);
        e.setFileHash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        e.setStatus("VERIFIED");
        e.setEvidenceType("CCTV");
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(e));

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(officer));
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(crimeCase));
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        when(aiRecommendationRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(investigatorReviewRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(publicInformationItemRepository.findByCrimeCaseIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(notificationRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        CaseReportSummaryDTO summary = caseReportService.getReportSummary(caseId, "TN-POL-1001");

        assertNotNull(summary);
        assertTrue(summary.isFirExists());
        assertEquals("FIR-CBE-2026-0101", summary.getFirNumber());
        assertEquals(1, summary.getVictims().size());
        assertEquals("K. Sundaram", summary.getVictims().get(0).getFullName());
        assertEquals(1, summary.getSuspects().size());
        assertEquals("R. Vikram", summary.getSuspects().get(0).getFullName());
        assertEquals("UNDER_INVESTIGATION", summary.getSuspects().get(0).getInvestigationStatus());
        assertEquals(1, summary.getTotalEvidenceCount());
        assertEquals(1, summary.getVerifiedEvidenceCount());
        assertEquals("EVD-2026-0001", summary.getEvidenceItems().get(0).getEvidenceNumber());
    }

    @Test
    public void testReportData_PreservesOriginalAiFindingOnEdit() {
        InvestigatorReview review = new InvestigatorReview();
        review.setId(UUID.randomUUID());

        AiRecommendationItem item = new AiRecommendationItem();
        item.setId(UUID.randomUUID());
        item.setItemType("POTENTIAL_LEAD");
        item.setContent("Analyze CCTV footage along Avinashi Road north towards airport.");
        item.setReasoning("Check getaway route cameras");

        review.setRecommendationItem(item);
        review.setDecision("EDITED");
        review.setReviewedContent("Analyze CCTV along Avinashi Road towards Goldwins junction specifically.");
        review.setReviewComment("Narrowed search grid based on eyewitness statement.");
        review.setCreatedAt(OffsetDateTime.now());

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(officer));
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(crimeCase));
        when(firRepository.findByCaseEntityId(caseId)).thenReturn(Optional.empty());
        when(victimRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId)).thenReturn(Collections.emptyList());
        when(suspectRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId)).thenReturn(Collections.emptyList());
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(aiRecommendationRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(investigatorReviewRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(review));
        when(publicInformationItemRepository.findByCrimeCaseIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(notificationRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        CaseReportSummaryDTO summary = caseReportService.getReportSummary(caseId, "TN-POL-1001");

        assertEquals(1, summary.getInvestigatorReviews().size());
        CaseReportSummaryDTO.InvestigatorReviewSummaryDTO revDTO = summary.getInvestigatorReviews().get(0);
        assertEquals("EDITED", revDTO.getInvestigatorDecision());
        assertEquals("Analyze CCTV footage along Avinashi Road north towards airport.", revDTO.getOriginalAiContent());
        assertEquals("Analyze CCTV along Avinashi Road towards Goldwins junction specifically.", revDTO.getReviewedContent());
    }

    @Test
    public void testGetReportHistory_Success() {
        CaseReportGeneration gen = new CaseReportGeneration(UUID.randomUUID(), crimeCase, officer, "CrimeSphere-Case-Report-v1", "CS-2026-0001-case-report.pdf");
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(officer));
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(crimeCase));
        when(reportGenerationRepository.findByCrimeCase_IdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(gen));

        List<CaseReportGeneration> history = caseReportService.getReportHistory(caseId, "TN-POL-1001");

        assertEquals(1, history.size());
        assertEquals("CS-2026-0001-case-report.pdf", history.get(0).getFileName());
    }
}
