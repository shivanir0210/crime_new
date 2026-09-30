package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.NotificationDelivery;
import com.crimesphere.backend.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    List<NotificationDelivery> findByCaseEntityIdOrderByCreatedAtDesc(UUID caseId);

    List<NotificationDelivery> findByRecipientOfficerIdOrderByCreatedAtDesc(UUID recipientOfficerId);

    Optional<NotificationDelivery> findByIdempotencyKey(String idempotencyKey);

    List<NotificationDelivery> findByStatus(NotificationStatus status);
}
