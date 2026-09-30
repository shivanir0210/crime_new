package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.CaseReportSummaryDTO;
import com.crimesphere.backend.dto.analysis.CaseStateDTO;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CaseReportDataBuilder {

    private final CaseRepository caseRepository;
    private final FIRRepository firRepository;
    private final VictimRepository victimRepository;
    private final SuspectRepository suspectRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceGapRepository evidenceGapRepository;
    private final CaseStateService caseStateService;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final RecommendationVerificationRepository recommendationVerificationRepository;
    private final InvestigatorReviewRepository investigatorReviewRepository;
    private final PublicInformationItemRepository publicInformationItemRepository;
    private final NotificationRepository notificationRepository;

    public CaseReportDataBuilder(CaseRepository caseRepository,
                                 FIRRepository firRepository,
                                 VictimRepository victimRepository,
                                 SuspectRepository suspectRepository,
                                 EvidenceRepository evidenceRepository,
                                 EvidenceGapRepository evidenceGapRepository,
                                 CaseStateService caseStateService,
                                 AiRecommendationRepository aiRecommendationRepository,
                                 RecommendationVerificationRepository recommendationVerificationRepository,
                                 InvestigatorReviewRepository investigatorReviewRepository,
                                 PublicInformationItemRepository publicInformationItemRepository,
                                 NotificationRepository notificationRepository) {
        this.caseRepository = caseRepository;
        this.firRepository = firRepository;
        this.victimRepository = victimRepository;
        this.suspectRepository = suspectRepository;
        this.evidenceRepository = evidenceRepository;
        this.evidenceGapRepository = evidenceGapRepository;
        this.caseStateService = caseStateService;
        this.aiRecommendationRepository = aiRecommendationRepository;
        this.recommendationVerificationRepository = recommendationVerificationRepository;
        this.investigatorReviewRepository = investigatorReviewRepository;
        this.publicInformationItemRepository = publicInformationItemRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public CaseReportSummaryDTO buildReportData(UUID caseId, PoliceOfficer officer) {
        Case crimeCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + caseId));

        // Station-level authorization check
        if (officer.getStation() != null && crimeCase.getStation() != null &&
                !officer.getStation().getId().equals(crimeCase.getStation().getId())) {
            throw new AccessDeniedException("Access Denied: Officer TN-POL-" + officer.getPoliceId() +
                    " is not authorized to generate report for case outside assigned station precinct.");
        }

        CaseReportSummaryDTO report = new CaseReportSummaryDTO();
        report.setReportTitle("INVESTIGATION CASE REPORT");
        report.setReportVersion("CrimeSphere-Case-Report-v1");
        report.setGeneratedAt(OffsetDateTime.now());
        report.setGeneratedByOfficerName(officer.getFullName() != null ? officer.getFullName() : "Officer " + officer.getPoliceId());
        report.setGeneratedByOfficerBadge(officer.getPoliceId());
        if (crimeCase.getStation() != null) {
            report.setPoliceStationName(crimeCase.getStation().getStationName());
            report.setPoliceStationDistrict(crimeCase.getStation().getDistrict());
        } else {
            report.setPoliceStationName("Precinct Headquarters");
            report.setPoliceStationDistrict("Central Precinct");
        }

        // Case Overview
        report.setCaseId(crimeCase.getId());
        report.setCaseNumber(crimeCase.getCaseNumber());
        report.setCaseTitle(crimeCase.getTitle());
        report.setDescription(crimeCase.getDescription());
        report.setCaseType(crimeCase.getCaseType() != null ? crimeCase.getCaseType() : "OTHER");
        report.setPriority(crimeCase.getPriority() != null ? crimeCase.getPriority() : "MEDIUM");
        report.setStatus(crimeCase.getStatus() != null ? crimeCase.getStatus() : "OPEN");
        report.setCreatedAt(crimeCase.getCreatedAt());
        report.setUpdatedAt(crimeCase.getUpdatedAt());

        // FIR Section
        Optional<FIR> firOpt = firRepository.findByCaseEntityId(caseId);
        if (firOpt.isPresent()) {
            FIR fir = firOpt.get();
            report.setFirExists(true);
            report.setFirNumber(fir.getFirNumber());
            report.setFirRegDate(fir.getRegistrationDate());
            report.setComplainantName(fir.getComplainantName());
            report.setComplainantContact(fir.getComplainantContact());
            report.setIncidentDate(fir.getIncidentDate());
            report.setIncidentLocation(fir.getIncidentLocation());
            report.setOffenceType(fir.getOffenceType());
            report.setSectionsOfLaw(fir.getSectionsOfLaw());
            report.setFirDescription(fir.getDescription());
        } else {
            report.setFirExists(false);
            report.setFirNumber("NOT REGISTERED / INFORMATION NOT AVAILABLE");
        }

        // Victims Section
        List<Victim> victimList = victimRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId);
        List<CaseReportSummaryDTO.VictimSummaryDTO> victimDTOs = victimList.stream().map(v -> {
            CaseReportSummaryDTO.VictimSummaryDTO dto = new CaseReportSummaryDTO.VictimSummaryDTO();
            dto.setFullName(v.getFullName());
            dto.setAge(v.getAge());
            dto.setGender(v.getGender());
            if (v.getDateOfBirth() != null) {
                dto.setDob(v.getDateOfBirth().atStartOfDay().atOffset(OffsetDateTime.now().getOffset()));
            }
            dto.setPhone(v.getPhone());
            dto.setAddress(v.getAddress());
            dto.setOccupation(v.getOccupation());
            dto.setRelationshipToCase(v.getRelationshipToCase());
            dto.setStatus(v.getStatus());
            dto.setDescription(v.getDescription());
            return dto;
        }).collect(Collectors.toList());
        report.setVictims(victimDTOs);

        // Suspects Section
        List<Suspect> suspectList = suspectRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId);
        List<CaseReportSummaryDTO.SuspectSummaryDTO> suspectDTOs = suspectList.stream().map(s -> {
            CaseReportSummaryDTO.SuspectSummaryDTO dto = new CaseReportSummaryDTO.SuspectSummaryDTO();
            dto.setFullName(s.getFullName());
            dto.setAlias(s.getAlias());
            dto.setAge(s.getAge());
            dto.setGender(s.getGender());
            dto.setPhone(s.getPhone());
            dto.setAddress(s.getAddress());
            dto.setOccupation(s.getOccupation());
            dto.setInvestigationStatus(s.getInvestigationStatus());
            dto.setDescription(s.getDescription());
            return dto;
        }).collect(Collectors.toList());
        report.setSuspects(suspectDTOs);

        // Evidence Section
        List<Evidence> evidenceList = evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
        report.setTotalEvidenceCount(evidenceList.size());
        report.setVerifiedEvidenceCount((int) evidenceList.stream().filter(e -> "VERIFIED".equalsIgnoreCase(e.getStatus())).count());
        report.setUnderReviewEvidenceCount((int) evidenceList.stream().filter(e -> "UNDER_REVIEW".equalsIgnoreCase(e.getStatus())).count());
        report.setRejectedEvidenceCount((int) evidenceList.stream().filter(e -> "REJECTED".equalsIgnoreCase(e.getStatus())).count());

        List<CaseReportSummaryDTO.EvidenceSummaryDTO> evidenceDTOs = evidenceList.stream().map(e -> {
            CaseReportSummaryDTO.EvidenceSummaryDTO dto = new CaseReportSummaryDTO.EvidenceSummaryDTO();
            dto.setEvidenceNumber(e.getEvidenceNumber());
            dto.setTitle(e.getTitle());
            dto.setDescription(e.getDescription());
            dto.setEvidenceType(e.getEvidenceType());
            dto.setStatus(e.getStatus());
            dto.setOriginalFileName(e.getOriginalFileName());
            dto.setMimeType(e.getMimeType());
            dto.setFileSizeBytes(e.getFileSize());
            dto.setFileHash(e.getFileHash());
            dto.setCollectedAt(e.getCollectedAt());
            dto.setCollectedLocation(e.getCollectedLocation());
            dto.setCollectedBy(e.getCollectedBy());
            dto.setNotes(e.getNotes());
            return dto;
        }).collect(Collectors.toList());
        report.setEvidenceItems(evidenceDTOs);

        // Evidence Gap & Investigation State (Phase 7)
        CaseStateDTO stateDTO = caseStateService.computeCaseState(crimeCase);
        if (stateDTO != null) {
            report.setInvestigationStateLabel(stateDTO.getStateLabel());
            report.setCompletenessPercentage(stateDTO.getProgressPercentage());
            report.setCompletedAreas(stateDTO.getCompletedAreas());
            report.setMissingAreas(stateDTO.getMissingAreas());
            report.setStateLastEvaluatedAt(stateDTO.getLastEvaluatedAt());
        }

        List<EvidenceGap> gapList = evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
        List<CaseReportSummaryDTO.EvidenceGapSummaryDTO> gapDTOs = gapList.stream().map(g -> {
            CaseReportSummaryDTO.EvidenceGapSummaryDTO dto = new CaseReportSummaryDTO.EvidenceGapSummaryDTO();
            dto.setCategory(g.getCategory());
            dto.setTitle(g.getTitle());
            dto.setDescription(g.getDescription());
            dto.setSeverity(g.getSeverity());
            dto.setStatus(g.getStatus());
            dto.setReason(g.getReason());
            return dto;
        }).collect(Collectors.toList());
        report.setEvidenceGaps(gapDTOs);

        // AI Analysis & Findings (Phase 8 - 11)
        List<AiRecommendation> recs = aiRecommendationRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
        if (!recs.isEmpty()) {
            AiRecommendation latestRec = recs.get(0);
            report.setAiAnalysisAvailable(true);
            report.setAnalysisVersion(latestRec.getAnalysisVersion());
            report.setModelName(latestRec.getModelName());
            report.setAnalysisDate(latestRec.getCreatedAt());
            report.setAiSummary(latestRec.getSummary());

            if (latestRec.getItems() != null) {
                List<RecommendationVerification> verifications = recommendationVerificationRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);

                List<CaseReportSummaryDTO.AiItemSummaryDTO> itemDTOs = latestRec.getItems().stream().map(item -> {
                    CaseReportSummaryDTO.AiItemSummaryDTO dto = new CaseReportSummaryDTO.AiItemSummaryDTO();
                    dto.setCategory(item.getItemType());
                    dto.setTitle(item.getReasoning() != null ? item.getItemType() + ": " + item.getReasoning() : item.getItemType());
                    dto.setContent(item.getContent());

                    if (item.getEvidenceReferences() != null) {
                        dto.setEvidenceReferences(item.getEvidenceReferences().stream()
                                .map(e -> e.getEvidence() != null ? e.getEvidence().getEvidenceNumber() : "EVD-REF")
                                .collect(Collectors.toList()));
                    }

                    // Attach Phase 11 verification matching item
                    Optional<RecommendationVerification> verOpt = verifications.stream()
                            .filter(v -> v.getRecommendationItem() != null && v.getRecommendationItem().getId().equals(item.getId()))
                            .findFirst();
                    if (verOpt.isPresent()) {
                        RecommendationVerification ver = verOpt.get();
                        dto.setVerificationStatus(ver.getVerificationStatus() != null ? ver.getVerificationStatus() : "UNVERIFIED");
                        dto.setSupportLevel(ver.getSupportLevel() != null ? ver.getSupportLevel() : "NONE");
                        dto.setVerificationSummary(ver.getVerificationSummary());
                        dto.setVerificationMethod(ver.getVerificationMethod());
                        dto.setVerifiedAt(ver.getCreatedAt());
                    } else {
                        dto.setVerificationStatus("UNVERIFIED");
                        dto.setSupportLevel("NONE");
                    }
                    return dto;
                }).collect(Collectors.toList());
                report.setAiFindings(itemDTOs);
            }
        } else {
            report.setAiAnalysisAvailable(false);
        }

        // Investigator Reviews (Phase 12)
        List<InvestigatorReview> reviewList = investigatorReviewRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
        List<CaseReportSummaryDTO.InvestigatorReviewSummaryDTO> reviewDTOs = reviewList.stream().map(r -> {
            CaseReportSummaryDTO.InvestigatorReviewSummaryDTO dto = new CaseReportSummaryDTO.InvestigatorReviewSummaryDTO();
            if (r.getRecommendationItem() != null) {
                dto.setItemTitle(r.getRecommendationItem().getItemType());
                dto.setCategory(r.getRecommendationItem().getItemType());
                dto.setOriginalAiContent(r.getRecommendationItem().getContent());
            }
            dto.setVerificationStatusAtReview(r.getVerificationStatusAtReview());
            dto.setSupportLevelAtReview(r.getSupportLevelAtReview());
            dto.setInvestigatorDecision(r.getDecision());
            dto.setReviewedContent(r.getReviewedContent());
            dto.setReviewComment(r.getReviewComment());
            dto.setReviewerName(r.getReviewer() != null ? r.getReviewer().getFullName() : "Investigator");
            dto.setReviewDate(r.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
        report.setInvestigatorReviews(reviewDTOs);

        // Public Information Section (Phase 14)
        List<PublicInformationItem> publicItems = publicInformationItemRepository.findByCrimeCaseIdOrderByCreatedAtDesc(caseId);
        List<CaseReportSummaryDTO.PublicInfoSummaryDTO> publicDTOs = publicItems.stream().map(p -> {
            CaseReportSummaryDTO.PublicInfoSummaryDTO dto = new CaseReportSummaryDTO.PublicInfoSummaryDTO();
            dto.setTitle(p.getTitle());
            dto.setSourceName(p.getSourceName());
            dto.setSourceUrl(p.getSourceUrl());
            dto.setPublishedAt(p.getPublishedAt());
            dto.setVerificationStatus(p.getVerificationStatus() != null ? p.getVerificationStatus().name() : "UNVERIFIED");
            dto.setInvestigatorNotes(p.getNotes());
            return dto;
        }).collect(Collectors.toList());
        report.setPublicInformationItems(publicDTOs);

        // Notification Summary (Phase 13)
        List<Notification> notifList = notificationRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
        int sent = (int) notifList.stream().filter(n -> n.getStatus() != null && "SENT".equalsIgnoreCase(n.getStatus().name())).count();
        int failed = (int) notifList.stream().filter(n -> n.getStatus() != null && "FAILED".equalsIgnoreCase(n.getStatus().name())).count();
        int skipped = (int) notifList.stream().filter(n -> n.getStatus() != null && "SKIPPED".equalsIgnoreCase(n.getStatus().name())).count();
        int email = (int) notifList.stream().filter(n -> n.getChannel() != null && "EMAIL".equalsIgnoreCase(n.getChannel().name())).count();
        int whatsapp = (int) notifList.stream().filter(n -> n.getChannel() != null && "WHATSAPP".equalsIgnoreCase(n.getChannel().name())).count();

        report.setTotalNotificationsSent(sent);
        report.setTotalNotificationsFailed(failed);
        report.setTotalNotificationsSkipped(skipped);
        report.setEmailCount(email);
        report.setWhatsappCount(whatsapp);

        String safeFileName = crimeCase.getCaseNumber().replaceAll("[^a-zA-Z0-9-]", "_") + "-case-report.pdf";
        report.setReportFileName(safeFileName);

        return report;
    }
}
