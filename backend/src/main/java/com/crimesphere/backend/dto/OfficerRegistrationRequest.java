package com.crimesphere.backend.dto;

import java.util.UUID;

public class OfficerRegistrationRequest {

    private String fullName;
    private String policeId;
    private String email;
    private String phone;
    private String password;
    private String confirmPassword;
    private UUID stationId;
    private String role = "OFFICER";

    public OfficerRegistrationRequest() {
    }

    public OfficerRegistrationRequest(String fullName, String policeId, String email, String phone,
                                    String password, String confirmPassword, UUID stationId, String role) {
        this.fullName = fullName;
        this.policeId = policeId;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.stationId = stationId;
        this.role = role != null && !role.isBlank() ? role : "OFFICER";
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPoliceId() {
        return policeId;
    }

    public void setPoliceId(String policeId) {
        this.policeId = policeId;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public UUID getStationId() {
        return stationId;
    }

    public void setStationId(UUID stationId) {
        this.stationId = stationId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
