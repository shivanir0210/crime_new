package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.ai.*;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EvidenceVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EvidenceVerificationService.class);

    private static final Pattern EVIDENCE_NUM_PATTERN = Pattern.compile("EVD-[A-Za-z0-9-]+");
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private final CaseRepository caseRepository;
    private final EvidenceRepository evidenceRepository;
    private final FIRRepository firRepository;
    private final EvidenceGapRepository evidenceGapRepository;
    private final AiRecommendationRepository recommendationRepository;
    private final AiRecommendationItemRepository recommendationItemRepository;
    private final EvidenceVerificationRepository verificationRepository;
    private final EvidenceVerificationItemRepository verificationItemRepository;
    private final PoliceOfficerRepository officerRepository;
    private final AuditLogRepository auditLogRepository;
    private final RecommendationVerificationRepository recommendationVerificationRepository;
    private final RecommendationVerificationEvidenceRepository recommendationVerificationEvidenceRepository;

    public EvidenceVerificationService(
            CaseRepository caseRepository,
            EvidenceRepository evidenceRepository,
            FIRRepository firRepository,
            EvidenceGapRepository evidenceGapRepository,
            AiRecommendationRepository recommendationRepository,
            AiRecommendationItemRepository recommendationItemRepository,
            EvidenceVerificationRepository verificationRepository,
            EvidenceVerificationItemRepository verificationItemRepository,
            PoliceOfficerRepository officerRepository,
            AuditLogRepository auditLogRepository
    ) {
        this(caseRepository, evidenceRepository, firRepository, evidenceGapRepository,
             recommendationRepository, recommendationItemRepository, verificationRepository,
             verificationItemRepository, officerRepository, auditLogRepository, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EvidenceVerificationService(
            CaseRepository caseRepository,
            EvidenceRepository evidenceRepository,
            FIRRepository firRepository,
            EvidenceGapRepository evidenceGapRepository,
            AiRecommendationRepository recommendationRepository,
            AiRecommendationItemRepository recommendationItemRepository,
            EvidenceVerificationRepository verificationRepository,
            EvidenceVerificationItemRepository verificationItemRepository,
            PoliceOfficerRepository officerRepository,
            AuditLogRepository auditLogRepository,
            RecommendationVerificationRepository recommendationVerificationRepository,
            RecommendationVerificationEvidenceRepository recommendationVerificationEvidenceRepository
    ) {
        this.caseRepository = caseRepository;
        this.evidenceRepository = evidenceRepository;
        this.firRepository = firRepository;
        this.evidenceGapRepository = evidenceGapRepository;
        this.recommendationRepository = recommendationRepository;
        this.recommendationItemRepository = recommendationItemRepository;
        this.verificationRepository = verificationRepository;
        this.verificationItemRepository = verificationItemRepository;
        this.officerRepository = officerRepository;
        this.auditLogRepository = auditLogRepository;
        this.recommendationVerificationRepository = recommendationVerificationRepository;
        this.recommendationVerificationEvidenceRepository = recommendationVerificationEvidenceRepository;
    }

    /**
     * Deterministic evidence verification for an AI recommendation.
     */
    @Transactional
    public EvidenceVerificationResponse verifyRecommendation(UUID caseId, UUID recommendationId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);
        AiRecommendation recommendation = getAuthorizedRecommendation(recommendationId, parentCase);

        recordAuditLog(officer, "AI_RECOMMENDATION_VERIFICATION_REQUESTED",
                String.format("Officer requested verification for AI recommendation %s on case %s",
                        recommendationId, parentCase.getCaseNumber()));

        try {
            // Load current case evidence and facts
            List<Evidence> caseEvidenceList = evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
            Map<String, Evidence> evidenceByNumber = new HashMap<>();
            Map<UUID, Evidence> evidenceById = new HashMap<>();
            for (Evidence ev : caseEvidenceList) {
                if (ev.getEvidenceNumber() != null) {
                    evidenceByNumber.put(ev.getEvidenceNumber().trim().toUpperCase(), ev);
                }
                if (ev.getId() != null) {
                    evidenceById.put(ev.getId(), ev);
                }
            }

            List<EvidenceGap> gaps = evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);

            // Load recommendation items
            List<AiRecommendationItem> recItems = recommendationItemRepository.findByRecommendationId(recommendationId);

            int verifiedCount = 0;
            int missingCount = 0;
            int conflictCount = 0;

            List<ItemEvaluation> evaluations = new ArrayList<>();
            List<EvidenceVerificationItem> itemEntities = new ArrayList<>();

            for (AiRecommendationItem recItem : recItems) {
                ItemEvaluation eval = evaluateItem(recItem, parentCase, caseEvidenceList, evidenceByNumber, evidenceById, gaps);
                evaluations.add(eval);

                if ("SUPPORTED".equals(eval.result) || "PARTIALLY_SUPPORTED".equals(eval.result)) {
                    if (eval.matchedEvidence != null) verifiedCount++;
                } else if ("NOT_FOUND".equals(eval.result) || "UNSUPPORTED".equals(eval.result)) {
                    missingCount++;
                } else if ("CONFLICT".equals(eval.result)) {
                    conflictCount++;
                }
            }

            // Temp DTO list for computing overall level
            List<EvidenceVerificationItemDTO> tempDtos = evaluations.stream().map(e -> {
                EvidenceVerificationItemDTO d = new EvidenceVerificationItemDTO();
                d.setResult(e.result);
                return d;
            }).collect(Collectors.toList());

            String overallSupportLevel = computeOverallSupportLevel(tempDtos, missingCount, conflictCount);
            String summary = buildVerificationSummary(overallSupportLevel, recItems.size(), verifiedCount, missingCount, conflictCount);

            EvidenceVerification verification = new EvidenceVerification();
            verification.setCaseEntity(parentCase);
            verification.setAiRecommendation(recommendation);
            verification.setVerificationStatus("COMPLETED");
            verification.setSupportLevel(overallSupportLevel);
            verification.setVerificationSummary(summary);
            verification.setVerifiedEvidenceCount(verifiedCount);
            verification.setMissingEvidenceCount(missingCount);
            verification.setConflictCount(conflictCount);
            verification.setVerifiedBy(officer);
            verification.setVerifiedAt(OffsetDateTime.now());

            EvidenceVerification savedVerification = verificationRepository.save(verification);

            List<EvidenceVerificationItemDTO> itemDtos = new ArrayList<>();

            for (int i = 0; i < recItems.size(); i++) {
                AiRecommendationItem recItem = recItems.get(i);
                ItemEvaluation eval = evaluations.get(i);

                String normalizedStatus = switch (eval.result != null ? eval.result.toUpperCase() : "") {
                    case "SUPPORTED", "VERIFIED" -> "VERIFIED";
                    case "PARTIALLY_SUPPORTED", "PARTIALLY_VERIFIED" -> "PARTIALLY_VERIFIED";
                    case "CONFLICT", "CONTRADICTED" -> "CONTRADICTED";
                    case "UNSUPPORTED", "NOT_FOUND", "REFERENCE_INVALID", "UNVERIFIED" -> "UNVERIFIED";
                    default -> "NEEDS_REVIEW";
                };

                String itemSupportLevel = switch (normalizedStatus) {
                    case "VERIFIED" -> "STRONG";
                    case "PARTIALLY_VERIFIED" -> "MODERATE";
                    case "CONTRADICTED" -> "CONFLICT";
                    case "UNVERIFIED" -> "NONE";
                    default -> "WEAK";
                };

                // Persist/Update RecommendationVerification for item
                if (recommendationVerificationRepository != null) {
                    Optional<RecommendationVerification> existingVerifOpt = recommendationVerificationRepository
                            .findFirstByRecommendationItemIdOrderByCreatedAtDesc(recItem.getId());
                    RecommendationVerification rv = existingVerifOpt.orElseGet(RecommendationVerification::new);
                    rv.setRecommendationItem(recItem);
                    rv.setCaseEntity(parentCase);
                    rv.setVerificationStatus(normalizedStatus);
                    rv.setSupportLevel(itemSupportLevel);
                    rv.setVerificationSummary(eval.reason != null ? eval.reason : "Verified against case database evidence.");
                    rv.setVerifiedBy(officer);
                    rv.setVerifiedAt(OffsetDateTime.now());
                    rv.setVerificationMethod("RULE_BASED");
                    RecommendationVerification savedRv = recommendationVerificationRepository.save(rv);

                    if (eval.matchedEvidence != null && recommendationVerificationEvidenceRepository != null) {
                        List<RecommendationVerificationEvidence> existingEvList = recommendationVerificationEvidenceRepository.findByVerificationId(savedRv.getId());
                        boolean alreadyLinked = existingEvList != null && existingEvList.stream().anyMatch(e -> e.getEvidence() != null && e.getEvidence().getId().equals(eval.matchedEvidence.getId()));
                        if (!alreadyLinked) {
                            RecommendationVerificationEvidence rve = new RecommendationVerificationEvidence();
                            rve.setVerification(savedRv);
                            rve.setEvidence(eval.matchedEvidence);
                            rve.setRelationship(normalizedStatus.equals("VERIFIED") || normalizedStatus.equals("PARTIALLY_VERIFIED") ? "SUPPORTS" : "CONTRADICTS");
                            rve.setSupportNote(eval.reason);
                            recommendationVerificationEvidenceRepository.save(rve);
                        }
                    }
                }

                EvidenceVerificationItem itemEntity = new EvidenceVerificationItem();
                itemEntity.setVerification(savedVerification);
                itemEntity.setEvidence(eval.matchedEvidence);
                itemEntity.setItemType(eval.itemType);
                itemEntity.setClaim(recItem.getContent() != null ? recItem.getContent() : "");
                itemEntity.setSupportingContent(eval.supportingContent);
                itemEntity.setResult(eval.result);
                itemEntity.setReason(eval.reason);

                EvidenceVerificationItem savedItem = verificationItemRepository.save(itemEntity);

                itemDtos.add(new EvidenceVerificationItemDTO(
                        savedItem.getId(),
                        savedVerification.getId(),
                        eval.matchedEvidence != null ? eval.matchedEvidence.getId() : null,
                        eval.matchedEvidence != null ? eval.matchedEvidence.getEvidenceNumber() : null,
                        eval.matchedEvidence != null ? eval.matchedEvidence.getTitle() : null,
                        eval.itemType,
                        savedItem.getClaim(),
                        eval.supportingContent,
                        eval.result,
                        eval.reason,
                        savedItem.getCreatedAt()
                ));
            }

            recordAuditLog(officer, "AI_RECOMMENDATION_VERIFICATION_COMPLETED",
                    String.format("Verification completed for case %s recommendation %s: %s (Verified: %d, Missing: %d, Conflict: %d)",
                            parentCase.getCaseNumber(), recommendationId, overallSupportLevel, verifiedCount, missingCount, conflictCount));

            return new EvidenceVerificationResponse(
                    savedVerification.getId(),
                    caseId,
                    parentCase.getCaseNumber(),
                    recommendationId,
                    null,
                    savedVerification.getVerificationStatus(),
                    overallSupportLevel,
                    summary,
                    verifiedCount,
                    missingCount,
                    conflictCount,
                    officer.getFullName(),
                    savedVerification.getVerifiedAt(),
                    savedVerification.getCreatedAt(),
                    itemDtos
            );

        } catch (Exception e) {
            recordAuditLog(officer, "AI_RECOMMENDATION_VERIFICATION_FAILED",
                    String.format("AI verification failed for case %s: %s", parentCase.getCaseNumber(), e.getMessage()));
            throw e;
        }
    }

    /**
     * Retrieves verification history for a case.
     */
    @Transactional(readOnly = true)
    public List<EvidenceVerificationResponse> getVerificationHistory(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        List<EvidenceVerification> verifications = verificationRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);

        return verifications.stream().map(v -> mapToResponseDto(v, parentCase)).collect(Collectors.toList());
    }

    /**
     * Retrieves details for a specific verification ID.
     */
    @Transactional(readOnly = true)
    public EvidenceVerificationResponse getVerificationDetails(UUID caseId, UUID verificationId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        EvidenceVerification verification = verificationRepository.findByIdAndCaseEntityId(verificationId, caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification record not found: " + verificationId));

        return mapToResponseDto(verification, parentCase);
    }

    /**
     * Evaluates a single recommendation item against actual database evidence.
     */
    private ItemEvaluation evaluateItem(
            AiRecommendationItem recItem,
            Case parentCase,
            List<Evidence> allCaseEvidence,
            Map<String, Evidence> evidenceByNumber,
            Map<UUID, Evidence> evidenceById,
            List<EvidenceGap> gaps
    ) {
        String content = recItem.getContent() != null ? recItem.getContent() : "";
        String reasoning = recItem.getReasoning() != null ? recItem.getReasoning() : "";
        String itemType = recItem.getItemType() != null ? recItem.getItemType().toUpperCase() : "CLAIM";
        String fullText = (content + " " + reasoning).trim();

        // 1. Check for referenced evidence IDs / numbers
        Set<String> citedNumbers = extractEvidenceNumbers(content, reasoning);
        List<UUID> citedUuids = extractUuids(content, reasoning);

        // Also check explicit evidence references attached to recItem if available
        if (recItem.getEvidenceReferences() != null) {
            for (AiRecommendationEvidence ref : recItem.getEvidenceReferences()) {
                if (ref.getEvidence() != null) {
                    if (ref.getEvidence().getId() != null) citedUuids.add(ref.getEvidence().getId());
                    if (ref.getEvidence().getEvidenceNumber() != null) citedNumbers.add(ref.getEvidence().getEvidenceNumber().toUpperCase());
                }
            }
        }

        // 2. Validate Cited References
        Evidence matchedEvidence = null;
        boolean referenceInvalid = false;
        String invalidReason = null;

        for (UUID uuid : citedUuids) {
            Evidence ev = evidenceById.get(uuid);
            if (ev != null) {
                matchedEvidence = ev;
                break;
            } else {
                referenceInvalid = true;
                invalidReason = "Referenced evidence ID [" + uuid + "] does not exist in current case database.";
            }
        }

        if (matchedEvidence == null && !citedNumbers.isEmpty()) {
            for (String evNum : citedNumbers) {
                Evidence ev = evidenceByNumber.get(evNum.toUpperCase());
                if (ev != null) {
                    matchedEvidence = ev;
                    break;
                } else {
                    referenceInvalid = true;
                    invalidReason = "Referenced evidence number [" + evNum + "] does not exist in current case database.";
                }
            }
        }

        if (referenceInvalid && matchedEvidence == null) {
            return new ItemEvaluation("EVIDENCE_REFERENCE", null, "NOT_FOUND", invalidReason, null);
        }

        // Check if matched evidence belongs to another case
        if (matchedEvidence != null && !matchedEvidence.getCaseEntity().getId().equals(parentCase.getId())) {
            return new ItemEvaluation("EVIDENCE_REFERENCE", null, "NOT_FOUND",
                    "Security/Validation Failure: Referenced evidence belongs to a different case.", null);
        }

        // Check matched evidence status (REJECTED or ARCHIVED)
        if (matchedEvidence != null) {
            String evStatus = matchedEvidence.getStatus() != null ? matchedEvidence.getStatus().toUpperCase() : "";
            if ("REJECTED".equals(evStatus) || "ARCHIVED".equals(evStatus)) {
                return new ItemEvaluation(
                        "EVIDENCE_REFERENCE",
                        matchedEvidence,
                        "UNSUPPORTED",
                        "Referenced evidence " + matchedEvidence.getEvidenceNumber() + " has status '" + evStatus + "' and cannot provide active support.",
                        matchedEvidence.getTitle() + " (" + matchedEvidence.getEvidenceType() + ")"
                );
            }
        }

        // 3. Handle Item Types
        switch (itemType) {
            case "OBSERVATION":
                if (matchedEvidence != null) {
                    String evStatus = matchedEvidence.getStatus() != null ? matchedEvidence.getStatus().toUpperCase() : "";
                    String suppContent = matchedEvidence.getEvidenceNumber() + ": " + matchedEvidence.getTitle() + " (" + matchedEvidence.getEvidenceType() + ")";
                    if ("VERIFIED".equals(evStatus)) {
                        return new ItemEvaluation("CLAIM", matchedEvidence, "SUPPORTED",
                                "Observation directly supported by verified evidence " + matchedEvidence.getEvidenceNumber(), suppContent);
                    } else {
                        return new ItemEvaluation("CLAIM", matchedEvidence, "PARTIALLY_SUPPORTED",
                                "Observation supported by evidence " + matchedEvidence.getEvidenceNumber() + " (status: " + evStatus + ")", suppContent);
                    }
                } else {
                    // Check if observation keywords match any active case evidence
                    Evidence keywordMatch = findMatchingEvidenceByKeyword(fullText, allCaseEvidence);
                    if (keywordMatch != null) {
                        String suppContent = keywordMatch.getEvidenceNumber() + ": " + keywordMatch.getTitle();
                        return new ItemEvaluation("CASE_FACT", keywordMatch, "PARTIALLY_SUPPORTED",
                                "Observation corresponds to stored case evidence " + keywordMatch.getEvidenceNumber(), suppContent);
                    }
                    return new ItemEvaluation("CLAIM", null, "VERIFICATION_REQUIRED",
                            "Observation lacks direct evidence citation in current case records.", null);
                }

            case "POTENTIAL_LEAD":
                if (matchedEvidence != null) {
                    String suppContent = matchedEvidence.getEvidenceNumber() + ": " + matchedEvidence.getTitle();
                    return new ItemEvaluation("CLAIM", matchedEvidence, "PARTIALLY_SUPPORTED",
                            "Potential lead grounded in cataloged evidence " + matchedEvidence.getEvidenceNumber(), suppContent);
                }
                return new ItemEvaluation("CLAIM", null, "VERIFICATION_REQUIRED",
                        "Investigative lead requires additional verification before reliance.", null);

            case "EVIDENCE_GAP":
                // Check if claimed missing evidence aligns with actual database evidence gaps
                boolean gapMatches = gaps.stream().anyMatch(g ->
                        (g.getTitle() != null && fullText.toLowerCase().contains(g.getTitle().toLowerCase())) ||
                        (g.getCategory() != null && fullText.toLowerCase().contains(g.getCategory().toLowerCase())) ||
                        (g.getDescription() != null && fullText.toLowerCase().contains(g.getDescription().toLowerCase()))
                );
                if (gapMatches) {
                    return new ItemEvaluation("MISSING_SUPPORT", null, "SUPPORTED",
                            "Identified evidence gap is supported by active case database records.", "Confirmed Database Evidence Gap");
                }
                return new ItemEvaluation("MISSING_SUPPORT", null, "PARTIALLY_SUPPORTED",
                        "Evidence gap noted; further verification against physical dossier recommended.", null);

            case "CONTRADICTION":
                if (matchedEvidence != null) {
                    String suppContent = matchedEvidence.getEvidenceNumber() + ": " + matchedEvidence.getTitle();
                    return new ItemEvaluation("CONTRADICTION", matchedEvidence, "CONFLICT",
                            "Contradiction identified involving evidence " + matchedEvidence.getEvidenceNumber(), suppContent);
                }
                return new ItemEvaluation("CONTRADICTION", null, "CONFLICT",
                        "AI highlighted contradictory case information requiring investigator review.", null);

            case "RECOMMENDED_ACTION":
                if (matchedEvidence != null) {
                    String suppContent = matchedEvidence.getEvidenceNumber() + ": " + matchedEvidence.getTitle();
                    return new ItemEvaluation("CLAIM", matchedEvidence, "SUPPORTED",
                            "Recommended action is grounded in verified evidence " + matchedEvidence.getEvidenceNumber(), suppContent);
                }
                // Check if action addresses an existing evidence gap
                boolean addressesGap = gaps.stream().anyMatch(g ->
                        (g.getCategory() != null && fullText.toLowerCase().contains(g.getCategory().toLowerCase())) ||
                        (g.getTitle() != null && fullText.toLowerCase().contains(g.getTitle().toLowerCase()))
                );
                if (addressesGap) {
                    return new ItemEvaluation("CLAIM", null, "SUPPORTED",
                            "Recommended action directly addresses an identified database evidence gap.", "Targeted Evidence Gap Action");
                }
                return new ItemEvaluation("CLAIM", null, "VERIFICATION_REQUIRED",
                        "Action suggested by AI; investigator discretion required.", null);

            case "UNCERTAINTY":
            default:
                return new ItemEvaluation("CLAIM", matchedEvidence, "VERIFICATION_REQUIRED",
                        "Uncertainty preserved; cannot be converted into established evidence.", null);
        }
    }

    private String computeOverallSupportLevel(List<EvidenceVerificationItemDTO> items, int missingCount, int conflictCount) {
        if (items.isEmpty()) {
            return "VERIFICATION_REQUIRED";
        }
        if (conflictCount > 0) {
            return "UNSUPPORTED";
        }
        long supportedCount = items.stream().filter(i -> "SUPPORTED".equals(i.getResult())).count();
        long partialCount = items.stream().filter(i -> "PARTIALLY_SUPPORTED".equals(i.getResult())).count();
        long missingOrNotFound = items.stream().filter(i -> "NOT_FOUND".equals(i.getResult()) || "UNSUPPORTED".equals(i.getResult())).count();

        if (missingOrNotFound > 0 && supportedCount == 0 && partialCount == 0) {
            return "UNSUPPORTED";
        }
        if (supportedCount > 0 && missingOrNotFound == 0 && conflictCount == 0) {
            return "SUPPORTED";
        }
        if (supportedCount > 0 || partialCount > 0) {
            return "PARTIALLY_SUPPORTED";
        }
        return "VERIFICATION_REQUIRED";
    }

    private String buildVerificationSummary(String supportLevel, int total, int verified, int missing, int conflict) {
        switch (supportLevel) {
            case "SUPPORTED":
                return String.format("All %d recommendation items are supported by verified evidence currently in the CrimeSphere database.", total);
            case "PARTIALLY_SUPPORTED":
                return String.format("%d of %d items are supported by available evidence; %d require additional verification or missing support.", verified, total, missing);
            case "UNSUPPORTED":
                return String.format("Recommendation contains %d unsupported/missing references or %d conflicts with stored case evidence.", missing, conflict);
            case "VERIFICATION_REQUIRED":
            default:
                return String.format("Recommendation items contain investigative hypotheses requiring investigator discretion and additional evidence collection.", total);
        }
    }

    private Evidence findMatchingEvidenceByKeyword(String text, List<Evidence> allCaseEvidence) {
        String lowerText = text.toLowerCase();
        for (Evidence ev : allCaseEvidence) {
            if ("REJECTED".equalsIgnoreCase(ev.getStatus()) || "ARCHIVED".equalsIgnoreCase(ev.getStatus())) continue;
            String type = ev.getEvidenceType() != null ? ev.getEvidenceType().toLowerCase() : "";
            String title = ev.getTitle() != null ? ev.getTitle().toLowerCase() : "";
            if ((!type.isEmpty() && lowerText.contains(type)) || (!title.isEmpty() && lowerText.contains(title))) {
                return ev;
            }
        }
        return null;
    }

    private Set<String> extractEvidenceNumbers(String text1, String text2) {
        Set<String> result = new HashSet<>();
        if (text1 != null) {
            Matcher m = EVIDENCE_NUM_PATTERN.matcher(text1);
            while (m.find()) {
                result.add(m.group().trim().toUpperCase());
            }
        }
        if (text2 != null) {
            Matcher m = EVIDENCE_NUM_PATTERN.matcher(text2);
            while (m.find()) {
                result.add(m.group().trim().toUpperCase());
            }
        }
        return result;
    }

    private List<UUID> extractUuids(String text1, String text2) {
        List<UUID> result = new ArrayList<>();
        if (text1 != null) {
            Matcher m = UUID_PATTERN.matcher(text1);
            while (m.find()) {
                try { result.add(UUID.fromString(m.group())); } catch (Exception ignored) {}
            }
        }
        if (text2 != null) {
            Matcher m = UUID_PATTERN.matcher(text2);
            while (m.find()) {
                try { result.add(UUID.fromString(m.group())); } catch (Exception ignored) {}
            }
        }
        return result;
    }

    private EvidenceVerificationResponse mapToResponseDto(EvidenceVerification v, Case parentCase) {
        List<EvidenceVerificationItem> items = verificationItemRepository.findByVerificationId(v.getId());
        List<EvidenceVerificationItemDTO> itemDtos = items.stream().map(i -> new EvidenceVerificationItemDTO(
                i.getId(),
                v.getId(),
                i.getEvidence() != null ? i.getEvidence().getId() : null,
                i.getEvidence() != null ? i.getEvidence().getEvidenceNumber() : null,
                i.getEvidence() != null ? i.getEvidence().getTitle() : null,
                i.getItemType(),
                i.getClaim(),
                i.getSupportingContent(),
                i.getResult(),
                i.getReason(),
                i.getCreatedAt()
        )).collect(Collectors.toList());

        return new EvidenceVerificationResponse(
                v.getId(),
                parentCase.getId(),
                parentCase.getCaseNumber(),
                v.getAiRecommendation() != null ? v.getAiRecommendation().getId() : null,
                v.getRecommendationItem() != null ? v.getRecommendationItem().getId() : null,
                v.getVerificationStatus(),
                v.getSupportLevel(),
                v.getVerificationSummary(),
                v.getVerifiedEvidenceCount(),
                v.getMissingEvidenceCount(),
                v.getConflictCount(),
                v.getVerifiedBy() != null ? v.getVerifiedBy().getFullName() : null,
                v.getVerifiedAt(),
                v.getCreatedAt(),
                itemDtos
        );
    }

    private PoliceOfficer getAuthenticatedOfficer(String policeId) {
        return officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new ResourceNotFoundException("Police officer not found: " + policeId));
    }

    private Case getAuthorizedCase(UUID caseId, PoliceOfficer officer) {
        UUID stationId = officer.getStation() != null ? officer.getStation().getId() : null;
        if (stationId == null) {
            throw new IllegalStateException("Police officer has no assigned station");
        }
        return caseRepository.findByIdAndStationId(caseId, stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found or access restricted to your station"));
    }

    private AiRecommendation getAuthorizedRecommendation(UUID recommendationId, Case parentCase) {
        AiRecommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new ResourceNotFoundException("AI recommendation not found: " + recommendationId));
        if (!rec.getCaseEntity().getId().equals(parentCase.getId())) {
            throw new AccessDeniedException("AI recommendation does not belong to specified case");
        }
        return rec;
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

    private static class ItemEvaluation {
        final String itemType;
        final Evidence matchedEvidence;
        final String result;
        final String reason;
        final String supportingContent;

        ItemEvaluation(String itemType, Evidence matchedEvidence, String result, String reason, String supportingContent) {
            this.itemType = itemType;
            this.matchedEvidence = matchedEvidence;
            this.result = result;
            this.reason = reason;
            this.supportingContent = supportingContent;
        }
    }
}
