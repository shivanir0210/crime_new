package com.crimesphere.backend;

import com.crimesphere.backend.dto.ai.EvidenceVerificationResponse;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import com.crimesphere.backend.service.EvidenceVerificationService;
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
public class EvidenceVerificationServiceTest {

    @Mock private CaseRepository caseRepository;
    @Mock private EvidenceRepository evidenceRepository;
    @Mock private FIRRepository firRepository;
    @Mock private EvidenceGapRepository evidenceGapRepository;
    @Mock private AiRecommendationRepository recommendationRepository;
    @Mock private AiRecommendationItemRepository recommendationItemRepository;
    @Mock private EvidenceVerificationRepository verificationRepository;
    @Mock private EvidenceVerificationItemRepository verificationItemRepository;
    @Mock private PoliceOfficerRepository officerRepository;
    @Mock private AuditLogRepository auditLogRepository;

    private EvidenceVerificationService verificationService;

    private UUID stationId;
    private UUID caseId;
    private UUID otherCaseId;
    private UUID recommendationId;
    private String policeId;

    private PoliceStation station;
    private PoliceOfficer officer;
    private Case parentCase;
    private Case otherCase;
    private AiRecommendation recommendation;

    @BeforeEach
    void setUp() {
        verificationService = new EvidenceVerificationService(
                caseRepository, evidenceRepository, firRepository, evidenceGapRepository,
                recommendationRepository, recommendationItemRepository, verificationRepository,
                verificationItemRepository, officerRepository, auditLogRepository
        );

        stationId = UUID.randomUUID();
        caseId = UUID.randomUUID();
        otherCaseId = UUID.randomUUID();
        recommendationId = UUID.randomUUID();
        policeId = "TN-POL-1001";

        station = new PoliceStation();
        station.setId(stationId);
        station.setStationCode("TN-POL-CBE");

        officer = new PoliceOfficer();
        officer.setId(UUID.randomUUID());
        officer.setPoliceId(policeId);
        officer.setStation(station);
        officer.setFullName("Inspector Murugan");

        parentCase = new Case();
        parentCase.setId(caseId);
        parentCase.setCaseNumber("CS-2026-0100");
        parentCase.setStation(station);

        otherCase = new Case();
        otherCase.setId(otherCaseId);
        otherCase.setCaseNumber("CS-2026-0999");
        otherCase.setStation(station);

        recommendation = new AiRecommendation();
        recommendation.setId(recommendationId);
        recommendation.setCaseEntity(parentCase);

        lenient().when(verificationRepository.save(any(EvidenceVerification.class))).thenAnswer(invocation -> {
            EvidenceVerification v = invocation.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            return v;
        });

        lenient().when(verificationItemRepository.save(any(EvidenceVerificationItem.class))).thenAnswer(invocation -> {
            EvidenceVerificationItem item = invocation.getArgument(0);
            if (item.getId() == null) item.setId(UUID.randomUUID());
            return item;
        });
    }

    @Test
    @DisplayName("1. Valid recommendation with valid supporting evidence -> SUPPORTED")
    void testValidRecommendationWithValidEvidence() {
        when(officerRepository.findByPoliceId(policeId)).thenReturn(Optional.of(officer));
        when(caseRepository.findByIdAndStationId(caseId, stationId)).thenReturn(Optional.of(parentCase));
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        Evidence evidence = new Evidence();
        evidence.setId(UUID.randomUUID());
        evidence.setCaseEntity(parentCase);
        evidence.setEvidenceNumber("EVD-001");
        evidence.setTitle("CCTV Footage");
        evidence.setEvidenceType("CCTV");
        evidence.setStatus("VERIFIED");

        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(evidence));
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        AiRecommendationItem item = new AiRecommendationItem();
        item.setId(UUID.randomUUID());
        item.setItemType("OBSERVATION");
        item.setContent("Analysis of EVD-001 shows vehicle entry at scene.");

        AiRecommendationEvidence ref = new AiRecommendationEvidence();
        ref.setEvidence(evidence);
        item.setEvidenceReferences(List.of(ref));

        when(recommendationItemRepository.findByRecommendationId(recommendationId)).thenReturn(List.of(item));
        when(verificationRepository.save(any(EvidenceVerification.class))).thenAnswer(i -> {
            EvidenceVerification v = i.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            return v;
        });

        EvidenceVerificationResponse response = verificationService.verifyRecommendation(caseId, recommendationId, policeId);

