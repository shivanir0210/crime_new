package com.crimesphere.backend.dto.evidence;

public class UpdateEvidenceStatusRequest {

    private String status;

    private String notes;

    public UpdateEvidenceStatusRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
