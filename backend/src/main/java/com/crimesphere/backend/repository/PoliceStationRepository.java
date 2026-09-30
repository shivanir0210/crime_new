package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.PoliceStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PoliceStationRepository extends JpaRepository<PoliceStation, UUID> {
    Optional<PoliceStation> findByStationCode(String stationCode);
    List<PoliceStation> findAllByOrderByStationNameAsc();
}
