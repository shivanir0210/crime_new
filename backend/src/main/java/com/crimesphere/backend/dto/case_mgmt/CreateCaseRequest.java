package com.crimesphere.backend.dto.case_mgmt;

public class CreateCaseRequest {

    private String title;
    private String description;
    private String caseType;
    private String priority;

    public CreateCaseRequest() {
    }

    public CreateCaseRequest(String title, String description, String caseType, String priority) {
        this.title = title;
        this.description = description;
        this.caseType = caseType;
        this.priority = priority;
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
}
