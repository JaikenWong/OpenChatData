package com.openchat4u.api;

import com.openchat4u.audit.AuditLog;
import com.openchat4u.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogService auditLogService;

    @GetMapping("/{tenantCode}")
    public Page<AuditLog> listLogs(
            @PathVariable String tenantCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            return auditLogService.findByTenantAndDateRange(tenantCode, start, end, pageRequest);
        }
        if (action != null && !action.trim().isEmpty()) {
            return auditLogService.findByTenantAndAction(tenantCode, action, pageRequest);
        }
        return auditLogService.findByTenant(tenantCode, pageRequest);
    }

    @GetMapping("/{tenantCode}/stats")
    public Map<String, Object> getStats(@PathVariable String tenantCode) {
        return Map.of(
            "totalLogs", auditLogService.countByTenant(tenantCode)
        );
    }
}
