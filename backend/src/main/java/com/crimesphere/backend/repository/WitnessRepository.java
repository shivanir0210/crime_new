package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.Witness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WitnessRepository extends JpaRepository<Witness, UUID> {

    List<Witness> findByCaseEntityIdOrderByCreatedAtAsc(UUID caseId);

    Optional<Witness> findByIdAndCaseEntityId(UUID id, UUID caseId);

    long countByCaseEntityId(UUID caseId);

    boolean existsByCaseEntityId(UUID caseId);
}
