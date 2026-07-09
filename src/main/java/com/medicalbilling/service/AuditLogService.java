package com.medicalbilling.service;

import com.medicalbilling.entity.AuditLog;
import com.medicalbilling.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public List<AuditLog> getRecent(int limit) {
        return auditLogRepository.findTop50ByOrderByTimestampDesc().stream()
                .limit(limit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getByUsername(String username) {
        return auditLogRepository.findByUsernameOrderByTimestampDesc(username);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getByEntityType(String entityType) {
        return auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByDateRange(start, end);
    }
}
