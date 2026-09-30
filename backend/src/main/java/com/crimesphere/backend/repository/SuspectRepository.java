package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.Suspect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SuspectRepository extends JpaRepository<Suspect, UUID> {

    List<Suspect> findByCaseEntityIdOrderByCreatedAtAsc(UUID caseId);

    Optional<Suspect> findByIdAndCaseEntityId(UUID id, UUID caseId);

    long countByCaseEntityId(UUID caseId);

    boolean existsByCaseEntityId(UUID caseId);
}
