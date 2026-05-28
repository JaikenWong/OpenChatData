package com.openchat4u.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public IPage<AuditLog> findByTenant(String tenantCode, long page, long size) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<AuditLog>()
            .eq(AuditLog::getTenantCode, tenantCode)
            .orderByDesc(AuditLog::getCreatedAt);
        return auditLogRepository.selectPage(new Page<>(page, size), wrapper);
    }

    public IPage<AuditLog> findByTenantAndAction(String tenantCode, String action, long page, long size) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<AuditLog>()
            .eq(AuditLog::getTenantCode, tenantCode)
            .eq(AuditLog::getAction, action)
            .orderByDesc(AuditLog::getCreatedAt);
        return auditLogRepository.selectPage(new Page<>(page, size), wrapper);
    }

    public IPage<AuditLog> findByTenantAndDateRange(String tenantCode, LocalDateTime start, LocalDateTime end, long page, long size) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<AuditLog>()
            .eq(AuditLog::getTenantCode, tenantCode)
            .between(AuditLog::getCreatedAt, start, end)
            .orderByDesc(AuditLog::getCreatedAt);
        return auditLogRepository.selectPage(new Page<>(page, size), wrapper);
    }

    public AuditLog save(AuditLog log) {
        auditLogRepository.insert(log);
        return log;
    }

    public long countByTenant(String tenantCode) {
        return auditLogRepository.selectCount(
            new LambdaQueryWrapper<AuditLog>().eq(AuditLog::getTenantCode, tenantCode)
        );
    }
}
