package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.PublicInformationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PublicInformationItemRepository extends JpaRepository<PublicInformationItem, UUID> {

    List<PublicInformationItem> findByCrimeCaseIdOrderByCreatedAtDesc(UUID caseId);

    Optional<PublicInformationItem> findByIdAndCrimeCaseId(UUID id, UUID caseId);

    Optional<PublicInformationItem> findByCrimeCaseIdAndSourceUrl(UUID caseId, String sourceUrl);

    boolean existsByCrimeCaseIdAndSourceUrl(UUID caseId, String sourceUrl);
}
