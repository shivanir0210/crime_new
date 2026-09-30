package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.Notification;
import com.crimesphere.backend.entity.NotificationStatus;
import com.crimesphere.backend.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID>, JpaSpecificationExecutor<Notification> {

    List<Notification> findByCaseEntityIdOrderByCreatedAtDesc(UUID caseId);

    List<Notification> findByRecipientOfficerIdOrderByCreatedAtDesc(UUID officerId);

    List<Notification> findByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
            List<NotificationStatus> statuses, int maxAttempts);

    Optional<Notification> findFirstByCaseEntityIdAndNotificationTypeAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID caseId, NotificationType notificationType, OffsetDateTime after);

    @Query("SELECT n FROM Notification n WHERE " +
           "(n.recipientOfficer.id = :officerId OR " +
           "(n.caseEntity IS NOT NULL AND n.caseEntity.station.id = :stationId)) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findVisibleToOfficer(@Param("officerId") UUID officerId,
                                            @Param("stationId") UUID stationId,
                                            Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE " +
           "(n.recipientOfficer.id = :officerId OR " +
           "(n.caseEntity IS NOT NULL AND n.caseEntity.station.id = :stationId)) " +
           "AND n.status IN ('SENT', 'PENDING')")
    long countActiveForOfficer(@Param("officerId") UUID officerId,
                               @Param("stationId") UUID stationId);

    long countByRecipientOfficerIdAndIsReadFalse(UUID officerId);

    List<Notification> findByRecipientOfficerIdAndIsReadFalseOrderByCreatedAtDesc(UUID officerId);

    Page<Notification> findByRecipientOfficerIdOrderByCreatedAtDesc(UUID officerId, Pageable pageable);

    Page<Notification> findByRecipientOfficerIdAndIsReadFalseOrderByCreatedAtDesc(UUID officerId, Pageable pageable);

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);
}
