package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.PoliceOfficer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PoliceOfficerRepository extends JpaRepository<PoliceOfficer, UUID> {
    Optional<PoliceOfficer> findByPoliceId(String policeId);
    java.util.List<PoliceOfficer> findByStationId(UUID stationId);
}
