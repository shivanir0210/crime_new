package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.PoliceStationDto;
import com.crimesphere.backend.repository.PoliceStationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DatabaseTestController {

    private final PoliceStationRepository policeStationRepository;
    private final com.crimesphere.backend.repository.PoliceOfficerRepository policeOfficerRepository;

    public DatabaseTestController(PoliceStationRepository policeStationRepository,
                                  com.crimesphere.backend.repository.PoliceOfficerRepository policeOfficerRepository) {
        this.policeStationRepository = policeStationRepository;
        this.policeOfficerRepository = policeOfficerRepository;
    }

    @GetMapping("/db/health")
    public ResponseEntity<Map<String, String>> getDatabaseHealth() {
        Map<String, String> response = new HashMap<>();
        try {
            long count = policeStationRepository.count();
            response.put("status", "UP");
            response.put("database", "connected");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("database", "disconnected");
            response.put("error", "Database connection error");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    @GetMapping("/stations")
    public ResponseEntity<List<PoliceStationDto>> getPoliceStations() {
        List<PoliceStationDto> stations = policeStationRepository.findAll()
                .stream()
                .map(PoliceStationDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/db/officers")
    public ResponseEntity<List<Map<String, Object>>> getOfficers() {
        List<Map<String, Object>> officers = policeOfficerRepository.findAll()
                .stream()
                .sorted(java.util.Comparator.comparing(o -> o.getPoliceId() != null ? o.getPoliceId() : ""))
                .map(o -> {
                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("police_id", o.getPoliceId());
                    map.put("full_name", o.getFullName());
                    map.put("email", o.getEmail());
                    map.put("role", o.getRole());
                    map.put("station_id", o.getStation() != null ? o.getStation().getId() : null);
                    map.put("station_name", o.getStation() != null ? o.getStation().getStationName() : null);
                    map.put("is_active", o.getIsActive());
                    map.put("status", o.getStatus());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(officers);
    }
}
