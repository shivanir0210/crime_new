package com.crimesphere.backend.dto.location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LocationIntelligenceSummaryDTO {

    private UUID caseId;
    private String caseNumber;
    private String caseTitle;
    private String caseStatus;
    private String casePriority;
    private String caseType;
    private String stationName;
    private String assignedOfficerName;

    private int totalLocations;
    private int totalEvents;
    private double totalDistanceKm;
    private String formattedDistance;
    private int estimatedTravelTimeMinutes;
    private String formattedTravelTime;

    private int liveCount;
    private int lastKnownCount;
    private int historicalCount;
    private int estimatedCount;

    private List<CaseLocationDTO> locations = new ArrayList<>();
    private List<LocationEventDTO> events = new ArrayList<>();

    public LocationIntelligenceSummaryDTO() {
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getCaseTitle() {
        return caseTitle;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

    public String getCaseStatus() {
        return caseStatus;
    }

    public void setCaseStatus(String caseStatus) {
        this.caseStatus = caseStatus;
    }

    public String getCasePriority() {
        return casePriority;
    }

    public void setCasePriority(String casePriority) {
        this.casePriority = casePriority;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getAssignedOfficerName() {
        return assignedOfficerName;
    }

    public void setAssignedOfficerName(String assignedOfficerName) {
        this.assignedOfficerName = assignedOfficerName;
    }

    public int getTotalLocations() {
        return totalLocations;
    }

    public void setTotalLocations(int totalLocations) {
        this.totalLocations = totalLocations;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(int totalEvents) {
        this.totalEvents = totalEvents;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public String getFormattedDistance() {
        return formattedDistance;
    }

    public void setFormattedDistance(String formattedDistance) {
        this.formattedDistance = formattedDistance;
    }

    public int getEstimatedTravelTimeMinutes() {
        return estimatedTravelTimeMinutes;
    }

    public void setEstimatedTravelTimeMinutes(int estimatedTravelTimeMinutes) {
        this.estimatedTravelTimeMinutes = estimatedTravelTimeMinutes;
    }

    public String getFormattedTravelTime() {
        return formattedTravelTime;
    }

    public void setFormattedTravelTime(String formattedTravelTime) {
        this.formattedTravelTime = formattedTravelTime;
    }

    public int getLiveCount() {
        return liveCount;
    }

    public void setLiveCount(int liveCount) {
        this.liveCount = liveCount;
    }

    public int getLastKnownCount() {
        return lastKnownCount;
    }

    public void setLastKnownCount(int lastKnownCount) {
        this.lastKnownCount = lastKnownCount;
    }

    public int getHistoricalCount() {
        return historicalCount;
    }

    public void setHistoricalCount(int historicalCount) {
        this.historicalCount = historicalCount;
    }

    public int getEstimatedCount() {
        return estimatedCount;
    }

    public void setEstimatedCount(int estimatedCount) {
        this.estimatedCount = estimatedCount;
    }

    public List<CaseLocationDTO> getLocations() {
        return locations;
    }

    public void setLocations(List<CaseLocationDTO> locations) {
        this.locations = locations;
    }

    public List<LocationEventDTO> getEvents() {
        return events;
    }

    public void setEvents(List<LocationEventDTO> events) {
        this.events = events;
    }
}
