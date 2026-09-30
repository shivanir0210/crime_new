package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.InterStationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterStationRequestRepository extends JpaRepository<InterStationRequest, UUID> {

    List<InterStationRequest> findBySourceStationIdOrderByCreatedAtDesc(UUID sourceStationId);

    List<InterStationRequest> findByDestinationStationIdOrderByCreatedAtDesc(UUID destinationStationId);

    List<InterStationRequest> findByDestinationStationIdAndStatusOrderByCreatedAtDesc(UUID destinationStationId, String status);

    List<InterStationRequest> findByCaseEntityIdOrderByCreatedAtDesc(UUID caseId);
}
