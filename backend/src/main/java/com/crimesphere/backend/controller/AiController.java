package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.ai.AiCaseAnalysisResponse;
import com.crimesphere.backend.dto.ai.AiRecommendationHistoryDto;
import com.crimesphere.backend.dto.ai.AiRetrievalResponse;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.service.AiAnalysisService;
import com.crimesphere.backend.service.AiRetrievalService;
import com.crimesphere.backend.service.AiServiceClient;
import com.crimesphere.backend.service.KnowledgeIndexingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.crimesphere.backend.dto.ai.EvidenceVerificationResponse;
import com.crimesphere.backend.dto.ai.EvidenceVerificationItemDTO;
import com.crimesphere.backend.dto.ai.CaseAnalysisVerificationResponse;
import com.crimesphere.backend.dto.ai.ItemVerificationDto;
import com.crimesphere.backend.dto.ai.InvestigatorReviewResponse;
import com.crimesphere.backend.dto.ai.ApproveRecommendationRequest;
import com.crimesphere.backend.dto.ai.EditRecommendationRequest;
import com.crimesphere.backend.dto.ai.RejectRecommendationRequest;
import com.crimesphere.backend.dto.ai.ReviewSummaryResponse;
import com.crimesphere.backend.service.EvidenceVerificationService;
import com.crimesphere.backend.service.InvestigatorReviewService;

@RestController
@RequestMapping("/api")
public class AiController {

    private final AiServiceClient aiServiceClient;
    private final AiAnalysisService aiAnalysisService;
    private final AiRetrievalService aiRetrievalService;
    private final KnowledgeIndexingService knowledgeIndexingService;
    private final EvidenceVerificationService evidenceVerificationService;
    private final InvestigatorReviewService investigatorReviewService;
    private final CaseRepository caseRepository;

    public AiController(
            AiServiceClient aiServiceClient,
            AiAnalysisService aiAnalysisService,
            AiRetrievalService aiRetrievalService,
            KnowledgeIndexingService knowledgeIndexingService,
            EvidenceVerificationService evidenceVerificationService,
            InvestigatorReviewService investigatorReviewService,
            CaseRepository caseRepository
    ) {
        this.aiServiceClient = aiServiceClient;
        this.aiAnalysisService = aiAnalysisService;
        this.aiRetrievalService = aiRetrievalService;
        this.knowledgeIndexingService = knowledgeIndexingService;
        this.evidenceVerificationService = evidenceVerificationService;
        this.investigatorReviewService = investigatorReviewService;
        this.caseRepository = caseRepository;
    }

    /**
     * Check AI Service health status from Spring Boot backend.
     */
    @GetMapping("/ai/health")
    public ResponseEntity<Map<String, Object>> getAiServiceHealth() {
        Map<String, Object> health = aiServiceClient.checkHealth();
        return ResponseEntity.ok(health);
    }

