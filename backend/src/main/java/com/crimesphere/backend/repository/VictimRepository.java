package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.Victim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VictimRepository extends JpaRepository<Victim, UUID> {

    List<Victim> findByCaseEntityIdOrderByCreatedAtAsc(UUID caseId);

    Optional<Victim> findByIdAndCaseEntityId(UUID id, UUID caseId);

    long countByCaseEntityId(UUID caseId);

    boolean existsByCaseEntityId(UUID caseId);
}
