package com.medicalbilling.repository;

import com.medicalbilling.entity.NotificationLog;
import com.medicalbilling.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findTop50ByOrderBySentAtDesc();
    List<NotificationLog> findByType(NotificationType type);
    boolean existsByReferenceIdAndSentAtAfter(String referenceId, LocalDateTime since);
}
