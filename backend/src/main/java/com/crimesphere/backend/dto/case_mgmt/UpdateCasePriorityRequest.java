package com.crimesphere.backend.dto.case_mgmt;

public class UpdateCasePriorityRequest {

    private String priority;

    public UpdateCasePriorityRequest() {
    }

    public UpdateCasePriorityRequest(String priority) {
        this.priority = priority;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
