package com.crimesphere.backend.dto;

public class AuthMeResponse {
    private String id;
    private String policeId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String stationId;
    private PoliceStationDto station;

    public AuthMeResponse() {
    }

    public AuthMeResponse(String id, String policeId, String fullName, String email, String phone, String role, String stationId, PoliceStationDto station) {
        this.id = id;
        this.policeId = policeId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.stationId = stationId;
        this.station = station;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPoliceId() {
        return policeId;
    }

    public void setPoliceId(String policeId) {
        this.policeId = policeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public PoliceStationDto getStation() {
        return station;
    }

    public void setStation(PoliceStationDto station) {
        this.station = station;
    }
}
