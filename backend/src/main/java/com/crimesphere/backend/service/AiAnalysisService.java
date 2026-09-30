package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.ai.*;
import com.crimesphere.backend.dto.analysis.CaseStateDTO;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AiAnalysisService.class);

    private final CaseRepository caseRepository;
    private final FIRRepository firRepository;
    private final VictimRepository victimRepository;
    private final SuspectRepository suspectRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceGapRepository evidenceGapRepository;
    private final PoliceOfficerRepository officerRepository;
    private final AuditLogRepository auditLogRepository;
    private final CaseStateService caseStateService;
    private final AiServiceClient aiServiceClient;
    private final AiRetrievalService aiRetrievalService;

    private final AiRecommendationRepository recommendationRepository;
    private final AiRecommendationItemRepository recommendationItemRepository;
    private final AiRecommendationEvidenceRepository recommendationEvidenceRepository;
    private final RecommendationVerificationRepository verificationRepository;
    private final RecommendationVerificationEvidenceRepository verificationEvidenceRepository;
    private final InvestigatorReviewRepository reviewRepository;
    private final NotificationService notificationService;

    public AiAnalysisService(
            CaseRepository caseRepository,
            FIRRepository firRepository,
            VictimRepository victimRepository,
            SuspectRepository suspectRepository,
            EvidenceRepository evidenceRepository,
            EvidenceGapRepository evidenceGapRepository,
            PoliceOfficerRepository officerRepository,
            AuditLogRepository auditLogRepository,
            CaseStateService caseStateService,
            AiServiceClient aiServiceClient,
            AiRetrievalService aiRetrievalService,
            AiRecommendationRepository recommendationRepository,
            AiRecommendationItemRepository recommendationItemRepository,
            AiRecommendationEvidenceRepository recommendationEvidenceRepository,
            RecommendationVerificationRepository verificationRepository,
            RecommendationVerificationEvidenceRepository verificationEvidenceRepository
    ) {
        this(caseRepository, firRepository, victimRepository, suspectRepository,
             evidenceRepository, evidenceGapRepository, officerRepository, auditLogRepository,
             caseStateService, aiServiceClient, aiRetrievalService, recommendationRepository,
             recommendationItemRepository, recommendationEvidenceRepository, verificationRepository,
             verificationEvidenceRepository, null, null);
    }

    public AiAnalysisService(
            CaseRepository caseRepository,
            FIRRepository firRepository,
            VictimRepository victimRepository,
            SuspectRepository suspectRepository,
            EvidenceRepository evidenceRepository,
            EvidenceGapRepository evidenceGapRepository,
            PoliceOfficerRepository officerRepository,
            AuditLogRepository auditLogRepository,
            CaseStateService caseStateService,
            AiServiceClient aiServiceClient,
            AiRetrievalService aiRetrievalService,
            AiRecommendationRepository recommendationRepository,
            AiRecommendationItemRepository recommendationItemRepository,
            AiRecommendationEvidenceRepository recommendationEvidenceRepository,
            RecommendationVerificationRepository verificationRepository,
            RecommendationVerificationEvidenceRepository verificationEvidenceRepository,
            InvestigatorReviewRepository reviewRepository
    ) {
        this(caseRepository, firRepository, victimRepository, suspectRepository,
             evidenceRepository, evidenceGapRepository, officerRepository, auditLogRepository,
             caseStateService, aiServiceClient, aiRetrievalService, recommendationRepository,
             recommendationItemRepository, recommendationEvidenceRepository, verificationRepository,
             verificationEvidenceRepository, reviewRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AiAnalysisService(
            CaseRepository caseRepository,
            FIRRepository firRepository,
            VictimRepository victimRepository,
            SuspectRepository suspectRepository,
            EvidenceRepository evidenceRepository,
            EvidenceGapRepository evidenceGapRepository,
            PoliceOfficerRepository officerRepository,
            AuditLogRepository auditLogRepository,
            CaseStateService caseStateService,
            AiServiceClient aiServiceClient,
            AiRetrievalService aiRetrievalService,
            AiRecommendationRepository recommendationRepository,
            AiRecommendationItemRepository recommendationItemRepository,
            AiRecommendationEvidenceRepository recommendationEvidenceRepository,
            RecommendationVerificationRepository verificationRepository,
            RecommendationVerificationEvidenceRepository verificationEvidenceRepository,
            InvestigatorReviewRepository reviewRepository,
            NotificationService notificationService
    ) {
        this.caseRepository = caseRepository;
        this.firRepository = firRepository;
        this.victimRepository = victimRepository;
        this.suspectRepository = suspectRepository;
        this.evidenceRepository = evidenceRepository;
        this.evidenceGapRepository = evidenceGapRepository;
        this.officerRepository = officerRepository;
        this.auditLogRepository = auditLogRepository;
        this.caseStateService = caseStateService;
        this.aiServiceClient = aiServiceClient;
        this.aiRetrievalService = aiRetrievalService;
        this.recommendationRepository = recommendationRepository;
        this.recommendationItemRepository = recommendationItemRepository;
        this.recommendationEvidenceRepository = recommendationEvidenceRepository;
        this.verificationRepository = verificationRepository;
        this.verificationEvidenceRepository = verificationEvidenceRepository;
        this.reviewRepository = reviewRepository;
        this.notificationService = notificationService;
    }

    /**
     * Compiles minimized case dossier, performs RAG retrieval, dispatches to FastAPI Groq agent,
     * validates evidence links, persists structured recommendation, and logs audit events.
     */
    public AiCaseAnalysisResponse dispatchCaseToAiService(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        // 1. Check if an AI analysis already exists for this case to avoid regenerating unnecessarily
        if (recommendationRepository != null) {
            List<AiRecommendation> existingRecs = recommendationRepository.findByCaseEntityIdOrderByCreatedAtDesc(parentCase.getId());
            if (existingRecs != null && !existingRecs.isEmpty()) {
                AiRecommendation existingRec = existingRecs.get(0);
                AiCaseAnalysisResponse detail = getAnalysisDetail(caseId, existingRec.getId(), policeId);
                if (detail != null && Boolean.TRUE.equals(detail.getAnalysisAvailable()) && detail.getSummary() != null) {
                    logger.info("Returning existing persisted AI analysis for case {}", parentCase.getCaseNumber());
                    return detail;
                }
            }
        }

        // 2. Audit Request Initiation
        recordAuditLog(officer, "AI_ANALYSIS_REQUESTED",
                String.format("Officer requested AI analysis for case %s", parentCase.getCaseNumber()));

        AiCaseAnalysisResponse response;
        try {
            // 2. Compile Minimized Case Dossier
            AiCaseAnalysisRequest request = buildCaseAnalysisPayload(parentCase);

            // 2b. Phase 10 RAG Retrieval: Attach similar historical case context
            try {
                if (aiRetrievalService != null) {
                    AiRetrievalResponse retResp = aiRetrievalService.retrieveSimilarKnowledge(parentCase.getId(), null, 5, 0.70, policeId);
                    if (retResp != null && retResp.getResults() != null && !retResp.getResults().isEmpty()) {
                        request.setRetrievedContext(retResp.getResults());
                        logger.info("Attached {} RAG retrieved context items to case analysis request for case {}",
                                retResp.getResults().size(), parentCase.getCaseNumber());
                    }
                }
            } catch (Exception e) {
                logger.warn("RAG retrieval skipped for case {}: {}", parentCase.getCaseNumber(), e.getMessage());
            }

            // 3. Dispatch to FastAPI AI Service via AiServiceClient
            response = aiServiceClient.sendCaseAnalysis(request);

            if (response == null) {
                throw new RuntimeException("Received empty response from AI Service");
            }
        } catch (Exception e) {
            recordAuditLog(officer, "AI_ANALYSIS_FAILED",
                    String.format("AI analysis failed for case %s: %s", parentCase.getCaseNumber(), e.getMessage()));
            throw e;
        }


        // 4. Persist Analysis and Recommendation Items (Phase 9+ when summary is present)
        if (response.getSummary() != null || response.getAnalysisId() != null) {
            persistAiAnalysis(parentCase, officer, response);
            response.setAnalysisAvailable(true);
            if (response.getStatus() == null || "RECEIVED".equalsIgnoreCase(response.getStatus())) {
                response.setStatus("REVIEW_REQUIRED");
            }
        } else {
            response.setAnalysisAvailable(false);
        }

        if (response.getCaseId() == null) {
            response.setCaseId(parentCase.getId());
        }
        if (response.getCaseNumber() == null) {
            response.setCaseNumber(parentCase.getCaseNumber());
        }

        // 5. Record Audit Log for Success
        recordAuditLog(officer, "AI_ANALYSIS_COMPLETED",
                String.format("AI analysis successfully dispatched for case %s (Status: %s)",
                        parentCase.getCaseNumber(), response.getStatus()));

        // 6. Trigger AI_REVIEW_REQUIRED notification
        try {
            if (notificationService != null) {
                notificationService.dispatchCaseNotification(
                        parentCase,
                        NotificationType.AI_REVIEW_REQUIRED,
                        "CrimeSphere AI — AI Review Required: " + parentCase.getCaseNumber(),
                        String.format("A new AI-generated investigation finding requires investigator review for case %s (%s).",
                                parentCase.getCaseNumber(), parentCase.getTitle()),
                        "Please review and validate the finding in CrimeSphere AI. AI recommendations are decision-support outputs and require investigator validation.",
                        officer
                );
            }
        } catch (Exception e) {
            logger.warn("AI_REVIEW_REQUIRED notification dispatch failed: {}", e.getMessage());
        }

        logger.info("Successfully completed AI investigation reasoning for case {} by officer {}",
                parentCase.getCaseNumber(), policeId);

        return response;
    }

    /**
     * Persists AI recommendation entities and maps validated evidence references.
     */
    @Transactional
    public void persistAiAnalysis(Case parentCase, PoliceOfficer officer, AiCaseAnalysisResponse response) {
        try {
            // 1. Map existing case evidence by evidenceNumber
            List<Evidence> existingEvidence = evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(parentCase.getId());
            Map<String, Evidence> evidenceMap = existingEvidence.stream()
                    .filter(e -> e.getEvidenceNumber() != null)
                    .collect(Collectors.toMap(Evidence::getEvidenceNumber, e -> e, (e1, e2) -> e1));

            // 2. Create AiRecommendation
            AiRecommendation rec = new AiRecommendation();
            rec.setCaseEntity(parentCase);
            rec.setAnalysisVersion(response.getAnalysisVersion() != null ? response.getAnalysisVersion() : "phase-9");
            rec.setSummary(response.getSummary());
            rec.setModelName(response.getModelName());
            rec.setPromptVersion(response.getPromptVersion());
            rec.setStatus(response.getStatus() != null ? response.getStatus() : "REVIEW_REQUIRED");
            rec.setCreatedBy(officer);
            rec.setCreatedAt(OffsetDateTime.now());

            AiRecommendation savedRec = recommendationRepository.save(rec);

            // 3. Save Key Observations
            if (response.getKeyObservations() != null) {
                for (AiKeyObservationDto obs : response.getKeyObservations()) {
                    AiRecommendationItem item = new AiRecommendationItem();
                    item.setRecommendation(savedRec);
                    item.setItemType("OBSERVATION");
                    item.setContent(obs.getObservation());
                    item.setReasoning(obs.getReasoning());
                    AiRecommendationItem savedItem = recommendationItemRepository.save(item);

                    linkEvidenceReferences(savedItem, obs.getEvidenceReferences(), evidenceMap);
                }
            }

            // 4. Save Potential Leads
            if (response.getPotentialLeads() != null) {
                for (AiPotentialLeadDto lead : response.getPotentialLeads()) {
                    AiRecommendationItem item = new AiRecommendationItem();
                    item.setRecommendation(savedRec);
                    item.setItemType("POTENTIAL_LEAD");
                    item.setContent(lead.getLead());
                    item.setReasoning(lead.getBasis());
                    item.setPriority(lead.getPriority());
                    AiRecommendationItem savedItem = recommendationItemRepository.save(item);

                    linkEvidenceReferences(savedItem, lead.getEvidenceReferences(), evidenceMap);
                }
            }

            // 5. Save Evidence Gaps
            if (response.getEvidenceGaps() != null) {
                for (AiEvidenceGapAnalysisDto gap : response.getEvidenceGaps()) {
                    AiRecommendationItem item = new AiRecommendationItem();
                    item.setRecommendation(savedRec);
                    item.setItemType("EVIDENCE_GAP");
                    item.setContent(gap.getGap());
                    item.setReasoning(gap.getReason());
                    item.setPriority(gap.getSeverity());
                    recommendationItemRepository.save(item);
                }
            }

            // 6. Save Contradictions
            if (response.getContradictions() != null) {
                for (AiContradictionDto contra : response.getContradictions()) {
                    AiRecommendationItem item = new AiRecommendationItem();
                    item.setRecommendation(savedRec);
                    item.setItemType("CONTRADICTION");
                    item.setContent(contra.getDescription());
                    recommendationItemRepository.save(item);
                }
            }

            // 7. Save Recommended Actions
            if (response.getRecommendedActions() != null) {
                for (AiRecommendedActionDto action : response.getRecommendedActions()) {
                    AiRecommendationItem item = new AiRecommendationItem();
                    item.setRecommendation(savedRec);
                    item.setItemType("RECOMMENDED_ACTION");
                    item.setContent(action.getAction());
                    item.setReasoning(action.getReason());
                    item.setPriority(action.getPriority());
                    AiRecommendationItem savedItem = recommendationItemRepository.save(item);

                    linkEvidenceReferences(savedItem, action.getEvidenceReferences(), evidenceMap);
                }
            }

            // 8. Save Uncertainties & Limitations
            if (response.getUncertainties() != null) {
                for (String uncertainty : response.getUncertainties()) {
                    AiRecommendationItem item = new AiRecommendationItem();
                    item.setRecommendation(savedRec);
                    item.setItemType("UNCERTAINTY");
                    item.setContent(uncertainty);
                    recommendationItemRepository.save(item);
                }
            }

            if (response.getLimitations() != null) {
                for (String limitation : response.getLimitations()) {
                    AiRecommendationItem item = new AiRecommendationItem();
                    item.setRecommendation(savedRec);
                    item.setItemType("LIMITATION");
                    item.setContent(limitation);
                    recommendationItemRepository.save(item);
                }
            }

            // Link ID in response
            response.setAnalysisId(savedRec.getId().toString());
            response.setCreatedAt(savedRec.getCreatedAt());
            response.setAnalysisAvailable(true);

        } catch (Exception e) {
            logger.error("Failed to persist AI recommendation record: {}", e.getMessage(), e);
        }
    }

    private void linkEvidenceReferences(AiRecommendationItem item, List<String> references, Map<String, Evidence> evidenceMap) {
        if (references == null || references.isEmpty()) return;
        for (String ref : references) {
            if (ref != null && evidenceMap.containsKey(ref.trim())) {
                Evidence ev = evidenceMap.get(ref.trim());
                AiRecommendationEvidence link = new AiRecommendationEvidence(null, item, ev);
                recommendationEvidenceRepository.save(link);
            }
        }
    }

    /**
     * Retrieves chronological AI analysis history for a case.
     */
    @Transactional(readOnly = true)
    public List<AiRecommendationHistoryDto> getCaseAnalysisHistory(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        List<AiRecommendation> history = recommendationRepository.findByCaseEntityIdOrderByCreatedAtDesc(parentCase.getId());
        return history.stream().map(rec -> {
            int itemCount = rec.getItems() != null ? rec.getItems().size() : 0;
            String officerName = rec.getCreatedBy() != null ? rec.getCreatedBy().getFullName() : "System";
            return new AiRecommendationHistoryDto(
                    rec.getId(),
                    parentCase.getId(),
                    rec.getAnalysisVersion(),
                    rec.getSummary(),
                    rec.getModelName(),
                    rec.getPromptVersion(),
                    rec.getStatus(),
                    rec.getCreatedAt(),
                    officerName,
                    itemCount
            );
        }).collect(Collectors.toList());
    }

    /**
     * Retrieves specific past AI analysis by ID.
     */
    @Transactional(readOnly = true)
    public AiCaseAnalysisResponse getAnalysisDetail(UUID caseId, UUID analysisId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        AiRecommendation rec = recommendationRepository.findByIdAndCaseEntityId(analysisId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("AI Analysis record not found for this case"));

        AiCaseAnalysisResponse res = new AiCaseAnalysisResponse();
        res.setAnalysisId(rec.getId().toString());
        res.setRequestId(rec.getId().toString());
        res.setAnalysisAvailable(true);
        res.setCaseId(parentCase.getId());
        res.setCaseNumber(parentCase.getCaseNumber());
        res.setAnalysisVersion(rec.getAnalysisVersion() != null ? rec.getAnalysisVersion() : "phase-9");
        res.setModelName(rec.getModelName() != null ? rec.getModelName() : "groq/compound-mini");
        res.setPromptVersion(rec.getPromptVersion() != null ? rec.getPromptVersion() : "prompt-v1");
        res.setStatus(rec.getStatus() != null ? rec.getStatus() : "REVIEW_REQUIRED");
        res.setSummary(rec.getSummary());
        res.setCreatedAt(rec.getCreatedAt());

        List<AiRecommendationItem> items = recommendationItemRepository.findByRecommendationIdOrderByCreatedAtAsc(rec.getId());

        for (AiRecommendationItem item : items) {
            List<AiRecommendationEvidence> evLinks = recommendationEvidenceRepository.findByRecommendationItemId(item.getId());
            List<String> evRefs = evLinks.stream()
                    .map(l -> l.getEvidence() != null ? l.getEvidence().getEvidenceNumber() : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            ItemVerificationDto verifDto = verificationRepository.findFirstByRecommendationItemIdOrderByCreatedAtDesc(item.getId())
                    .map(rv -> {
                        List<RecommendationVerificationEvidence> rveList = verificationEvidenceRepository.findByVerificationId(rv.getId());
                        List<VerificationEvidenceDetailDto> evDetails = rveList.stream().map(rve -> new VerificationEvidenceDetailDto(
                                rve.getEvidence().getId(),
                                rve.getEvidence().getEvidenceNumber(),
                                rve.getEvidence().getTitle(),
                                rve.getEvidence().getEvidenceType(),
                                rve.getEvidence().getStatus(),
                                rve.getRelationship(),
                                rve.getSupportNote()
                        )).collect(Collectors.toList());

                        return new ItemVerificationDto(
                                rv.getId(),
                                rv.getRecommendationItem().getId(),
                                rv.getRecommendationItem().getItemType(),
                                rv.getRecommendationItem().getContent(),
                                rv.getVerificationStatus(),
                                rv.getSupportLevel(),
                                rv.getVerificationMethod(),
                                rv.getVerificationSummary(),
                                rv.getVerifiedAt(),
                                rv.getVerifiedBy() != null ? rv.getVerifiedBy().getFullName() : null,
                                 evDetails
                        );
                    }).orElse(null);

            InvestigatorReviewResponse reviewDto = null;
            if (reviewRepository != null) {
                ItemVerificationDto finalVerif = verifDto;
                reviewDto = reviewRepository.findFirstByRecommendationItemIdOrderByCreatedAtDesc(item.getId())
                        .map(r -> new InvestigatorReviewResponse(
                                r.getId(),
                                r.getCaseEntity().getId(),
                                rec.getId(),
                                r.getRecommendationItem().getId(),
                                r.getDecision(),
                                r.getOriginalContent(),
                                r.getReviewedContent(),
                                r.getReviewComment(),
                                r.getVerificationStatusAtReview(),
                                r.getSupportLevelAtReview(),
                                r.getReviewer() != null ? r.getReviewer().getFullName() : null,
                                r.getCreatedAt(),
                                r.getUpdatedAt()
                        )).orElseGet(() -> new InvestigatorReviewResponse(
                                null,
                                parentCase.getId(),
                                rec.getId(),
                                item.getId(),
                                "PENDING_REVIEW",
                                item.getContent(),
                                null,
                                null,
                                finalVerif != null ? finalVerif.getStatus() : "UNVERIFIED",
                                finalVerif != null ? finalVerif.getSupportLevel() : "NONE",
                                null,
                                item.getCreatedAt(),
                                null
                        ));
            }

            switch (item.getItemType()) {
                case "OBSERVATION" -> {
                    AiKeyObservationDto obs = new AiKeyObservationDto(item.getContent(), evRefs, item.getReasoning());
                    obs.setId(item.getId());
                    obs.setVerification(verifDto);
                    obs.setReview(reviewDto);
                    res.getKeyObservations().add(obs);
                }
                case "POTENTIAL_LEAD" -> {
                    AiPotentialLeadDto lead = new AiPotentialLeadDto(item.getContent(), item.getReasoning(), evRefs, item.getPriority());
                    lead.setId(item.getId());
                    lead.setVerification(verifDto);
                    lead.setReview(reviewDto);
                    res.getPotentialLeads().add(lead);
                }
                case "EVIDENCE_GAP" -> {
                    AiEvidenceGapAnalysisDto gap = new AiEvidenceGapAnalysisDto(item.getContent(), item.getReasoning(), item.getPriority());
                    gap.setId(item.getId());
                    gap.setVerification(verifDto);
                    gap.setReview(reviewDto);
                    res.getEvidenceGaps().add(gap);
                }
                case "CONTRADICTION" -> {
                    AiContradictionDto contra = new AiContradictionDto(item.getContent(), evRefs);
                    contra.setId(item.getId());
                    contra.setVerification(verifDto);
                    contra.setReview(reviewDto);
                    res.getContradictions().add(contra);
                }
                case "RECOMMENDED_ACTION" -> {
                    AiRecommendedActionDto act = new AiRecommendedActionDto(item.getContent(), item.getReasoning(), item.getPriority(), evRefs);
                    act.setId(item.getId());
                    act.setVerification(verifDto);
                    act.setReview(reviewDto);
                    res.getRecommendedActions().add(act);
                }
                case "UNCERTAINTY" -> res.getUncertainties().add(item.getContent());
                case "LIMITATION" -> res.getLimitations().add(item.getContent());
            }
        }

        return res;
    }

    /**
     * Constructs data-minimized payload adhering to Phase 8 and Phase 9 standards.
     */
    public AiCaseAnalysisRequest buildCaseAnalysisPayload(Case parentCase) {
        UUID caseId = parentCase.getId();

        // Evaluate dynamic case state and completeness
        CaseStateDTO stateDto = caseStateService.computeCaseState(parentCase);

        AiCaseAnalysisRequest payload = new AiCaseAnalysisRequest();
        payload.setCaseId(parentCase.getId());
        payload.setCaseNumber(parentCase.getCaseNumber());
        payload.setCaseTitle(parentCase.getTitle());
        payload.setCaseType(parentCase.getCaseType() != null ? parentCase.getCaseType() : "OTHER");
        payload.setCaseStatus(parentCase.getStatus() != null ? parentCase.getStatus() : "OPEN");
        payload.setInvestigationState(stateDto.getCurrentState());
        payload.setInvestigationCompleteness(stateDto.getProgressPercentage());

        // FIR (Data minimized)
        firRepository.findByCaseEntityId(caseId).ifPresent(fir -> {
            AiFirDto firDto = new AiFirDto();
            firDto.setId(fir.getId());
            firDto.setFirNumber(fir.getFirNumber());
            firDto.setPoliceStation(fir.getPoliceStation());
            firDto.setRegistrationDate(fir.getRegistrationDate());
            firDto.setIncidentDate(fir.getIncidentDate());
            firDto.setIncidentLocation(fir.getIncidentLocation());
            firDto.setOffenceType(fir.getOffenceType());
            firDto.setSectionsOfLaw(fir.getSectionsOfLaw());
            firDto.setDescription(fir.getDescription());
            payload.setFir(firDto);
        });

        // Victims (Data minimized)
        List<AiVictimDto> victimDtos = victimRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId).stream()
                .map(v -> {
                    AiVictimDto dto = new AiVictimDto();
                    dto.setId(v.getId());
                    dto.setFullName(v.getFullName());
                    dto.setAge(v.getAge());
                    dto.setGender(v.getGender());
                    dto.setPhone(v.getPhone());
                    dto.setOccupation(v.getOccupation());
                    dto.setRelationshipToCase(v.getRelationshipToCase());
                    dto.setStatus(v.getStatus());
                    return dto;
                })
                .collect(Collectors.toList());
        payload.setVictims(victimDtos);

        // Suspects (Data minimized)
        List<AiSuspectDto> suspectDtos = suspectRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId).stream()
                .map(s -> {
                    AiSuspectDto dto = new AiSuspectDto();
                    dto.setId(s.getId());
                    dto.setFullName(s.getFullName());
                    dto.setAlias(s.getAlias());
                    dto.setAge(s.getAge());
                    dto.setGender(s.getGender());
                    dto.setOccupation(s.getOccupation());
                    dto.setInvestigationStatus(s.getInvestigationStatus());
                    return dto;
                })
                .collect(Collectors.toList());
        payload.setSuspects(suspectDtos);

        // Evidence (Data minimized: metadata and cryptographic hashes)
        List<AiEvidenceDto> evidenceDtos = evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId).stream()
                .map(e -> {
                    AiEvidenceDto dto = new AiEvidenceDto();
                    dto.setId(e.getId());
                    dto.setEvidenceNumber(e.getEvidenceNumber());
                    dto.setTitle(e.getTitle());
                    dto.setDescription(e.getDescription());
                    dto.setEvidenceType(e.getEvidenceType());
                    dto.setStatus(e.getStatus());
                    dto.setMimeType(e.getMimeType());
                    dto.setFileSize(e.getFileSize());
                    dto.setFileHash(e.getFileHash());
                    dto.setCollectedAt(e.getCollectedAt());
                    dto.setCollectedLocation(e.getCollectedLocation());
                    dto.setCollectedBy(e.getCollectedBy());
                    return dto;
                })
                .collect(Collectors.toList());
        payload.setEvidence(evidenceDtos);

        // Evidence Gaps
        List<AiEvidenceGapDto> gapDtos = evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId).stream()
                .map(g -> {
                    AiEvidenceGapDto dto = new AiEvidenceGapDto();
                    dto.setId(g.getId());
                    dto.setCategory(g.getCategory());
                    dto.setTitle(g.getTitle());
                    dto.setDescription(g.getDescription());
                    dto.setSeverity(g.getSeverity());
                    dto.setStatus(g.getStatus());
                    dto.setReason(g.getReason());
                    return dto;
                })
                .collect(Collectors.toList());
        payload.setEvidenceGaps(gapDtos);

        return payload;
    }

    public Case getAuthorizedCase(UUID caseId, PoliceOfficer officer) {
        UUID stationId = officer.getStation() != null ? officer.getStation().getId() : null;
        if (stationId == null) {
            throw new IllegalStateException("Police officer has no assigned station");
        }
        return caseRepository.findByIdAndStationId(caseId, stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found or access restricted to your station"));
    }

    public PoliceOfficer getAuthenticatedOfficer(String policeId) {
        return officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated police officer record not found"));
    }

    private void recordAuditLog(PoliceOfficer officer, String action, String details) {
        try {
            AuditLog log = new AuditLog(null, officer, action, "SUCCESS", null, details);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to record audit log for action {}: {}", action, e.getMessage());
        }
    }
}
