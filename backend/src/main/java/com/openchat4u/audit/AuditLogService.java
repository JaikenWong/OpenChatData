package com.openchat4u.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public Page<AuditLog> findByTenant(String tenantCode, Pageable pageable) {
        return auditLogRepository.findByTenantCodeOrderByCreatedAtDesc(tenantCode, pageable);
    }

    public Page<AuditLog> findByTenantAndAction(String tenantCode, String action, Pageable pageable) {
        return auditLogRepository.findByTenantCodeAndActionOrderByCreatedAtDesc(tenantCode, action, pageable);
    }

    public Page<AuditLog> findByTenantAndDateRange(String tenantCode, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return auditLogRepository.findByTenantCodeAndCreatedAtBetweenOrderByCreatedAtDesc(tenantCode, start, end, pageable);
    }

    public AuditLog save(AuditLog log) {
        return auditLogRepository.save(log);
    }

    public long countByTenant(String tenantCode) {
        return auditLogRepository.countByTenantCode(tenantCode);
    }
}
