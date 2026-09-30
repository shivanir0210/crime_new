package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.PublicSearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PublicSearchHistoryRepository extends JpaRepository<PublicSearchHistory, UUID> {

    Page<PublicSearchHistory> findByCrimeCaseIdOrderByCreatedAtDesc(UUID caseId, Pageable pageable);

    Page<PublicSearchHistory> findBySearchedByIdOrderByCreatedAtDesc(UUID searchedById, Pageable pageable);
}
