package com.crimesphere.backend.dto;

public class OcrComparisonResultDto {

    private String ocrStatus; // PASSED, REVIEW_REQUIRED, FAILED, PENDING
    private String detectedName;
    private String detectedPoliceId;
    private String detectedStation;
    private String detectedRole;

    private boolean nameMatches;
    private boolean policeIdMatches;
    private boolean stationMatches;
    private boolean roleMatches;

    private String summaryMessage;

    public OcrComparisonResultDto() {
    }

    public OcrComparisonResultDto(String ocrStatus, String detectedName, String detectedPoliceId,
                                  String detectedStation, String detectedRole, boolean nameMatches,
                                  boolean policeIdMatches, boolean stationMatches, boolean roleMatches,
                                  String summaryMessage) {
        this.ocrStatus = ocrStatus;
        this.detectedName = detectedName;
        this.detectedPoliceId = detectedPoliceId;
        this.detectedStation = detectedStation;
        this.detectedRole = detectedRole;
        this.nameMatches = nameMatches;
        this.policeIdMatches = policeIdMatches;
        this.stationMatches = stationMatches;
        this.roleMatches = roleMatches;
        this.summaryMessage = summaryMessage;
    }

    public String getOcrStatus() {
        return ocrStatus;
    }

    public void setOcrStatus(String ocrStatus) {
        this.ocrStatus = ocrStatus;
    }

    public String getDetectedName() {
        return detectedName;
    }

    public void setDetectedName(String detectedName) {
        this.detectedName = detectedName;
    }

    public String getDetectedPoliceId() {
        return detectedPoliceId;
    }

    public void setDetectedPoliceId(String detectedPoliceId) {
        this.detectedPoliceId = detectedPoliceId;
    }

    public String getDetectedStation() {
        return detectedStation;
    }

    public void setDetectedStation(String detectedStation) {
        this.detectedStation = detectedStation;
    }

    public String getDetectedRole() {
        return detectedRole;
    }

    public void setDetectedRole(String detectedRole) {
        this.detectedRole = detectedRole;
    }

    public boolean isNameMatches() {
        return nameMatches;
    }

    public void setNameMatches(boolean nameMatches) {
        this.nameMatches = nameMatches;
    }

    public boolean isPoliceIdMatches() {
        return policeIdMatches;
    }

    public void setPoliceIdMatches(boolean policeIdMatches) {
        this.policeIdMatches = policeIdMatches;
    }

    public boolean isStationMatches() {
        return stationMatches;
    }

    public void setStationMatches(boolean stationMatches) {
        this.stationMatches = stationMatches;
    }

    public boolean isRoleMatches() {
        return roleMatches;
    }

    public void setRoleMatches(boolean roleMatches) {
        this.roleMatches = roleMatches;
    }

    public String getSummaryMessage() {
        return summaryMessage;
    }

    public void setSummaryMessage(String summaryMessage) {
        this.summaryMessage = summaryMessage;
    }
}
