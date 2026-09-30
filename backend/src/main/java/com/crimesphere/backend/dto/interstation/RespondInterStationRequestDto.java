package com.crimesphere.backend.dto.interstation;

public class RespondInterStationRequestDto {

    private String responseNotes;

    public RespondInterStationRequestDto() {
    }

    public RespondInterStationRequestDto(String responseNotes) {
        this.responseNotes = responseNotes;
    }

    public String getResponseNotes() {
        return responseNotes;
    }

    public void setResponseNotes(String responseNotes) {
        this.responseNotes = responseNotes;
    }
}
