package com.crimesphere.backend.dto.case_mgmt;

public class UpdateCaseRequest {

    private String title;
    private String description;
    private String caseType;
    private String priority;
    private String status;

    public UpdateCaseRequest() {
    }

    public UpdateCaseRequest(String title, String description, String caseType, String priority, String status) {
        this.title = title;
        this.description = description;
        this.caseType = caseType;
        this.priority = priority;
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
