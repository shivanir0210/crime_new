package com.crimesphere.backend.dto;

public class LoginRequest {
    private String policeId;
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String policeId, String password) {
        this.policeId = policeId;
        this.password = password;
    }

    public String getPoliceId() {
        return policeId;
    }

    public void setPoliceId(String policeId) {
        this.policeId = policeId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