    /**
     * Dispatch case dossier to FastAPI Groq AI investigation agent.
     */
    @PostMapping("/cases/{caseId}/ai/analyze")
    public ResponseEntity<AiCaseAnalysisResponse> analyzeCaseWithAi(@PathVariable UUID caseId) {
        String policeId = getAuthenticatedPoliceId();
        AiCaseAnalysisResponse response = aiAnalysisService.dispatchCaseToAiService(caseId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Fetch chronological AI analysis history for a case.
     */
    @GetMapping("/cases/{caseId}/ai/history")
    public ResponseEntity<List<AiRecommendationHistoryDto>> getCaseAiHistory(@PathVariable UUID caseId) {
        String policeId = getAuthenticatedPoliceId();
        List<AiRecommendationHistoryDto> history = aiAnalysisService.getCaseAnalysisHistory(caseId, policeId);
        return ResponseEntity.ok(history);
    }

    /**
     * Fetch specific past AI analysis by ID.
     */
    @GetMapping("/cases/{caseId}/ai/analysis/{analysisId}")
    public ResponseEntity<AiCaseAnalysisResponse> getCaseAiAnalysisDetail(
            @PathVariable UUID caseId,
            @PathVariable UUID analysisId
    ) {
        String policeId = getAuthenticatedPoliceId();
        AiCaseAnalysisResponse detail = aiAnalysisService.getAnalysisDetail(caseId, analysisId, policeId);
        return ResponseEntity.ok(detail);
    }

    /**
     * Retrieve semantically related historical cases and evidence for the current investigation.
     */
    @PostMapping("/cases/{caseId}/ai/retrieve")
    public ResponseEntity<AiRetrievalResponse> retrieveSimilarKnowledge(
            @PathVariable UUID caseId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String policeId = getAuthenticatedPoliceId();
        String query = body != null && body.get("query") != null ? body.get("query").toString() : null;
        int topK = body != null && body.get("top_k") != null ? Integer.parseInt(body.get("top_k").toString()) : 5;
        double minSimilarity = body != null && body.get("min_similarity") != null ? Double.parseDouble(body.get("min_similarity").toString()) : 0.70;

        AiRetrievalResponse response = aiRetrievalService.retrieveSimilarKnowledge(caseId, query, topK, minSimilarity, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Index case documents and evidence as knowledge chunks for vector search.
     */
    @PostMapping("/cases/{caseId}/ai/index")
    public ResponseEntity<Map<String, Object>> indexCaseKnowledge(@PathVariable UUID caseId) {
        String policeId = getAuthenticatedPoliceId();
        Map<String, Object> result = knowledgeIndexingService.indexCase(caseId, policeId);
        return ResponseEntity.ok(result);
    }

    /**
     * Toggle whether a case is enabled for historical knowledge retrieval.
     */
    @PatchMapping({"/cases/{caseId}/retrieval", "/cases/{caseId}/retrieval-status"})
    public ResponseEntity<Map<String, Object>> toggleCaseRetrievalStatus(
            @PathVariable UUID caseId,
            @RequestBody Map<String, Object> body
    ) {
        String policeId = getAuthenticatedPoliceId();
        PoliceOfficer officer = aiAnalysisService.getAuthenticatedOfficer(policeId);
        Case parentCase = aiAnalysisService.getAuthorizedCase(caseId, officer);

        boolean enabled = false;
        if (body != null) {
            if (body.get("enabled") != null) {
                enabled = Boolean.parseBoolean(body.get("enabled").toString());
            } else if (body.get("retrieval_enabled") != null) {
                enabled = Boolean.parseBoolean(body.get("retrieval_enabled").toString());
            }
        }

        parentCase.setRetrievalEnabled(enabled);
        caseRepository.save(parentCase);

        return ResponseEntity.ok(Map.of(
                "case_id", caseId,
                "case_number", parentCase.getCaseNumber(),
                "retrieval_enabled", parentCase.getRetrievalEnabled(),
                "enabled", parentCase.getRetrievalEnabled()
        ));
    }

    /**
     * Phase 11: Run deterministic evidence verification for an AI recommendation.
     */
    @PostMapping({"/cases/{caseId}/ai/recommendations/{recommendationId}/verify", "/cases/{caseId}/ai/verify/{recommendationId}"})
    public ResponseEntity<EvidenceVerificationResponse> verifyRecommendation(
            @PathVariable UUID caseId,
            @PathVariable UUID recommendationId
    ) {
        String policeId = getAuthenticatedPoliceId();
        EvidenceVerificationResponse response = evidenceVerificationService.verifyRecommendation(caseId, recommendationId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 11: Get verification history for a case.
     */
    @GetMapping("/cases/{caseId}/ai/verifications")
    public ResponseEntity<List<EvidenceVerificationResponse>> getVerificationHistory(
            @PathVariable UUID caseId
    ) {
        String policeId = getAuthenticatedPoliceId();
        List<EvidenceVerificationResponse> response = evidenceVerificationService.getVerificationHistory(caseId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 11: Get complete verification details by verification ID.
     */
    @GetMapping({"/cases/{caseId}/ai/verifications/{verificationId}", "/cases/{caseId}/ai/verification/{verificationId}"})
    public ResponseEntity<EvidenceVerificationResponse> getVerificationDetails(
            @PathVariable UUID caseId,
            @PathVariable UUID verificationId
    ) {
        String policeId = getAuthenticatedPoliceId();
        EvidenceVerificationResponse response = evidenceVerificationService.getVerificationDetails(caseId, verificationId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 12: Section 8 Contract - Approve AI recommendation.
     */
    @PostMapping("/cases/{caseId}/ai/recommendations/{recommendationId}/approve")
    public ResponseEntity<InvestigatorReviewResponse> approveRecommendationByRecId(
            @PathVariable UUID caseId,
            @PathVariable UUID recommendationId,
            @RequestBody(required = false) ApproveRecommendationRequest request
    ) {
        String policeId = getAuthenticatedPoliceId();
        InvestigatorReviewResponse response = investigatorReviewService.approveRecommendationById(caseId, recommendationId, request, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 12: Section 8 Contract - Edit AI recommendation.
     */
    @PostMapping("/cases/{caseId}/ai/recommendations/{recommendationId}/edit")
    public ResponseEntity<InvestigatorReviewResponse> editRecommendationByRecId(
            @PathVariable UUID caseId,
            @PathVariable UUID recommendationId,
            @RequestBody EditRecommendationRequest request
    ) {
        String policeId = getAuthenticatedPoliceId();
        InvestigatorReviewResponse response = investigatorReviewService.editRecommendationById(caseId, recommendationId, request, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 12: Section 8 Contract - Reject AI recommendation.
     */
    @PostMapping("/cases/{caseId}/ai/recommendations/{recommendationId}/reject")
    public ResponseEntity<InvestigatorReviewResponse> rejectRecommendationByRecId(
            @PathVariable UUID caseId,
            @PathVariable UUID recommendationId,
            @RequestBody RejectRecommendationRequest request
    ) {
        String policeId = getAuthenticatedPoliceId();
        InvestigatorReviewResponse response = investigatorReviewService.rejectRecommendationById(caseId, recommendationId, request, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 12: Section 8 Contract - Get investigator review history for a recommendation.
     */
    @GetMapping("/cases/{caseId}/ai/recommendations/{recommendationId}/reviews")
    public ResponseEntity<List<InvestigatorReviewResponse>> getReviewsForRecommendation(
            @PathVariable UUID caseId,
            @PathVariable UUID recommendationId
    ) {
        String policeId = getAuthenticatedPoliceId();
        List<InvestigatorReviewResponse> response = investigatorReviewService.getReviewsForRecommendationById(caseId, recommendationId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 12: Section 8 Contract - Get recommendation detail with current review status and verification state.
     */
    @GetMapping("/cases/{caseId}/ai/recommendations/{recommendationId}")
    public ResponseEntity<Map<String, Object>> getRecommendationDetail(
            @PathVariable UUID caseId,
            @PathVariable UUID recommendationId
    ) {
        String policeId = getAuthenticatedPoliceId();
        Map<String, Object> response = investigatorReviewService.getRecommendationDetailById(caseId, recommendationId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 12: Approve recommendation item for investigation record.
     */
    @PostMapping("/cases/{caseId}/ai/reviews/{analysisId}/items/{itemId}/approve")
    public ResponseEntity<InvestigatorReviewResponse> approveRecommendation(
            @PathVariable UUID caseId,
            @PathVariable UUID analysisId,
            @PathVariable UUID itemId,
            @RequestBody(required = false) ApproveRecommendationRequest request
    ) {
        String policeId = getAuthenticatedPoliceId();
        InvestigatorReviewResponse response = investigatorReviewService.approveRecommendation(caseId, analysisId, itemId, request, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 12: Edit recommendation item. Original AI content preserved immutably.
     */
    @PostMapping("/cases/{caseId}/ai/reviews/{analysisId}/items/{itemId}/edit")
    public ResponseEntity<InvestigatorReviewResponse> editRecommendation(
            @PathVariable UUID caseId,
            @PathVariable UUID analysisId,
            @PathVariable UUID itemId,
            @RequestBody EditRecommendationRequest request
    ) {
        String policeId = getAuthenticatedPoliceId();
        InvestigatorReviewResponse response = investigatorReviewService.editRecommendation(caseId, analysisId, itemId, request, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 12: Reject recommendation item with required investigator comment.
     */
    @PostMapping("/cases/{caseId}/ai/reviews/{analysisId}/items/{itemId}/reject")
    public ResponseEntity<InvestigatorReviewResponse> rejectRecommendation(
            @PathVariable UUID caseId,
            @PathVariable UUID analysisId,
            @PathVariable UUID itemId,
            @RequestBody RejectRecommendationRequest request
    ) {
        String policeId = getAuthenticatedPoliceId();
        InvestigatorReviewResponse response = investigatorReviewService.rejectRecommendation(caseId, analysisId, itemId, request, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 12: Fetch all review decisions for an AI analysis.
     */
    @GetMapping("/cases/{caseId}/ai/reviews/{analysisId}")
    public ResponseEntity<List<InvestigatorReviewResponse>> getReviewsForAnalysis(
            @PathVariable UUID caseId,
            @PathVariable UUID analysisId
    ) {
        String policeId = getAuthenticatedPoliceId();
        List<InvestigatorReviewResponse> response = investigatorReviewService.getReviewsForAnalysis(caseId, analysisId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 12: Fetch review decision for a single recommendation item.
     */
    @GetMapping("/cases/{caseId}/ai/reviews/{analysisId}/items/{itemId}")
    public ResponseEntity<InvestigatorReviewResponse> getReviewForItem(
            @PathVariable UUID caseId,
            @PathVariable UUID analysisId,
            @PathVariable UUID itemId
    ) {
        String policeId = getAuthenticatedPoliceId();
        InvestigatorReviewResponse response = investigatorReviewService.getReviewForItem(caseId, analysisId, itemId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 12: Fetch review status summary for an AI analysis.
     */
    @GetMapping("/cases/{caseId}/ai/reviews/{analysisId}/summary")
    public ResponseEntity<ReviewSummaryResponse> getReviewSummary(
            @PathVariable UUID caseId,
            @PathVariable UUID analysisId
    ) {
        String policeId = getAuthenticatedPoliceId();
        ReviewSummaryResponse response = investigatorReviewService.getReviewSummary(caseId, analysisId, policeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 12: Fetch items requiring review for a case.
     */
    @GetMapping("/cases/{caseId}/ai/reviews/pending")
    public ResponseEntity<List<InvestigatorReviewResponse>> getPendingReviews(
            @PathVariable UUID caseId
    ) {
        String policeId = getAuthenticatedPoliceId();
        List<InvestigatorReviewResponse> response = investigatorReviewService.getPendingReviews(caseId, policeId);
        return ResponseEntity.ok(response);
    }

    private String getAuthenticatedPoliceId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.authentication.BadCredentialsException("Unauthenticated request");
        }
        return authentication.getName();
    }
}
