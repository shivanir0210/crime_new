package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.EvidenceVerificationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceVerificationItemRepository extends JpaRepository<EvidenceVerificationItem, UUID> {

    List<EvidenceVerificationItem> findByVerificationId(UUID verificationId);
}
