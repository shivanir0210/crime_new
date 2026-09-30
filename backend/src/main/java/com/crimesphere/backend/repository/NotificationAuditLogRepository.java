package com.crimesphere.backend.repository;

import com.crimesphere.backend.entity.NotificationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationAuditLogRepository extends JpaRepository<NotificationAuditLog, UUID> {

    List<NotificationAuditLog> findByNotificationIdOrderByCreatedAtDesc(UUID notificationId);

    List<NotificationAuditLog> findByCaseEntityIdOrderByCreatedAtDesc(UUID caseId);
}
