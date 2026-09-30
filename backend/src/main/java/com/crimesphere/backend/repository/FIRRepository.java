package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.FIR;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FIRRepository extends JpaRepository<FIR, UUID> {

    Optional<FIR> findByCaseEntityId(UUID caseId);

    Optional<FIR> findByFirNumber(String firNumber);

    boolean existsByCaseEntityId(UUID caseId);

    @Query(value = "SELECT nextval('fir_number_seq')", nativeQuery = true)
    Long getNextFirNumberSequence();
}
