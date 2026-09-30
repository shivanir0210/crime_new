package com.crimesphere.backend.dto;

public class LoginResponse {
    private String token;
    private String tokenType = "Bearer";
    private long expiresIn;
    private OfficerDto officer;
    private PoliceStationDto station;

    public LoginResponse() {
    }

    public LoginResponse(String token, long expiresIn, OfficerDto officer, PoliceStationDto station) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.officer = officer;
        this.station = station;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public OfficerDto getOfficer() {
        return officer;
    }

    public void setOfficer(OfficerDto officer) {
        this.officer = officer;
    }

    public PoliceStationDto getStation() {
        return station;
    }

    public void setStation(PoliceStationDto station) {
        this.station = station;
    }
}
