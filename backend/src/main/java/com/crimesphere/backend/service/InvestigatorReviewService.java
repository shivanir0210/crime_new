package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.ai.*;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.entity.NotificationType;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvestigatorReviewService {

    private static final Logger logger = LoggerFactory.getLogger(InvestigatorReviewService.class);

    private final PoliceOfficerRepository policeOfficerRepository;
    private final CaseRepository caseRepository;
    private final AiRecommendationRepository recommendationRepository;
    private final AiRecommendationItemRepository recommendationItemRepository;
    private final AiRecommendationEvidenceRepository recommendationEvidenceRepository;
    private final RecommendationVerificationRepository verificationRepository;
    private final EvidenceRepository evidenceRepository;
    private final InvestigatorReviewRepository reviewRepository;
    private final InvestigatorReviewHistoryRepository reviewHistoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;

    public InvestigatorReviewService(PoliceOfficerRepository policeOfficerRepository,
                                    CaseRepository caseRepository,
                                    AiRecommendationRepository recommendationRepository,
                                    AiRecommendationItemRepository recommendationItemRepository,
                                    RecommendationVerificationRepository verificationRepository,
                                    EvidenceRepository evidenceRepository,
                                    InvestigatorReviewRepository reviewRepository,
                                    InvestigatorReviewHistoryRepository reviewHistoryRepository,
                                    AuditLogRepository auditLogRepository) {
        this(policeOfficerRepository, caseRepository, recommendationRepository,
             recommendationItemRepository, null, verificationRepository, evidenceRepository,
             reviewRepository, reviewHistoryRepository, auditLogRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public InvestigatorReviewService(PoliceOfficerRepository policeOfficerRepository,
                                    CaseRepository caseRepository,
                                    AiRecommendationRepository recommendationRepository,
                                    AiRecommendationItemRepository recommendationItemRepository,
                                    AiRecommendationEvidenceRepository recommendationEvidenceRepository,
                                    RecommendationVerificationRepository verificationRepository,
                                    EvidenceRepository evidenceRepository,
                                    InvestigatorReviewRepository reviewRepository,
                                    InvestigatorReviewHistoryRepository reviewHistoryRepository,
                                    AuditLogRepository auditLogRepository,
                                    NotificationService notificationService) {
        this.policeOfficerRepository = policeOfficerRepository;
        this.caseRepository = caseRepository;
        this.recommendationRepository = recommendationRepository;
        this.recommendationItemRepository = recommendationItemRepository;
        this.recommendationEvidenceRepository = recommendationEvidenceRepository;
        this.verificationRepository = verificationRepository;
        this.evidenceRepository = evidenceRepository;
        this.reviewRepository = reviewRepository;
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
    }

    /**
     * Approves an AI recommendation item.
     */
    @Transactional
    public InvestigatorReviewResponse approveRecommendation(UUID caseId, UUID analysisId, UUID itemId,
                                                            ApproveRecommendationRequest request, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);
        AiRecommendation recommendation = getAuthorizedRecommendation(analysisId, parentCase);
        AiRecommendationItem item = getAuthorizedRecommendationItem(itemId, recommendation);

        // Load verification snapshot
        Optional<RecommendationVerification> verifOpt = verificationRepository
                .findFirstByRecommendationItemIdOrderByCreatedAtDesc(item.getId());
        String verifStatus = verifOpt.map(RecommendationVerification::getVerificationStatus).orElse("UNVERIFIED");
        String supportLevel = verifOpt.map(RecommendationVerification::getSupportLevel).orElse("NONE");

        // Approval of UNSUPPORTED requires explicit confirmation
        if ("UNSUPPORTED".equalsIgnoreCase(verifStatus)) {
            if (request == null || !Boolean.TRUE.equals(request.getConfirmUnsupported())) {
                throw new IllegalArgumentException("Explicit confirmation is required to approve an UNSUPPORTED recommendation");
            }
        }

        String comment = request != null ? request.getComment() : null;

        Optional<InvestigatorReview> existingReviewOpt = reviewRepository
                .findFirstByRecommendationItemIdOrderByCreatedAtDesc(item.getId());

        InvestigatorReview review;
        if (existingReviewOpt.isPresent()) {
            review = existingReviewOpt.get();
            // Preserve history
            createHistoryEntry(review, review.getDecision(), "APPROVED",
                    review.getReviewedContent() != null ? review.getReviewedContent() : review.getOriginalContent(),
                    null, comment, officer);

            review.setDecision("APPROVED");
            review.setReviewer(officer);
            review.setReviewComment(comment);
            review.setVerificationStatusAtReview(verifStatus);
            review.setSupportLevelAtReview(supportLevel);
            review.setUpdatedAt(OffsetDateTime.now());
        } else {
            review = new InvestigatorReview();
            review.setRecommendationItem(item);
            review.setCaseEntity(parentCase);
            review.setReviewer(officer);
            review.setDecision("APPROVED");
            review.setOriginalContent(item.getContent());
            review.setReviewedContent(null);
            review.setReviewComment(comment);
            review.setVerificationStatusAtReview(verifStatus);
            review.setSupportLevelAtReview(supportLevel);
        }

        InvestigatorReview savedReview = reviewRepository.save(review);
        recommendation.setStatus("APPROVED");
        recommendationRepository.save(recommendation);

        try {
            if (notificationService != null) {
                notificationService.dispatchCaseNotification(
                        parentCase,
                        NotificationType.AI_RECOMMENDATION_APPROVED,
                        "CrimeSphere AI — AI Finding Approved: " + parentCase.getCaseNumber(),
                        String.format("Investigator %s approved AI recommendation item for case %s (Status: %s).",
                                officer.getFullName(), parentCase.getCaseNumber(), verifStatus),
                        "No further action required.",
                        officer
                );
            }
        } catch (Exception e) {
            logger.warn("AI_RECOMMENDATION_APPROVED notification dispatch failed: {}", e.getMessage());
        }

        recordAuditLog(officer, "AI_RECOMMENDATION_APPROVED",
                String.format("Officer %s approved AI recommendation %s for case %s (Verification: %s)",
                        officer.getFullName(), itemId, parentCase.getCaseNumber(), verifStatus));

        return toDto(savedReview, analysisId);
    }

    /**
     * Edits an AI recommendation item. Preserves original AI content immutably.
     */
    @Transactional
    public InvestigatorReviewResponse editRecommendation(UUID caseId, UUID analysisId, UUID itemId,
                                                         EditRecommendationRequest request, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);
        AiRecommendation recommendation = getAuthorizedRecommendation(analysisId, parentCase);
        AiRecommendationItem item = getAuthorizedRecommendationItem(itemId, recommendation);

        if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Edited content cannot be empty");
        }

        // Validate evidence references belong to current case
        if (request.getEvidenceReferences() != null && !request.getEvidenceReferences().isEmpty()) {
            List<Evidence> caseEvidence = evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
            Set<String> validEvNumbers = caseEvidence.stream()
                    .map(e -> e.getEvidenceNumber() != null ? e.getEvidenceNumber().trim().toUpperCase() : "")
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());

            for (String ref : request.getEvidenceReferences()) {
                if (ref != null && !ref.trim().isEmpty()) {
                    if (!validEvNumbers.contains(ref.trim().toUpperCase())) {
                        throw new IllegalArgumentException("Evidence reference '" + ref + "' does not belong to case " + parentCase.getCaseNumber());
                    }
                }
            }
        }

        // Load verification snapshot
        Optional<RecommendationVerification> verifOpt = verificationRepository
                .findFirstByRecommendationItemIdOrderByCreatedAtDesc(item.getId());
        String verifStatus = verifOpt.map(RecommendationVerification::getVerificationStatus).orElse("UNVERIFIED");
        String supportLevel = verifOpt.map(RecommendationVerification::getSupportLevel).orElse("NONE");

        String editedContent = request.getContent().trim();
        String comment = request.getComment();

        Optional<InvestigatorReview> existingReviewOpt = reviewRepository
                .findFirstByRecommendationItemIdOrderByCreatedAtDesc(item.getId());

        InvestigatorReview review;
        if (existingReviewOpt.isPresent()) {
            review = existingReviewOpt.get();
            createHistoryEntry(review, review.getDecision(), "EDITED",
                    review.getReviewedContent() != null ? review.getReviewedContent() : review.getOriginalContent(),
                    editedContent, comment, officer);

            review.setDecision("EDITED");
            review.setReviewer(officer);
            review.setReviewedContent(editedContent);
            review.setReviewComment(comment);
            review.setVerificationStatusAtReview(verifStatus);
            review.setSupportLevelAtReview(supportLevel);
            review.setUpdatedAt(OffsetDateTime.now());
        } else {
            review = new InvestigatorReview();
            review.setRecommendationItem(item);
            review.setCaseEntity(parentCase);
            review.setReviewer(officer);
            review.setDecision("EDITED");
            review.setOriginalContent(item.getContent());
            review.setReviewedContent(editedContent);
            review.setReviewComment(comment);
            review.setVerificationStatusAtReview(verifStatus);
            review.setSupportLevelAtReview(supportLevel);
        }

        InvestigatorReview savedReview = reviewRepository.save(review);
        recommendation.setStatus("EDITED");
        recommendationRepository.save(recommendation);

        try {
            if (notificationService != null) {
                notificationService.dispatchCaseNotification(
                        parentCase,
                        NotificationType.AI_RECOMMENDATION_EDITED,
                        "CrimeSphere AI — AI Finding Edited: " + parentCase.getCaseNumber(),
                        String.format("Investigator %s modified AI recommendation item for case %s.",
                                officer.getFullName(), parentCase.getCaseNumber()),
                        "Review modified finding in case dossier.",
                        officer
                );
            }
        } catch (Exception e) {
            logger.warn("AI_RECOMMENDATION_EDITED notification dispatch failed: {}", e.getMessage());
        }

        recordAuditLog(officer, "AI_RECOMMENDATION_EDITED",
                String.format("Officer %s edited AI recommendation %s for case %s",
                        officer.getFullName(), itemId, parentCase.getCaseNumber()));

        return toDto(savedReview, analysisId);
    }

    /**
     * Rejects an AI recommendation item. Comment is mandatory.
     */
    @Transactional
    public InvestigatorReviewResponse rejectRecommendation(UUID caseId, UUID analysisId, UUID itemId,
                                                           RejectRecommendationRequest request, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);
        AiRecommendation recommendation = getAuthorizedRecommendation(analysisId, parentCase);
        AiRecommendationItem item = getAuthorizedRecommendationItem(itemId, recommendation);

        if (request == null || request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new IllegalArgumentException("Comment is required when rejecting a recommendation");
        }

        String comment = request.getComment().trim();

        // Load verification snapshot
        Optional<RecommendationVerification> verifOpt = verificationRepository
                .findFirstByRecommendationItemIdOrderByCreatedAtDesc(item.getId());
        String verifStatus = verifOpt.map(RecommendationVerification::getVerificationStatus).orElse("UNVERIFIED");
        String supportLevel = verifOpt.map(RecommendationVerification::getSupportLevel).orElse("NONE");

        Optional<InvestigatorReview> existingReviewOpt = reviewRepository
                .findFirstByRecommendationItemIdOrderByCreatedAtDesc(item.getId());

        InvestigatorReview review;
        if (existingReviewOpt.isPresent()) {
            review = existingReviewOpt.get();
            createHistoryEntry(review, review.getDecision(), "REJECTED",
                    review.getReviewedContent() != null ? review.getReviewedContent() : review.getOriginalContent(),
                    null, comment, officer);

            review.setDecision("REJECTED");
            review.setReviewer(officer);
            review.setReviewComment(comment);
            review.setVerificationStatusAtReview(verifStatus);
            review.setSupportLevelAtReview(supportLevel);
            review.setUpdatedAt(OffsetDateTime.now());
        } else {
            review = new InvestigatorReview();
            review.setRecommendationItem(item);
            review.setCaseEntity(parentCase);
            review.setReviewer(officer);
            review.setDecision("REJECTED");
            review.setOriginalContent(item.getContent());
            review.setReviewedContent(null);
            review.setReviewComment(comment);
            review.setVerificationStatusAtReview(verifStatus);
            review.setSupportLevelAtReview(supportLevel);
        }

        InvestigatorReview savedReview = reviewRepository.save(review);
        recommendation.setStatus("REJECTED");
        recommendationRepository.save(recommendation);

        try {
            if (notificationService != null) {
                notificationService.dispatchCaseNotification(
                        parentCase,
                        NotificationType.AI_RECOMMENDATION_REJECTED,
                        "CrimeSphere AI — AI Finding Rejected: " + parentCase.getCaseNumber(),
                        String.format("Investigator %s rejected AI recommendation item for case %s. Reason: %s",
                                officer.getFullName(), parentCase.getCaseNumber(), comment),
                        "Review rejection reason in investigator panel.",
                        officer
                );
            }
        } catch (Exception e) {
            logger.warn("AI_RECOMMENDATION_REJECTED notification dispatch failed: {}", e.getMessage());
        }

        recordAuditLog(officer, "AI_RECOMMENDATION_REJECTED",
                String.format("Officer %s rejected AI recommendation %s for case %s: %s",
                        officer.getFullName(), itemId, parentCase.getCaseNumber(), comment));

        return toDto(savedReview, analysisId);
    }

    /**
     * Gets all reviews for a given analysis.
     */
    @Transactional(readOnly = true)
    public List<InvestigatorReviewResponse> getReviewsForAnalysis(UUID caseId, UUID analysisId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);
        AiRecommendation recommendation = getAuthorizedRecommendation(analysisId, parentCase);

        List<AiRecommendationItem> items = recommendationItemRepository
                .findByRecommendationIdOrderByCreatedAtAsc(recommendation.getId());

        List<InvestigatorReviewResponse> results = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            AiRecommendationItem item = items.get(i);
            Optional<InvestigatorReview> reviewOpt = reviewRepository
                    .findFirstByRecommendationItemIdOrderByCreatedAtDesc(item.getId());
            if (reviewOpt.isPresent()) {
                results.add(toDto(reviewOpt.get(), analysisId, item, i + 1));
            } else {
                results.add(createPendingReviewDto(item, caseId, analysisId, i + 1));
            }
        }

        return results;
    }

    /**
     * Gets the review for a single item.
     */
    @Transactional(readOnly = true)
    public InvestigatorReviewResponse getReviewForItem(UUID caseId, UUID analysisId, UUID itemId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);
        AiRecommendation recommendation = getAuthorizedRecommendation(analysisId, parentCase);
        AiRecommendationItem item = getAuthorizedRecommendationItem(itemId, recommendation);

        return reviewRepository.findFirstByRecommendationItemIdOrderByCreatedAtDesc(item.getId())
                .map(r -> toDto(r, analysisId, item, 1))
                .orElseGet(() -> createPendingReviewDto(item, caseId, analysisId, 1));
    }

    /**
     * Computes the review summary for a given analysis.
     */
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummary(UUID caseId, UUID analysisId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);
        AiRecommendation recommendation = getAuthorizedRecommendation(analysisId, parentCase);

        List<AiRecommendationItem> items = recommendationItemRepository
                .findByRecommendationIdOrderByCreatedAtAsc(recommendation.getId());

        int total = items.size();
        int pendingReview = 0;
        int approved = 0;
        int edited = 0;
        int rejected = 0;

        for (AiRecommendationItem item : items) {
            Optional<InvestigatorReview> reviewOpt = reviewRepository
                    .findFirstByRecommendationItemIdOrderByCreatedAtDesc(item.getId());
            if (reviewOpt.isEmpty()) {
                pendingReview++;
            } else {
                String decision = reviewOpt.get().getDecision();
                if ("APPROVED".equalsIgnoreCase(decision)) {
                    approved++;
                } else if ("EDITED".equalsIgnoreCase(decision)) {
                    edited++;
                } else if ("REJECTED".equalsIgnoreCase(decision)) {
                    rejected++;
                } else {
                    pendingReview++;
                }
            }
        }

        boolean isComplete = total > 0 && pendingReview == 0;
        String message = isComplete
                ? "Investigator review completed."
                : (pendingReview + " AI findings require investigator review.");

        return new ReviewSummaryResponse(total, pendingReview, approved, edited, rejected, isComplete, message);
    }

    /**
     * Finds pending review items for a case.
     */
    @Transactional(readOnly = true)
    public List<InvestigatorReviewResponse> getPendingReviews(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        List<AiRecommendation> recommendations = recommendationRepository
                .findByCaseEntityIdOrderByCreatedAtDesc(parentCase.getId());

        if (recommendations.isEmpty()) {
            return Collections.emptyList();
        }

        // Check latest recommendation
        AiRecommendation latest = recommendations.get(0);
        return getReviewsForAnalysis(caseId, latest.getId(), policeId).stream()
                .filter(r -> "PENDING_REVIEW".equalsIgnoreCase(r.getDecision()))
                .collect(Collectors.toList());
    }

    private void createHistoryEntry(InvestigatorReview review, String prevDecision, String newDecision,
                                    String prevContent, String newContent, String comment, PoliceOfficer officer) {
        try {
            InvestigatorReviewHistory history = new InvestigatorReviewHistory();
            history.setInvestigatorReview(review);
            history.setPreviousDecision(prevDecision);
            history.setNewDecision(newDecision);
            history.setPreviousContent(prevContent);
            history.setNewContent(newContent);
            history.setComment(comment);
            history.setChangedBy(officer);
            reviewHistoryRepository.save(history);
        } catch (Exception e) {
            logger.error("Failed to persist review history for review {}: {}", review.getId(), e.getMessage());
        }
    }

    private PoliceOfficer getAuthenticatedOfficer(String policeId) {
        return policeOfficerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new ResourceNotFoundException("Police officer not found: " + policeId));
    }

    private Case getAuthorizedCase(UUID caseId, PoliceOfficer officer) {
        Case c = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));

        if (!"ADMIN".equalsIgnoreCase(officer.getRole())) {
            if (officer.getStation() == null || c.getStation() == null
                    || !officer.getStation().getId().equals(c.getStation().getId())) {
                throw new ResourceNotFoundException("Case not found or unauthorized for officer station");
            }
        }
        return c;
    }

    private AiRecommendation getAuthorizedRecommendation(UUID analysisId, Case parentCase) {
        return recommendationRepository.findByIdAndCaseEntityId(analysisId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("AI Analysis record not found for this case"));
    }

    private AiRecommendationItem getAuthorizedRecommendationItem(UUID itemId, AiRecommendation recommendation) {
        AiRecommendationItem item = recommendationItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation item not found: " + itemId));

        if (!item.getRecommendation().getId().equals(recommendation.getId())) {
            throw new IllegalArgumentException("Recommendation item does not belong to specified analysis");
        }
        return item;
    }

    private void recordAuditLog(PoliceOfficer officer, String action, String description) {
        try {
            AuditLog log = new AuditLog();
            log.setPoliceOfficer(officer);
            log.setAction(action);
            log.setStatus("SUCCESS");
            log.setUserAgent(description);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to write audit log for action: {}", action, e);
        }
    }

    private List<String> extractEvidenceReferences(AiRecommendationItem item) {
        if (item == null) return Collections.emptyList();
        Set<String> refs = new LinkedHashSet<>();
        if (recommendationEvidenceRepository != null && item.getId() != null) {
            try {
                List<AiRecommendationEvidence> links = recommendationEvidenceRepository.findByRecommendationItemId(item.getId());
                if (links != null) {
                    for (AiRecommendationEvidence l : links) {
                        if (l.getEvidence() != null && l.getEvidence().getEvidenceNumber() != null) {
                            refs.add(l.getEvidence().getEvidenceNumber().trim());
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not query recommendation evidence references: {}", e.getMessage());
            }
        }
        String text = (item.getContent() != null ? item.getContent() : "") + " " + (item.getReasoning() != null ? item.getReasoning() : "");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("EVD-[A-Za-z0-9-]+").matcher(text);
        while (m.find()) {
            refs.add(m.group());
        }
        return new ArrayList<>(refs);
    }

    public InvestigatorReviewResponse toDto(InvestigatorReview r, UUID analysisId) {
        return toDto(r, analysisId, r.getRecommendationItem(), 1);
    }

    public InvestigatorReviewResponse toDto(InvestigatorReview r, UUID analysisId, AiRecommendationItem item, int index) {
        AiRecommendationItem targetItem = item != null ? item : r.getRecommendationItem();
        List<String> evRefs = extractEvidenceReferences(targetItem);
        return new InvestigatorReviewResponse(
                r.getId(),
                r.getCaseEntity().getId(),
                analysisId,
                targetItem != null ? targetItem.getId() : r.getRecommendationItem().getId(),
                r.getDecision(),
                r.getOriginalContent(),
                r.getReviewedContent(),
                r.getReviewComment(),
                r.getVerificationStatusAtReview(),
                r.getSupportLevelAtReview(),
                r.getReviewer() != null ? r.getReviewer().getFullName() : null,
                r.getCreatedAt(),
                r.getUpdatedAt(),
                targetItem != null ? targetItem.getItemType() : null,
                targetItem != null ? targetItem.getReasoning() : null,
                targetItem != null ? targetItem.getPriority() : null,
                evRefs,
                index
        );
    }

    private InvestigatorReviewResponse createPendingReviewDto(AiRecommendationItem item, UUID caseId, UUID analysisId, int index) {
        Optional<RecommendationVerification> verifOpt = verificationRepository
                .findFirstByRecommendationItemIdOrderByCreatedAtDesc(item.getId());
        String verifStatus = verifOpt.map(RecommendationVerification::getVerificationStatus).orElse("UNVERIFIED");
        String supportLevel = verifOpt.map(RecommendationVerification::getSupportLevel).orElse("NONE");
        List<String> evRefs = extractEvidenceReferences(item);

        return new InvestigatorReviewResponse(
                null,
                caseId,
                analysisId,
                item.getId(),
                "PENDING_REVIEW",
                item.getContent(),
                null,
                null,
                verifStatus,
                supportLevel,
                null,
                item.getCreatedAt(),
                null,
                item.getItemType(),
                item.getReasoning(),
                item.getPriority(),
                evRefs,
                index
        );
    }

    /**
     * Approve recommendation by recommendation ID or item ID.
     */
    @Transactional
    public InvestigatorReviewResponse approveRecommendationById(UUID caseId, UUID recommendationId,
                                                               ApproveRecommendationRequest request, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Optional<AiRecommendation> recOpt = recommendationRepository.findByIdAndCaseEntityId(recommendationId, parentCase.getId());
        if (recOpt.isPresent()) {
            AiRecommendation recommendation = recOpt.get();
            List<AiRecommendationItem> items = recommendationItemRepository.findByRecommendationIdOrderByCreatedAtAsc(recommendation.getId());
            if (items.isEmpty()) {
                throw new ResourceNotFoundException("No recommendation items found for analysis " + recommendationId);
            }
            return approveRecommendation(caseId, recommendation.getId(), items.get(0).getId(), request, policeId);
        }

        Optional<AiRecommendationItem> itemOpt = recommendationItemRepository.findById(recommendationId);
        if (itemOpt.isPresent()) {
            AiRecommendationItem item = itemOpt.get();
            return approveRecommendation(caseId, item.getRecommendation().getId(), item.getId(), request, policeId);
        }

        throw new ResourceNotFoundException("Recommendation or item not found: " + recommendationId);
    }

    /**
     * Edit recommendation by recommendation ID or item ID.
     */
    @Transactional
    public InvestigatorReviewResponse editRecommendationById(UUID caseId, UUID recommendationId,
                                                            EditRecommendationRequest request, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Optional<AiRecommendation> recOpt = recommendationRepository.findByIdAndCaseEntityId(recommendationId, parentCase.getId());
        if (recOpt.isPresent()) {
            AiRecommendation recommendation = recOpt.get();
            List<AiRecommendationItem> items = recommendationItemRepository.findByRecommendationIdOrderByCreatedAtAsc(recommendation.getId());
            if (items.isEmpty()) {
                throw new ResourceNotFoundException("No recommendation items found for analysis " + recommendationId);
            }
            return editRecommendation(caseId, recommendation.getId(), items.get(0).getId(), request, policeId);
        }

        Optional<AiRecommendationItem> itemOpt = recommendationItemRepository.findById(recommendationId);
        if (itemOpt.isPresent()) {
            AiRecommendationItem item = itemOpt.get();
            return editRecommendation(caseId, item.getRecommendation().getId(), item.getId(), request, policeId);
        }

        throw new ResourceNotFoundException("Recommendation or item not found: " + recommendationId);
    }

    /**
     * Reject recommendation by recommendation ID or item ID.
     */
    @Transactional
    public InvestigatorReviewResponse rejectRecommendationById(UUID caseId, UUID recommendationId,
                                                             RejectRecommendationRequest request, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Optional<AiRecommendation> recOpt = recommendationRepository.findByIdAndCaseEntityId(recommendationId, parentCase.getId());
        if (recOpt.isPresent()) {
            AiRecommendation recommendation = recOpt.get();
            List<AiRecommendationItem> items = recommendationItemRepository.findByRecommendationIdOrderByCreatedAtAsc(recommendation.getId());
            if (items.isEmpty()) {
                throw new ResourceNotFoundException("No recommendation items found for analysis " + recommendationId);
            }
            return rejectRecommendation(caseId, recommendation.getId(), items.get(0).getId(), request, policeId);
        }

        Optional<AiRecommendationItem> itemOpt = recommendationItemRepository.findById(recommendationId);
        if (itemOpt.isPresent()) {
            AiRecommendationItem item = itemOpt.get();
            return rejectRecommendation(caseId, item.getRecommendation().getId(), item.getId(), request, policeId);
        }

        throw new ResourceNotFoundException("Recommendation or item not found: " + recommendationId);
    }

    /**
     * Get review history for recommendation by recommendation ID or item ID.
     */
    @Transactional(readOnly = true)
    public List<InvestigatorReviewResponse> getReviewsForRecommendationById(UUID caseId, UUID recommendationId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Optional<AiRecommendation> recOpt = recommendationRepository.findByIdAndCaseEntityId(recommendationId, parentCase.getId());
        if (recOpt.isPresent()) {
            return getReviewsForAnalysis(caseId, recommendationId, policeId);
        }

        Optional<AiRecommendationItem> itemOpt = recommendationItemRepository.findById(recommendationId);
        if (itemOpt.isPresent()) {
            AiRecommendationItem item = itemOpt.get();
            return List.of(getReviewForItem(caseId, item.getRecommendation().getId(), item.getId(), policeId));
        }

        throw new ResourceNotFoundException("Recommendation or item not found: " + recommendationId);
    }

    /**
     * Get recommendation details by ID.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRecommendationDetailById(UUID caseId, UUID recommendationId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        AiRecommendation recommendation = getAuthorizedRecommendation(recommendationId, parentCase);
        List<InvestigatorReviewResponse> reviews = getReviewsForAnalysis(caseId, recommendationId, policeId);

        List<RecommendationVerification> verifs = verificationRepository.findByAiRecommendationId(recommendation.getId());
        Optional<RecommendationVerification> verifOpt = verifs.stream().findFirst();

        Map<String, Object> detail = new HashMap<>();
        detail.put("recommendationId", recommendation.getId());
        detail.put("caseId", caseId);
        detail.put("status", recommendation.getStatus());
        detail.put("summary", recommendation.getSummary());
        detail.put("verificationStatus", verifOpt.map(RecommendationVerification::getVerificationStatus).orElse("UNVERIFIED"));
        detail.put("supportLevel", verifOpt.map(RecommendationVerification::getSupportLevel).orElse("NONE"));
        detail.put("reviews", reviews);
        detail.put("createdAt", recommendation.getCreatedAt());

        return detail;
    }
}
