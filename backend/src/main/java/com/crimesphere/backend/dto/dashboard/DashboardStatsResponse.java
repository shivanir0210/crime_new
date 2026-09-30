package com.crimesphere.backend.dto.dashboard;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DashboardStatsResponse {

    private long activeCases;
    private long highPriorityCases;
    private long criticalCases;
    private long pendingReview;
    private long onHoldCases;
    private long totalCases;
    private String stationName;
    private String stationCode;

    // Case Workload Breakdown
    private long underInvestigationCases;
    private long filedCases;
    private long closedCases;

    // Evidence Overview Breakdown
    private long totalEvidence;
    private long verifiedEvidence;
    private long pendingEvidence;
    private long evidenceGaps;

    // Recent Station Activity
    private List<RecentActivityDto> recentActivities = new ArrayList<>();

    // Cases Requiring Attention
    private List<AttentionCaseDto> attentionCases = new ArrayList<>();

    public DashboardStatsResponse() {
    }

    public DashboardStatsResponse(long activeCases, long highPriorityCases, long criticalCases, long pendingReview, long onHoldCases, long totalCases, String stationName, String stationCode) {
        this.activeCases = activeCases;
        this.highPriorityCases = highPriorityCases;
        this.criticalCases = criticalCases;
        this.pendingReview = pendingReview;
        this.onHoldCases = onHoldCases;
        this.totalCases = totalCases;
        this.stationName = stationName;
        this.stationCode = stationCode;
    }

    public long getActiveCases() {
        return activeCases;
    }

    public void setActiveCases(long activeCases) {
        this.activeCases = activeCases;
    }

    public long getHighPriorityCases() {
        return highPriorityCases;
    }

    public void setHighPriorityCases(long highPriorityCases) {
        this.highPriorityCases = highPriorityCases;
    }

    public long getCriticalCases() {
        return criticalCases;
    }

    public void setCriticalCases(long criticalCases) {
        this.criticalCases = criticalCases;
    }

    public long getPendingReview() {
        return pendingReview;
    }

    public void setPendingReview(long pendingReview) {
        this.pendingReview = pendingReview;
    }

    public long getOnHoldCases() {
        return onHoldCases;
    }

    public void setOnHoldCases(long onHoldCases) {
        this.onHoldCases = onHoldCases;
    }

    public long getTotalCases() {
        return totalCases;
    }

    public void setTotalCases(long totalCases) {
        this.totalCases = totalCases;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public long getUnderInvestigationCases() {
        return underInvestigationCases;
    }

    public void setUnderInvestigationCases(long underInvestigationCases) {
        this.underInvestigationCases = underInvestigationCases;
    }

    public long getFiledCases() {
        return filedCases;
    }

    public void setFiledCases(long filedCases) {
        this.filedCases = filedCases;
    }

    public long getClosedCases() {
        return closedCases;
    }

    public void setClosedCases(long closedCases) {
        this.closedCases = closedCases;
    }

    public long getTotalEvidence() {
        return totalEvidence;
    }

    public void setTotalEvidence(long totalEvidence) {
        this.totalEvidence = totalEvidence;
    }

    public long getVerifiedEvidence() {
        return verifiedEvidence;
    }

    public void setVerifiedEvidence(long verifiedEvidence) {
        this.verifiedEvidence = verifiedEvidence;
    }

    public long getPendingEvidence() {
        return pendingEvidence;
    }

    public void setPendingEvidence(long pendingEvidence) {
        this.pendingEvidence = pendingEvidence;
    }

    public long getEvidenceGaps() {
        return evidenceGaps;
    }

    public void setEvidenceGaps(long evidenceGaps) {
        this.evidenceGaps = evidenceGaps;
    }

    public List<RecentActivityDto> getRecentActivities() {
        return recentActivities;
    }

    public void setRecentActivities(List<RecentActivityDto> recentActivities) {
        this.recentActivities = recentActivities;
    }

    public List<AttentionCaseDto> getAttentionCases() {
        return attentionCases;
    }

    public void setAttentionCases(List<AttentionCaseDto> attentionCases) {
        this.attentionCases = attentionCases;
    }

    public static class RecentActivityDto {
        private UUID id;
        private String action;
        private String actorName;
        private String actorBadge;
        private String details;
        private String status;
        private OffsetDateTime createdAt;

        public RecentActivityDto() {}

        public RecentActivityDto(UUID id, String action, String actorName, String actorBadge, String details, String status, OffsetDateTime createdAt) {
            this.id = id;
            this.action = action;
            this.actorName = actorName;
            this.actorBadge = actorBadge;
            this.details = details;
            this.status = status;
            this.createdAt = createdAt;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getActorName() { return actorName; }
        public void setActorName(String actorName) { this.actorName = actorName; }

        public String getActorBadge() { return actorBadge; }
        public void setActorBadge(String actorBadge) { this.actorBadge = actorBadge; }

        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class AttentionCaseDto {
        private UUID id;
        private String caseNumber;
        private String title;
        private String priority;
        private String status;
        private String reason;
        private OffsetDateTime updatedAt;

        public AttentionCaseDto() {}

        public AttentionCaseDto(UUID id, String caseNumber, String title, String priority, String status, String reason, OffsetDateTime updatedAt) {
            this.id = id;
            this.caseNumber = caseNumber;
            this.title = title;
            this.priority = priority;
            this.status = status;
            this.reason = reason;
            this.updatedAt = updatedAt;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public String getCaseNumber() { return caseNumber; }
        public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
