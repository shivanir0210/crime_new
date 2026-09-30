package com.crimesphere.backend.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CaseReportSummaryDTO {

    // Header & Responsible AI Notice
    private String reportTitle = "INVESTIGATION CASE REPORT";
    private String reportVersion = "CrimeSphere-Case-Report-v1";
    private OffsetDateTime generatedAt;
    private String generatedByOfficerName;
    private String generatedByOfficerBadge;
    private String policeStationName;
    private String policeStationDistrict;
    private String responsibleAiNotice = "CrimeSphere AI is an investigation decision-support system. AI-generated findings are informational and require investigator validation. The system does not independently determine guilt, innocence, or legal liability.";

    // Case Overview
    private UUID caseId;
    private String caseNumber;
    private String caseTitle;
    private String description;
    private String caseType;
    private String priority;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // FIR Section
    private boolean firExists;
    private String firNumber;
    private OffsetDateTime firRegDate;
    private String complainantName;
    private String complainantContact;
    private OffsetDateTime incidentDate;
    private String incidentLocation;
    private String offenceType;
    private String sectionsOfLaw;
    private String firDescription;

    // Victims & Suspects
    private List<VictimSummaryDTO> victims = new ArrayList<>();
    private List<SuspectSummaryDTO> suspects = new ArrayList<>();

    // Evidence Section
    private int totalEvidenceCount;
    private int verifiedEvidenceCount;
    private int underReviewEvidenceCount;
    private int rejectedEvidenceCount;
    private List<EvidenceSummaryDTO> evidenceItems = new ArrayList<>();

    // Evidence Gap Analysis & Investigation State
    private String investigationStateLabel;
    private double completenessPercentage;
    private List<String> completedAreas = new ArrayList<>();
    private List<String> missingAreas = new ArrayList<>();
    private OffsetDateTime stateLastEvaluatedAt;
    private List<EvidenceGapSummaryDTO> evidenceGaps = new ArrayList<>();

    // AI Analysis Section
    private boolean aiAnalysisAvailable;
    private String analysisVersion;
    private String modelName;
    private OffsetDateTime analysisDate;
    private String aiSummary;
    private List<AiItemSummaryDTO> aiFindings = new ArrayList<>();

    // Investigator Reviews (Phase 12)
    private List<InvestigatorReviewSummaryDTO> investigatorReviews = new ArrayList<>();

    // Public Information Section (Phase 14)
    private List<PublicInfoSummaryDTO> publicInformationItems = new ArrayList<>();

    // Notification Summary (Phase 13)
    private int totalNotificationsSent;
    private int totalNotificationsFailed;
    private int totalNotificationsSkipped;
    private int emailCount;
    private int whatsappCount;

    // Report Metadata
    private String reportFileName;

    public CaseReportSummaryDTO() {
    }

    // Getters and Setters

    public String getReportTitle() { return reportTitle; }
    public void setReportTitle(String reportTitle) { this.reportTitle = reportTitle; }

    public String getReportVersion() { return reportVersion; }
    public void setReportVersion(String reportVersion) { this.reportVersion = reportVersion; }

    public OffsetDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(OffsetDateTime generatedAt) { this.generatedAt = generatedAt; }

    public String getGeneratedByOfficerName() { return generatedByOfficerName; }
    public void setGeneratedByOfficerName(String generatedByOfficerName) { this.generatedByOfficerName = generatedByOfficerName; }

    public String getGeneratedByOfficerBadge() { return generatedByOfficerBadge; }
    public void setGeneratedByOfficerBadge(String generatedByOfficerBadge) { this.generatedByOfficerBadge = generatedByOfficerBadge; }

    public String getPoliceStationName() { return policeStationName; }
    public void setPoliceStationName(String policeStationName) { this.policeStationName = policeStationName; }

    public String getPoliceStationDistrict() { return policeStationDistrict; }
    public void setPoliceStationDistrict(String policeStationDistrict) { this.policeStationDistrict = policeStationDistrict; }

    public String getResponsibleAiNotice() { return responsibleAiNotice; }
    public void setResponsibleAiNotice(String responsibleAiNotice) { this.responsibleAiNotice = responsibleAiNotice; }

    public UUID getCaseId() { return caseId; }
    public void setCaseId(UUID caseId) { this.caseId = caseId; }

    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }

    public String getCaseTitle() { return caseTitle; }
    public void setCaseTitle(String caseTitle) { this.caseTitle = caseTitle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCaseType() { return caseType; }
    public void setCaseType(String caseType) { this.caseType = caseType; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isFirExists() { return firExists; }
    public void setFirExists(boolean firExists) { this.firExists = firExists; }

    public String getFirNumber() { return firNumber; }
    public void setFirNumber(String firNumber) { this.firNumber = firNumber; }

    public OffsetDateTime getFirRegDate() { return firRegDate; }
    public void setFirRegDate(OffsetDateTime firRegDate) { this.firRegDate = firRegDate; }

    public String getComplainantName() { return complainantName; }
    public void setComplainantName(String complainantName) { this.complainantName = complainantName; }

    public String getComplainantContact() { return complainantContact; }
    public void setComplainantContact(String complainantContact) { this.complainantContact = complainantContact; }

    public OffsetDateTime getIncidentDate() { return incidentDate; }
    public void setIncidentDate(OffsetDateTime incidentDate) { this.incidentDate = incidentDate; }

    public String getIncidentLocation() { return incidentLocation; }
    public void setIncidentLocation(String incidentLocation) { this.incidentLocation = incidentLocation; }

    public String getOffenceType() { return offenceType; }
    public void setOffenceType(String offenceType) { this.offenceType = offenceType; }

    public String getSectionsOfLaw() { return sectionsOfLaw; }
    public void setSectionsOfLaw(String sectionsOfLaw) { this.sectionsOfLaw = sectionsOfLaw; }

    public String getFirDescription() { return firDescription; }
    public void setFirDescription(String firDescription) { this.firDescription = firDescription; }

    public List<VictimSummaryDTO> getVictims() { return victims; }
    public void setVictims(List<VictimSummaryDTO> victims) { this.victims = victims; }

    public List<SuspectSummaryDTO> getSuspects() { return suspects; }
    public void setSuspects(List<SuspectSummaryDTO> suspects) { this.suspects = suspects; }

    public int getTotalEvidenceCount() { return totalEvidenceCount; }
    public void setTotalEvidenceCount(int totalEvidenceCount) { this.totalEvidenceCount = totalEvidenceCount; }

    public int getVerifiedEvidenceCount() { return verifiedEvidenceCount; }
    public void setVerifiedEvidenceCount(int verifiedEvidenceCount) { this.verifiedEvidenceCount = verifiedEvidenceCount; }

    public int getUnderReviewEvidenceCount() { return underReviewEvidenceCount; }
    public void setUnderReviewEvidenceCount(int underReviewEvidenceCount) { this.underReviewEvidenceCount = underReviewEvidenceCount; }

    public int getRejectedEvidenceCount() { return rejectedEvidenceCount; }
    public void setRejectedEvidenceCount(int rejectedEvidenceCount) { this.rejectedEvidenceCount = rejectedEvidenceCount; }

    public List<EvidenceSummaryDTO> getEvidenceItems() { return evidenceItems; }
    public void setEvidenceItems(List<EvidenceSummaryDTO> evidenceItems) { this.evidenceItems = evidenceItems; }

    public String getInvestigationStateLabel() { return investigationStateLabel; }
    public void setInvestigationStateLabel(String investigationStateLabel) { this.investigationStateLabel = investigationStateLabel; }

    public double getCompletenessPercentage() { return completenessPercentage; }
    public void setCompletenessPercentage(double completenessPercentage) { this.completenessPercentage = completenessPercentage; }

    public List<String> getCompletedAreas() { return completedAreas; }
    public void setCompletedAreas(List<String> completedAreas) { this.completedAreas = completedAreas; }

    public List<String> getMissingAreas() { return missingAreas; }
    public void setMissingAreas(List<String> missingAreas) { this.missingAreas = missingAreas; }

    public OffsetDateTime getStateLastEvaluatedAt() { return stateLastEvaluatedAt; }
    public void setStateLastEvaluatedAt(OffsetDateTime stateLastEvaluatedAt) { this.stateLastEvaluatedAt = stateLastEvaluatedAt; }

    public List<EvidenceGapSummaryDTO> getEvidenceGaps() { return evidenceGaps; }
    public void setEvidenceGaps(List<EvidenceGapSummaryDTO> evidenceGaps) { this.evidenceGaps = evidenceGaps; }

    public boolean isAiAnalysisAvailable() { return aiAnalysisAvailable; }
    public void setAiAnalysisAvailable(boolean aiAnalysisAvailable) { this.aiAnalysisAvailable = aiAnalysisAvailable; }

    public String getAnalysisVersion() { return analysisVersion; }
    public void setAnalysisVersion(String analysisVersion) { this.analysisVersion = analysisVersion; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public OffsetDateTime getAnalysisDate() { return analysisDate; }
    public void setAnalysisDate(OffsetDateTime analysisDate) { this.analysisDate = analysisDate; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public List<AiItemSummaryDTO> getAiFindings() { return aiFindings; }
    public void setAiFindings(List<AiItemSummaryDTO> aiFindings) { this.aiFindings = aiFindings; }

    public List<InvestigatorReviewSummaryDTO> getInvestigatorReviews() { return investigatorReviews; }
    public void setInvestigatorReviews(List<InvestigatorReviewSummaryDTO> investigatorReviews) { this.investigatorReviews = investigatorReviews; }

    public List<PublicInfoSummaryDTO> getPublicInformationItems() { return publicInformationItems; }
    public void setPublicInformationItems(List<PublicInfoSummaryDTO> publicInformationItems) { this.publicInformationItems = publicInformationItems; }

    public int getTotalNotificationsSent() { return totalNotificationsSent; }
    public void setTotalNotificationsSent(int totalNotificationsSent) { this.totalNotificationsSent = totalNotificationsSent; }

    public int getTotalNotificationsFailed() { return totalNotificationsFailed; }
    public void setTotalNotificationsFailed(int totalNotificationsFailed) { this.totalNotificationsFailed = totalNotificationsFailed; }

    public int getTotalNotificationsSkipped() { return totalNotificationsSkipped; }
    public void setTotalNotificationsSkipped(int totalNotificationsSkipped) { this.totalNotificationsSkipped = totalNotificationsSkipped; }

    public int getEmailCount() { return emailCount; }
    public void setEmailCount(int emailCount) { this.emailCount = emailCount; }

    public int getWhatsappCount() { return whatsappCount; }
    public void setWhatsappCount(int whatsappCount) { this.whatsappCount = whatsappCount; }

    public String getReportFileName() { return reportFileName; }
    public void setReportFileName(String reportFileName) { this.reportFileName = reportFileName; }

    // Nested DTO Classes

    public static class VictimSummaryDTO {
        private String fullName;
        private Integer age;
        private String gender;
        private OffsetDateTime dob;
        private String phone;
        private String address;
        private String occupation;
        private String relationshipToCase;
        private String status;
        private String description;

        public VictimSummaryDTO() {}

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public OffsetDateTime getDob() { return dob; }
        public void setDob(OffsetDateTime dob) { this.dob = dob; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getOccupation() { return occupation; }
        public void setOccupation(String occupation) { this.occupation = occupation; }
        public String getRelationshipToCase() { return relationshipToCase; }
        public void setRelationshipToCase(String relationshipToCase) { this.relationshipToCase = relationshipToCase; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class SuspectSummaryDTO {
        private String fullName;
        private String alias;
        private Integer age;
        private String gender;
        private String phone;
        private String address;
        private String occupation;
        private String investigationStatus;
        private String description;

        public SuspectSummaryDTO() {}

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getAlias() { return alias; }
        public void setAlias(String alias) { this.alias = alias; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getOccupation() { return occupation; }
        public void setOccupation(String occupation) { this.occupation = occupation; }
        public String getInvestigationStatus() { return investigationStatus; }
        public void setInvestigationStatus(String investigationStatus) { this.investigationStatus = investigationStatus; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class EvidenceSummaryDTO {
        private String evidenceNumber;
        private String title;
        private String description;
        private String evidenceType;
        private String status;
        private String originalFileName;
        private String mimeType;
        private Long fileSizeBytes;
        private String fileHash;
        private OffsetDateTime collectedAt;
        private String collectedLocation;
        private String collectedBy;
        private String notes;

        public EvidenceSummaryDTO() {}

        public String getEvidenceNumber() { return evidenceNumber; }
        public void setEvidenceNumber(String evidenceNumber) { this.evidenceNumber = evidenceNumber; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getEvidenceType() { return evidenceType; }
        public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getOriginalFileName() { return originalFileName; }
        public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public Long getFileSizeBytes() { return fileSizeBytes; }
        public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
        public String getFileHash() { return fileHash; }
        public void setFileHash(String fileHash) { this.fileHash = fileHash; }
        public OffsetDateTime getCollectedAt() { return collectedAt; }
        public void setCollectedAt(OffsetDateTime collectedAt) { this.collectedAt = collectedAt; }
        public String getCollectedLocation() { return collectedLocation; }
        public void setCollectedLocation(String collectedLocation) { this.collectedLocation = collectedLocation; }
        public String getCollectedBy() { return collectedBy; }
        public void setCollectedBy(String collectedBy) { this.collectedBy = collectedBy; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class EvidenceGapSummaryDTO {
        private String category;
        private String title;
        private String description;
        private String severity;
        private String status;
        private String reason;

        public EvidenceGapSummaryDTO() {}

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class AiItemSummaryDTO {
        private String category; // OBSERVATION, POTENTIAL_LEAD, EVIDENCE_GAP, CONTRADICTION, RECOMMENDED_ACTION, UNCERTAINTY
        private String title;
        private String content;
        private Double confidenceScore;
        private List<String> evidenceReferences = new ArrayList<>();
        private String verificationStatus; // SUPPORTED, PARTIALLY_SUPPORTED, UNSUPPORTED, UNVERIFIABLE, REFERENCE_INVALID
        private String supportLevel; // STRONG, MODERATE, WEAK, NONE
        private String verificationSummary;
        private String verificationMethod;
        private OffsetDateTime verifiedAt;

        public AiItemSummaryDTO() {}

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
        public List<String> getEvidenceReferences() { return evidenceReferences; }
        public void setEvidenceReferences(List<String> evidenceReferences) { this.evidenceReferences = evidenceReferences; }
        public String getVerificationStatus() { return verificationStatus; }
        public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
        public String getSupportLevel() { return supportLevel; }
        public void setSupportLevel(String supportLevel) { this.supportLevel = supportLevel; }
        public String getVerificationSummary() { return verificationSummary; }
        public void setVerificationSummary(String verificationSummary) { this.verificationSummary = verificationSummary; }
        public String getVerificationMethod() { return verificationMethod; }
        public void setVerificationMethod(String verificationMethod) { this.verificationMethod = verificationMethod; }
        public OffsetDateTime getVerifiedAt() { return verifiedAt; }
        public void setVerifiedAt(OffsetDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    }

    public static class InvestigatorReviewSummaryDTO {
        private String itemTitle;
        private String category;
        private String originalAiContent;
        private String verificationStatusAtReview;
        private String supportLevelAtReview;
        private String investigatorDecision; // APPROVED, EDITED, REJECTED
        private String reviewedContent;
        private String reviewComment;
        private String reviewerName;
        private OffsetDateTime reviewDate;

        public InvestigatorReviewSummaryDTO() {}

        public String getItemTitle() { return itemTitle; }
        public void setItemTitle(String itemTitle) { this.itemTitle = itemTitle; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getOriginalAiContent() { return originalAiContent; }
        public void setOriginalAiContent(String originalAiContent) { this.originalAiContent = originalAiContent; }
        public String getVerificationStatusAtReview() { return verificationStatusAtReview; }
        public void setVerificationStatusAtReview(String verificationStatusAtReview) { this.verificationStatusAtReview = verificationStatusAtReview; }
        public String getSupportLevelAtReview() { return supportLevelAtReview; }
        public void setSupportLevelAtReview(String supportLevelAtReview) { this.supportLevelAtReview = supportLevelAtReview; }
        public String getInvestigatorDecision() { return investigatorDecision; }
        public void setInvestigatorDecision(String investigatorDecision) { this.investigatorDecision = investigatorDecision; }
        public String getReviewedContent() { return reviewedContent; }
        public void setReviewedContent(String reviewedContent) { this.reviewedContent = reviewedContent; }
        public String getReviewComment() { return reviewComment; }
        public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
        public String getReviewerName() { return reviewerName; }
        public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
        public OffsetDateTime getReviewDate() { return reviewDate; }
        public void setReviewDate(OffsetDateTime reviewDate) { this.reviewDate = reviewDate; }
    }

    public static class PublicInfoSummaryDTO {
        private String title;
        private String sourceName;
        private String sourceUrl;
        private OffsetDateTime publishedAt;
        private String verificationStatus;
        private String investigatorNotes;

        public PublicInfoSummaryDTO() {}

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSourceName() { return sourceName; }
        public void setSourceName(String sourceName) { this.sourceName = sourceName; }
        public String getSourceUrl() { return sourceUrl; }
        public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
        public OffsetDateTime getPublishedAt() { return publishedAt; }
        public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
        public String getVerificationStatus() { return verificationStatus; }
        public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
        public String getInvestigatorNotes() { return investigatorNotes; }
        public void setInvestigatorNotes(String investigatorNotes) { this.investigatorNotes = investigatorNotes; }
    }
}
