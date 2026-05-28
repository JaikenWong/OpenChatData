package com.openchat4u.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.openchat4u.audit.AuditLog;
import com.openchat4u.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogService auditLogService;

    @GetMapping("/{tenantCode}")
    public IPage<AuditLog> listLogs(
            @PathVariable String tenantCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            return auditLogService.findByTenantAndDateRange(tenantCode, start, end, page, size);
        }
        if (action != null && !action.trim().isEmpty()) {
            return auditLogService.findByTenantAndAction(tenantCode, action, page, size);
        }
        return auditLogService.findByTenant(tenantCode, page, size);
    }

    @GetMapping("/{tenantCode}/stats")
    public Map<String, Object> getStats(@PathVariable String tenantCode) {
        return Map.of(
            "totalLogs", auditLogService.countByTenant(tenantCode)
        );
    }
}
