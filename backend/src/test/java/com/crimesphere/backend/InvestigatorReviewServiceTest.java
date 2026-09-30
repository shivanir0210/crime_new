package com.crimesphere.backend;

import com.crimesphere.backend.dto.ai.*;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import com.crimesphere.backend.service.InvestigatorReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvestigatorReviewServiceTest {

    @Mock
    private PoliceOfficerRepository policeOfficerRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private AiRecommendationRepository recommendationRepository;

    @Mock
    private AiRecommendationItemRepository recommendationItemRepository;

    @Mock
    private RecommendationVerificationRepository verificationRepository;

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private InvestigatorReviewRepository reviewRepository;

    @Mock
    private InvestigatorReviewHistoryRepository reviewHistoryRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private InvestigatorReviewService reviewService;

    private PoliceStation cbeStation;
    private PoliceStation egmStation;
    private PoliceOfficer cbeOfficer;
    private Case cbeCase;
    private AiRecommendation cbeRec;
    private AiRecommendationItem cbeItem;
    private Evidence cbeEvidence;

    @BeforeEach
    void setUp() {
        reviewService = new InvestigatorReviewService(
                policeOfficerRepository,
                caseRepository,
                recommendationRepository,
                recommendationItemRepository,
                verificationRepository,
                evidenceRepository,
                reviewRepository,
                reviewHistoryRepository,
                auditLogRepository
        );

        cbeStation = new PoliceStation();
        cbeStation.setId(UUID.randomUUID());
        cbeStation.setStationName("Coimbatore Central");
        cbeStation.setStationCode("TN-CBE-001");

        egmStation = new PoliceStation();
        egmStation.setId(UUID.randomUUID());
        egmStation.setStationName("Egmore Station");
        egmStation.setStationCode("TN-EGM-002");

        cbeOfficer = new PoliceOfficer();
        cbeOfficer.setId(UUID.randomUUID());
        cbeOfficer.setPoliceId("IND-TN-99881");
        cbeOfficer.setFullName("Inspector Vijay");
        cbeOfficer.setRole("OFFICER");
        cbeOfficer.setStation(cbeStation);

        cbeCase = new Case();
        cbeCase.setId(UUID.randomUUID());
        cbeCase.setCaseNumber("CS-CBE-2026-0001");
        cbeCase.setTitle("Jewellery Store Heist");
        cbeCase.setStatus("UNDER_INVESTIGATION");
        cbeCase.setStation(cbeStation);

        cbeRec = new AiRecommendation();
        cbeRec.setId(UUID.randomUUID());
        cbeRec.setCaseEntity(cbeCase);
        cbeRec.setSummary("AI Analysis Summary");

        cbeItem = new AiRecommendationItem();
        cbeItem.setId(UUID.randomUUID());
        cbeItem.setRecommendation(cbeRec);
        cbeItem.setItemType("POTENTIAL_LEAD");
        cbeItem.setContent("Review CCTV footage near cross cut road");

        cbeEvidence = new Evidence();
        cbeEvidence.setId(UUID.randomUUID());
        cbeEvidence.setCaseEntity(cbeCase);
        cbeEvidence.setEvidenceNumber("EVD-CBE-2026-0001");
        cbeEvidence.setTitle("CCTV Footage");
    }

    @Test
    @DisplayName("Should successfully approve recommendation with supported evidence")
    void testApproveRecommendation_Success() {
        when(policeOfficerRepository.findByPoliceId(cbeOfficer.getPoliceId())).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findById(cbeCase.getId())).thenReturn(Optional.of(cbeCase));
        when(recommendationRepository.findByIdAndCaseEntityId(cbeRec.getId(), cbeCase.getId())).thenReturn(Optional.of(cbeRec));
        when(recommendationItemRepository.findById(cbeItem.getId())).thenReturn(Optional.of(cbeItem));

        RecommendationVerification verif = new RecommendationVerification();
        verif.setVerificationStatus("SUPPORTED");
        verif.setSupportLevel("STRONG");
        when(verificationRepository.findFirstByRecommendationItemIdOrderByCreatedAtDesc(cbeItem.getId()))
                .thenReturn(Optional.of(verif));

        when(reviewRepository.findFirstByRecommendationItemIdOrderByCreatedAtDesc(cbeItem.getId()))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(any(InvestigatorReview.class))).thenAnswer(invocation -> {
            InvestigatorReview r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ApproveRecommendationRequest request = new ApproveRecommendationRequest("Confirmed against footage", false);
        InvestigatorReviewResponse response = reviewService.approveRecommendation(
                cbeCase.getId(), cbeRec.getId(), cbeItem.getId(), request, cbeOfficer.getPoliceId()
        );

        assertNotNull(response);
        assertEquals("APPROVED", response.getDecision());
        assertEquals("Review CCTV footage near cross cut road", response.getOriginalContent());
        assertNull(response.getReviewedContent());
        assertEquals("Confirmed against footage", response.getReviewComment());
        assertEquals("SUPPORTED", response.getVerificationStatus());
        assertEquals("STRONG", response.getSupportLevel());
        assertEquals(cbeOfficer.getFullName(), response.getReviewer());

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should reject approval of UNSUPPORTED recommendation without explicit confirmation")
    void testApproveRecommendation_Unsupported_WithoutConfirmation_ThrowsException() {
        when(policeOfficerRepository.findByPoliceId(cbeOfficer.getPoliceId())).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findById(cbeCase.getId())).thenReturn(Optional.of(cbeCase));
        when(recommendationRepository.findByIdAndCaseEntityId(cbeRec.getId(), cbeCase.getId())).thenReturn(Optional.of(cbeRec));
        when(recommendationItemRepository.findById(cbeItem.getId())).thenReturn(Optional.of(cbeItem));

        RecommendationVerification verif = new RecommendationVerification();
        verif.setVerificationStatus("UNSUPPORTED");
        verif.setSupportLevel("NONE");
        when(verificationRepository.findFirstByRecommendationItemIdOrderByCreatedAtDesc(cbeItem.getId()))
                .thenReturn(Optional.of(verif));

        ApproveRecommendationRequest request = new ApproveRecommendationRequest("Trying to approve anyway", false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                reviewService.approveRecommendation(cbeCase.getId(), cbeRec.getId(), cbeItem.getId(), request, cbeOfficer.getPoliceId())
        );
        assertTrue(ex.getMessage().contains("Explicit confirmation is required"));
    }

    @Test
    @DisplayName("Should allow approval of UNSUPPORTED recommendation with explicit confirmation")
    void testApproveRecommendation_Unsupported_WithConfirmation_Success() {
        when(policeOfficerRepository.findByPoliceId(cbeOfficer.getPoliceId())).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findById(cbeCase.getId())).thenReturn(Optional.of(cbeCase));
        when(recommendationRepository.findByIdAndCaseEntityId(cbeRec.getId(), cbeCase.getId())).thenReturn(Optional.of(cbeRec));
        when(recommendationItemRepository.findById(cbeItem.getId())).thenReturn(Optional.of(cbeItem));

        RecommendationVerification verif = new RecommendationVerification();
        verif.setVerificationStatus("UNSUPPORTED");
        verif.setSupportLevel("NONE");
        when(verificationRepository.findFirstByRecommendationItemIdOrderByCreatedAtDesc(cbeItem.getId()))
                .thenReturn(Optional.of(verif));

        when(reviewRepository.findFirstByRecommendationItemIdOrderByCreatedAtDesc(cbeItem.getId()))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(any(InvestigatorReview.class))).thenAnswer(invocation -> {
            InvestigatorReview r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ApproveRecommendationRequest request = new ApproveRecommendationRequest("Investigator confirmed lead manually", true);
        InvestigatorReviewResponse response = reviewService.approveRecommendation(
                cbeCase.getId(), cbeRec.getId(), cbeItem.getId(), request, cbeOfficer.getPoliceId()
        );

        assertNotNull(response);
        assertEquals("APPROVED", response.getDecision());
        assertEquals("UNSUPPORTED", response.getVerificationStatus());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should successfully edit recommendation preserving original AI content")
    void testEditRecommendation_Success() {
        when(policeOfficerRepository.findByPoliceId(cbeOfficer.getPoliceId())).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findById(cbeCase.getId())).thenReturn(Optional.of(cbeCase));
        when(recommendationRepository.findByIdAndCaseEntityId(cbeRec.getId(), cbeCase.getId())).thenReturn(Optional.of(cbeRec));
        when(recommendationItemRepository.findById(cbeItem.getId())).thenReturn(Optional.of(cbeItem));
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(cbeCase.getId()))
                .thenReturn(List.of(cbeEvidence));

        when(reviewRepository.findFirstByRecommendationItemIdOrderByCreatedAtDesc(cbeItem.getId()))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(any(InvestigatorReview.class))).thenAnswer(invocation -> {
            InvestigatorReview r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        EditRecommendationRequest request = new EditRecommendationRequest(
                "Review CCTV specifically between 02:00 AM and 03:30 AM",
                "Narrowed timeframe based on witness statement",
                List.of("EVD-CBE-2026-0001")
        );

        InvestigatorReviewResponse response = reviewService.editRecommendation(
                cbeCase.getId(), cbeRec.getId(), cbeItem.getId(), request, cbeOfficer.getPoliceId()
        );

        assertNotNull(response);
        assertEquals("EDITED", response.getDecision());
        assertEquals("Review CCTV footage near cross cut road", response.getOriginalContent());
        assertEquals("Review CCTV specifically between 02:00 AM and 03:30 AM", response.getReviewedContent());
        assertEquals("Narrowed timeframe based on witness statement", response.getReviewComment());

        // Verify original item content was NOT altered
        assertEquals("Review CCTV footage near cross cut road", cbeItem.getContent());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should reject edit with invalid / cross-case evidence reference")
    void testEditRecommendation_InvalidEvidenceReference_ThrowsException() {
        when(policeOfficerRepository.findByPoliceId(cbeOfficer.getPoliceId())).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findById(cbeCase.getId())).thenReturn(Optional.of(cbeCase));
        when(recommendationRepository.findByIdAndCaseEntityId(cbeRec.getId(), cbeCase.getId())).thenReturn(Optional.of(cbeRec));
        when(recommendationItemRepository.findById(cbeItem.getId())).thenReturn(Optional.of(cbeItem));
        when(evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(cbeCase.getId()))
                .thenReturn(List.of(cbeEvidence)); // only EVD-CBE-2026-0001

        EditRecommendationRequest request = new EditRecommendationRequest(
                "Check cross-station weapon evidence",
                "Attempting to attach wrong station evidence",
                List.of("EVD-EGM-2026-9999")
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                reviewService.editRecommendation(cbeCase.getId(), cbeRec.getId(), cbeItem.getId(), request, cbeOfficer.getPoliceId())
        );
        assertTrue(ex.getMessage().contains("does not belong to case"));
    }

    @Test
    @DisplayName("Should reject editing recommendation with empty content")
    void testEditRecommendation_EmptyContent_ThrowsException() {
        when(policeOfficerRepository.findByPoliceId(cbeOfficer.getPoliceId())).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findById(cbeCase.getId())).thenReturn(Optional.of(cbeCase));
        when(recommendationRepository.findByIdAndCaseEntityId(cbeRec.getId(), cbeCase.getId())).thenReturn(Optional.of(cbeRec));
        when(recommendationItemRepository.findById(cbeItem.getId())).thenReturn(Optional.of(cbeItem));

        EditRecommendationRequest request = new EditRecommendationRequest("   ", "comment", Collections.emptyList());

        assertThrows(IllegalArgumentException.class, () ->
                reviewService.editRecommendation(cbeCase.getId(), cbeRec.getId(), cbeItem.getId(), request, cbeOfficer.getPoliceId())
        );
    }

    @Test
    @DisplayName("Should successfully reject recommendation with comment")
    void testRejectRecommendation_Success() {
        when(policeOfficerRepository.findByPoliceId(cbeOfficer.getPoliceId())).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findById(cbeCase.getId())).thenReturn(Optional.of(cbeCase));
        when(recommendationRepository.findByIdAndCaseEntityId(cbeRec.getId(), cbeCase.getId())).thenReturn(Optional.of(cbeRec));
        when(recommendationItemRepository.findById(cbeItem.getId())).thenReturn(Optional.of(cbeItem));

        when(reviewRepository.findFirstByRecommendationItemIdOrderByCreatedAtDesc(cbeItem.getId()))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(any(InvestigatorReview.class))).thenAnswer(invocation -> {
            InvestigatorReview r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        RejectRecommendationRequest request = new RejectRecommendationRequest("Outside jurisdiction boundary");
        InvestigatorReviewResponse response = reviewService.rejectRecommendation(
                cbeCase.getId(), cbeRec.getId(), cbeItem.getId(), request, cbeOfficer.getPoliceId()
        );

        assertNotNull(response);
        assertEquals("REJECTED", response.getDecision());
        assertEquals("Outside jurisdiction boundary", response.getReviewComment());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should fail when rejecting recommendation without comment")
    void testRejectRecommendation_EmptyComment_ThrowsException() {
        when(policeOfficerRepository.findByPoliceId(cbeOfficer.getPoliceId())).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findById(cbeCase.getId())).thenReturn(Optional.of(cbeCase));
        when(recommendationRepository.findByIdAndCaseEntityId(cbeRec.getId(), cbeCase.getId())).thenReturn(Optional.of(cbeRec));
        when(recommendationItemRepository.findById(cbeItem.getId())).thenReturn(Optional.of(cbeItem));

        RejectRecommendationRequest request = new RejectRecommendationRequest("   ");

        assertThrows(IllegalArgumentException.class, () ->
                reviewService.rejectRecommendation(cbeCase.getId(), cbeRec.getId(), cbeItem.getId(), request, cbeOfficer.getPoliceId())
        );
    }

    @Test
    @DisplayName("Should preserve history when decision changes")
    void testReviewHistoryPreserved_OnSubsequentDecision() {
        when(policeOfficerRepository.findByPoliceId(cbeOfficer.getPoliceId())).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findById(cbeCase.getId())).thenReturn(Optional.of(cbeCase));
        when(recommendationRepository.findByIdAndCaseEntityId(cbeRec.getId(), cbeCase.getId())).thenReturn(Optional.of(cbeRec));
        when(recommendationItemRepository.findById(cbeItem.getId())).thenReturn(Optional.of(cbeItem));

        InvestigatorReview existingReview = new InvestigatorReview();
        existingReview.setId(UUID.randomUUID());
        existingReview.setCaseEntity(cbeCase);
        existingReview.setRecommendationItem(cbeItem);
        existingReview.setDecision("APPROVED");
        existingReview.setOriginalContent("Original finding");
        existingReview.setReviewedContent(null);
        existingReview.setReviewer(cbeOfficer);

        when(reviewRepository.findFirstByRecommendationItemIdOrderByCreatedAtDesc(cbeItem.getId()))
                .thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(InvestigatorReview.class))).thenReturn(existingReview);

        RejectRecommendationRequest request = new RejectRecommendationRequest("Changed mind after forensic report");
        InvestigatorReviewResponse response = reviewService.rejectRecommendation(
                cbeCase.getId(), cbeRec.getId(), cbeItem.getId(), request, cbeOfficer.getPoliceId()
        );

        assertEquals("REJECTED", response.getDecision());
        // Verify history entry was persisted with previous decision APPROVED
        verify(reviewHistoryRepository, times(1)).save(any(InvestigatorReviewHistory.class));
    }

    @Test
    @DisplayName("Should deny cross-station review access")
    void testCrossStationAccess_Denied() {
        PoliceOfficer egmOfficer = new PoliceOfficer();
        egmOfficer.setId(UUID.randomUUID());
        egmOfficer.setPoliceId("IND-TN-11223");
        egmOfficer.setRole("OFFICER");
        egmOfficer.setStation(egmStation);

        when(policeOfficerRepository.findByPoliceId(egmOfficer.getPoliceId())).thenReturn(Optional.of(egmOfficer));
        when(caseRepository.findById(cbeCase.getId())).thenReturn(Optional.of(cbeCase));

        ApproveRecommendationRequest request = new ApproveRecommendationRequest("Cross-station approve attempt", false);

        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.approveRecommendation(cbeCase.getId(), cbeRec.getId(), cbeItem.getId(), request, egmOfficer.getPoliceId())
        );
    }

    @Test
    @DisplayName("Should deny review if item belongs to different analysis")
    void testItemBelongsToDifferentAnalysis_Denied() {
        when(policeOfficerRepository.findByPoliceId(cbeOfficer.getPoliceId())).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findById(cbeCase.getId())).thenReturn(Optional.of(cbeCase));

        AiRecommendation differentRec = new AiRecommendation();
        differentRec.setId(UUID.randomUUID());
        differentRec.setCaseEntity(cbeCase);

        when(recommendationRepository.findByIdAndCaseEntityId(differentRec.getId(), cbeCase.getId()))
                .thenReturn(Optional.of(differentRec));
        when(recommendationItemRepository.findById(cbeItem.getId())).thenReturn(Optional.of(cbeItem));

        ApproveRecommendationRequest request = new ApproveRecommendationRequest("Approve mismatch", false);

        assertThrows(IllegalArgumentException.class, () ->
                reviewService.approveRecommendation(cbeCase.getId(), differentRec.getId(), cbeItem.getId(), request, cbeOfficer.getPoliceId())
        );
    }

    @Test
    @DisplayName("Should compute review summary accurately")
    void testGetReviewSummary() {
        when(policeOfficerRepository.findByPoliceId(cbeOfficer.getPoliceId())).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findById(cbeCase.getId())).thenReturn(Optional.of(cbeCase));
        when(recommendationRepository.findByIdAndCaseEntityId(cbeRec.getId(), cbeCase.getId())).thenReturn(Optional.of(cbeRec));

        AiRecommendationItem item1 = new AiRecommendationItem();
        item1.setId(UUID.randomUUID());
        AiRecommendationItem item2 = new AiRecommendationItem();
        item2.setId(UUID.randomUUID());
        AiRecommendationItem item3 = new AiRecommendationItem();
        item3.setId(UUID.randomUUID());

        when(recommendationItemRepository.findByRecommendationIdOrderByCreatedAtAsc(cbeRec.getId()))
                .thenReturn(List.of(item1, item2, item3));

        InvestigatorReview rev1 = new InvestigatorReview();
        rev1.setDecision("APPROVED");
        when(reviewRepository.findFirstByRecommendationItemIdOrderByCreatedAtDesc(item1.getId()))
                .thenReturn(Optional.of(rev1));

        InvestigatorReview rev2 = new InvestigatorReview();
        rev2.setDecision("EDITED");
        when(reviewRepository.findFirstByRecommendationItemIdOrderByCreatedAtDesc(item2.getId()))
                .thenReturn(Optional.of(rev2));

        // item3 has no review -> pending
        when(reviewRepository.findFirstByRecommendationItemIdOrderByCreatedAtDesc(item3.getId()))
                .thenReturn(Optional.empty());

        ReviewSummaryResponse summary = reviewService.getReviewSummary(cbeCase.getId(), cbeRec.getId(), cbeOfficer.getPoliceId());

        assertNotNull(summary);
        assertEquals(3, summary.getTotal());
        assertEquals(1, summary.getApproved());
        assertEquals(1, summary.getEdited());
        assertEquals(0, summary.getRejected());
        assertEquals(1, summary.getPendingReview());
        assertFalse(summary.isComplete());
        assertTrue(summary.getMessage().contains("1 AI findings require investigator review"));
    }
}
