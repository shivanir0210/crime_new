package com.crimesphere.backend.dto;

import com.crimesphere.backend.entity.PoliceOfficer;
import java.util.UUID;

public class OfficerDto {
    private String policeId;
    private String fullName;
    private String role;
    private UUID stationId;

    public OfficerDto() {
    }

    public OfficerDto(PoliceOfficer officer) {
        this.policeId = officer.getPoliceId();
        this.fullName = officer.getFullName();
        this.role = officer.getRole();
        if (officer.getStation() != null) {
            this.stationId = officer.getStation().getId();
        }
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public UUID getStationId() {
        return stationId;
    }

    public void setStationId(UUID stationId) {
        this.stationId = stationId;
    }
}