        assertNotNull(response);
        assertEquals("SUPPORTED", response.getSupportLevel());
        assertEquals(1, response.getVerifiedEvidenceCount());
        assertEquals(0, response.getMissingEvidenceCount());
        assertEquals(0, response.getConflictCount());
    }

    @Test
    @DisplayName("2. Recommendation with missing evidence -> PARTIALLY_SUPPORTED / VERIFICATION_REQUIRED")
    void testRecommendationWithMissingEvidence() {
        when(officerRepository.findByPoliceId(policeId)).thenReturn(Optional.of(officer));
        when(caseRepository.findByIdAndStationId(caseId, stationId)).thenReturn(Optional.of(parentCase));
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        AiRecommendationItem item = new AiRecommendationItem();
        item.setId(UUID.randomUUID());
        item.setItemType("OBSERVATION");
        item.setContent("Footage from EVD-999 shows missing suspect.");

        when(recommendationItemRepository.findByRecommendationId(recommendationId)).thenReturn(List.of(item));
        when(verificationRepository.save(any(EvidenceVerification.class))).thenAnswer(i -> {
            EvidenceVerification v = i.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            return v;
        });

        EvidenceVerificationResponse response = verificationService.verifyRecommendation(caseId, recommendationId, policeId);

        assertNotNull(response);
        assertEquals("UNSUPPORTED", response.getSupportLevel());
        assertEquals(1, response.getMissingEvidenceCount());
    }

    @Test
    @DisplayName("3. Recommendation with invalid evidence ID -> UNSUPPORTED / NOT_FOUND")
    void testRecommendationWithInvalidEvidenceId() {
        when(officerRepository.findByPoliceId(policeId)).thenReturn(Optional.of(officer));
        when(caseRepository.findByIdAndStationId(caseId, stationId)).thenReturn(Optional.of(parentCase));
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        UUID invalidUuid = UUID.randomUUID();
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        AiRecommendationItem item = new AiRecommendationItem();
        item.setId(UUID.randomUUID());
        item.setItemType("OBSERVATION");
        item.setContent("Evidence ID " + invalidUuid + " proves suspect presence.");

        when(recommendationItemRepository.findByRecommendationId(recommendationId)).thenReturn(List.of(item));
        when(verificationRepository.save(any(EvidenceVerification.class))).thenAnswer(i -> {
            EvidenceVerification v = i.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            return v;
        });

        EvidenceVerificationResponse response = verificationService.verifyRecommendation(caseId, recommendationId, policeId);

        assertNotNull(response);
        assertEquals("UNSUPPORTED", response.getSupportLevel());
        assertEquals(1, response.getItems().size());
        assertEquals("NOT_FOUND", response.getItems().get(0).getResult());
    }

    @Test
    @DisplayName("4. Evidence belonging to another case -> must never be accepted")
    void testEvidenceBelongingToAnotherCase() {
        when(officerRepository.findByPoliceId(policeId)).thenReturn(Optional.of(officer));
        when(caseRepository.findByIdAndStationId(caseId, stationId)).thenReturn(Optional.of(parentCase));
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        // Case A has no evidence, evidence EVD-001 belongs to Case B
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        Evidence otherCaseEv = new Evidence();
        otherCaseEv.setId(UUID.randomUUID());
        otherCaseEv.setCaseEntity(otherCase);
        otherCaseEv.setEvidenceNumber("EVD-001");

        AiRecommendationItem item = new AiRecommendationItem();
        item.setId(UUID.randomUUID());
        item.setItemType("OBSERVATION");
        item.setContent("Cross-referencing EVD-001.");

        AiRecommendationEvidence ref = new AiRecommendationEvidence();
        ref.setEvidence(otherCaseEv);
        item.setEvidenceReferences(List.of(ref));

        when(recommendationItemRepository.findByRecommendationId(recommendationId)).thenReturn(List.of(item));
        when(verificationRepository.save(any(EvidenceVerification.class))).thenAnswer(i -> {
            EvidenceVerification v = i.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            return v;
        });

        EvidenceVerificationResponse response = verificationService.verifyRecommendation(caseId, recommendationId, policeId);

        assertNotNull(response);
        assertEquals("UNSUPPORTED", response.getSupportLevel());
        assertEquals("NOT_FOUND", response.getItems().get(0).getResult());
    }

    @Test
    @DisplayName("5. Rejected evidence -> must not be treated as supporting evidence")
    void testRejectedEvidenceNotAccepted() {
        when(officerRepository.findByPoliceId(policeId)).thenReturn(Optional.of(officer));
        when(caseRepository.findByIdAndStationId(caseId, stationId)).thenReturn(Optional.of(parentCase));
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        Evidence rejectedEv = new Evidence();
        rejectedEv.setId(UUID.randomUUID());
        rejectedEv.setCaseEntity(parentCase);
        rejectedEv.setEvidenceNumber("EVD-002");
        rejectedEv.setStatus("REJECTED");

        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(rejectedEv));
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        AiRecommendationItem item = new AiRecommendationItem();
        item.setId(UUID.randomUUID());
        item.setItemType("OBSERVATION");
        item.setContent("Analysis of EVD-002 shows fingerprint match.");

        AiRecommendationEvidence ref = new AiRecommendationEvidence();
        ref.setEvidence(rejectedEv);
        item.setEvidenceReferences(List.of(ref));

        when(recommendationItemRepository.findByRecommendationId(recommendationId)).thenReturn(List.of(item));
        when(verificationRepository.save(any(EvidenceVerification.class))).thenAnswer(i -> {
            EvidenceVerification v = i.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            return v;
        });

        EvidenceVerificationResponse response = verificationService.verifyRecommendation(caseId, recommendationId, policeId);

        assertNotNull(response);
        assertEquals("UNSUPPORTED", response.getSupportLevel());
        assertEquals("UNSUPPORTED", response.getItems().get(0).getResult());
    }

    @Test
    @DisplayName("6. Archived evidence -> must not be treated as active supporting evidence")
    void testArchivedEvidenceNotAccepted() {
        when(officerRepository.findByPoliceId(policeId)).thenReturn(Optional.of(officer));
        when(caseRepository.findByIdAndStationId(caseId, stationId)).thenReturn(Optional.of(parentCase));
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        Evidence archivedEv = new Evidence();
        archivedEv.setId(UUID.randomUUID());
        archivedEv.setCaseEntity(parentCase);
        archivedEv.setEvidenceNumber("EVD-003");
        archivedEv.setStatus("ARCHIVED");

        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(archivedEv));
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        AiRecommendationItem item = new AiRecommendationItem();
        item.setId(UUID.randomUUID());
        item.setItemType("OBSERVATION");
        item.setContent("Analysis of EVD-003.");

        AiRecommendationEvidence ref = new AiRecommendationEvidence();
        ref.setEvidence(archivedEv);
        item.setEvidenceReferences(List.of(ref));

        when(recommendationItemRepository.findByRecommendationId(recommendationId)).thenReturn(List.of(item));
        when(verificationRepository.save(any(EvidenceVerification.class))).thenAnswer(i -> {
            EvidenceVerification v = i.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            return v;
        });

        EvidenceVerificationResponse response = verificationService.verifyRecommendation(caseId, recommendationId, policeId);

        assertNotNull(response);
        assertEquals("UNSUPPORTED", response.getSupportLevel());
        assertEquals("UNSUPPORTED", response.getItems().get(0).getResult());
    }

    @Test
    @DisplayName("7. Recommendation with no evidence reference -> VERIFICATION_REQUIRED")
    void testRecommendationWithNoEvidenceReference() {
        when(officerRepository.findByPoliceId(policeId)).thenReturn(Optional.of(officer));
        when(caseRepository.findByIdAndStationId(caseId, stationId)).thenReturn(Optional.of(parentCase));
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        AiRecommendationItem item = new AiRecommendationItem();
        item.setId(UUID.randomUUID());
        item.setItemType("POTENTIAL_LEAD");
        item.setContent("Investigate secondary suspect financial transactions.");

        when(recommendationItemRepository.findByRecommendationId(recommendationId)).thenReturn(List.of(item));
        when(verificationRepository.save(any(EvidenceVerification.class))).thenAnswer(i -> {
            EvidenceVerification v = i.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            return v;
        });

        EvidenceVerificationResponse response = verificationService.verifyRecommendation(caseId, recommendationId, policeId);

        assertNotNull(response);
        assertEquals("VERIFICATION_REQUIRED", response.getSupportLevel());
        assertEquals("VERIFICATION_REQUIRED", response.getItems().get(0).getResult());
    }

    @Test
    @DisplayName("8. Contradictory evidence -> conflict detected")
    void testContradictoryEvidenceConflictDetected() {
        when(officerRepository.findByPoliceId(policeId)).thenReturn(Optional.of(officer));
        when(caseRepository.findByIdAndStationId(caseId, stationId)).thenReturn(Optional.of(parentCase));
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        Evidence ev = new Evidence();
        ev.setId(UUID.randomUUID());
        ev.setCaseEntity(parentCase);
        ev.setEvidenceNumber("EVD-004");
        ev.setStatus("VERIFIED");

        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(List.of(ev));
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        AiRecommendationItem item = new AiRecommendationItem();
        item.setId(UUID.randomUUID());
        item.setItemType("CONTRADICTION");
        item.setContent("Alibi statement conflicts with CCTV evidence EVD-004.");

        AiRecommendationEvidence ref = new AiRecommendationEvidence();
        ref.setEvidence(ev);
        item.setEvidenceReferences(List.of(ref));

        when(recommendationItemRepository.findByRecommendationId(recommendationId)).thenReturn(List.of(item));
        when(verificationRepository.save(any(EvidenceVerification.class))).thenAnswer(i -> {
            EvidenceVerification v = i.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            return v;
        });

        EvidenceVerificationResponse response = verificationService.verifyRecommendation(caseId, recommendationId, policeId);

        assertNotNull(response);
        assertEquals("UNSUPPORTED", response.getSupportLevel());
        assertEquals(1, response.getConflictCount());
        assertEquals("CONFLICT", response.getItems().get(0).getResult());
    }

    @Test
    @DisplayName("9. Station isolation -> officer cannot verify another station's case")
    void testStationIsolationEnforced() {
        when(officerRepository.findByPoliceId(policeId)).thenReturn(Optional.of(officer));
        when(caseRepository.findByIdAndStationId(caseId, stationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                verificationService.verifyRecommendation(caseId, recommendationId, policeId)
        );
    }

    @Test
    @DisplayName("10. Historical RAG result -> must never be treated as current evidence")
    void testHistoricalRagNotTreatedAsCurrentEvidence() {
        when(officerRepository.findByPoliceId(policeId)).thenReturn(Optional.of(officer));
        when(caseRepository.findByIdAndStationId(caseId, stationId)).thenReturn(Optional.of(parentCase));
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        // Current case has no physical evidence in DB
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        // Recommendation mentions historical case pattern but no current case evidence ID
        AiRecommendationItem item = new AiRecommendationItem();
        item.setId(UUID.randomUUID());
        item.setItemType("OBSERVATION");
        item.setContent("Historical similarity pattern from CASE-1021 suggests similar MO.");

        when(recommendationItemRepository.findByRecommendationId(recommendationId)).thenReturn(List.of(item));
        when(verificationRepository.save(any(EvidenceVerification.class))).thenAnswer(i -> {
            EvidenceVerification v = i.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            return v;
        });

        EvidenceVerificationResponse response = verificationService.verifyRecommendation(caseId, recommendationId, policeId);

        assertNotNull(response);
        // Lacking current case evidence reference, it requires verification
        assertEquals("VERIFICATION_REQUIRED", response.getSupportLevel());
        assertEquals(0, response.getVerifiedEvidenceCount());
    }

    @Test
    @DisplayName("11. No raw embedding vectors in API response")
    void testNoRawEmbeddingVectorsInApiResponse() {
        when(officerRepository.findByPoliceId(policeId)).thenReturn(Optional.of(officer));
        when(caseRepository.findByIdAndStationId(caseId, stationId)).thenReturn(Optional.of(parentCase));
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());
        when(evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId)).thenReturn(Collections.emptyList());

        AiRecommendationItem item = new AiRecommendationItem();
        item.setId(UUID.randomUUID());
        item.setItemType("POTENTIAL_LEAD");
        item.setContent("Verify CCTV camera 4.");

        when(recommendationItemRepository.findByRecommendationId(recommendationId)).thenReturn(List.of(item));
        when(verificationRepository.save(any(EvidenceVerification.class))).thenAnswer(i -> {
            EvidenceVerification v = i.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            return v;
        });

        EvidenceVerificationResponse response = verificationService.verifyRecommendation(caseId, recommendationId, policeId);

        assertNotNull(response);
        // Verify response payload fields
        assertNotNull(response.getVerificationId());
        assertEquals("CS-2026-0100", response.getCaseNumber());
        assertEquals("Inspector Murugan", response.getVerifiedBy());
    }
}
