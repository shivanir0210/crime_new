package com.crimesphere.backend.dto;

import com.crimesphere.backend.entity.PoliceStation;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PoliceStationDto {
    private UUID id;
    private String stationCode;
    private String stationName;
    private String district;
    private String state;
    private String headName;
    private String headEmail;

    public PoliceStationDto() {
    }

    public PoliceStationDto(PoliceStation station) {
        this(station, true);
    }

    public PoliceStationDto(PoliceStation station, boolean includeHeadEmail) {
        if (station != null) {
            this.id = station.getId();
            this.stationCode = station.getStationCode();
            this.stationName = station.getStationName();
            this.district = station.getDistrict();
            this.state = station.getState();
            this.headName = station.getHeadName();
            if (includeHeadEmail) {
                this.headEmail = station.getHeadEmail();
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getHeadName() {
        return headName;
    }

    public void setHeadName(String headName) {
        this.headName = headName;
    }

    public String getHeadEmail() {
        return headEmail;
    }

    public void setHeadEmail(String headEmail) {
        this.headEmail = headEmail;
    }
}
