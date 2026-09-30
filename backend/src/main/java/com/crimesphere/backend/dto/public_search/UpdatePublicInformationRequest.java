package com.crimesphere.backend.dto.public_search;

import com.crimesphere.backend.entity.PublicInformationVerificationStatus;

public class UpdatePublicInformationRequest {

    private PublicInformationVerificationStatus verificationStatus;
    private String notes;

    public UpdatePublicInformationRequest() {
    }

    public UpdatePublicInformationRequest(PublicInformationVerificationStatus verificationStatus, String notes) {
        this.verificationStatus = verificationStatus;
        this.notes = notes;
    }

    public PublicInformationVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(PublicInformationVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
