package com.crimesphere.backend.dto.case_mgmt;

public class UpdateCaseStatusRequest {

    private String status;

    public UpdateCaseStatusRequest() {
    }

    public UpdateCaseStatusRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
