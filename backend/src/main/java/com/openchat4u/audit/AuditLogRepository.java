package com.openchat4u.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByTenantCodeOrderByCreatedAtDesc(String tenantCode, Pageable pageable);
    Page<AuditLog> findByTenantCodeAndActionOrderByCreatedAtDesc(String tenantCode, String action, Pageable pageable);
    Page<AuditLog> findByTenantCodeAndCreatedAtBetweenOrderByCreatedAtDesc(String tenantCode, LocalDateTime start, LocalDateTime end, Pageable pageable);
    long countByTenantCode(String tenantCode);
}
