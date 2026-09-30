package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.PoliceStationDto;
import com.crimesphere.backend.repository.PoliceStationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PoliceStationController {

    private final PoliceStationRepository policeStationRepository;

    public PoliceStationController(PoliceStationRepository policeStationRepository) {
        this.policeStationRepository = policeStationRepository;
    }

    @GetMapping("/police-stations")
    public ResponseEntity<List<PoliceStationDto>> getAllPoliceStations() {
        List<PoliceStationDto> stations = policeStationRepository.findAllByOrderByStationNameAsc()
                .stream()
                .map(station -> new PoliceStationDto(station, false))
                .collect(Collectors.toList());
        return ResponseEntity.ok(stations);
    }
}
