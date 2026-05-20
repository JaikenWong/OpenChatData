package com.openchat4u.config;

import com.openchat4u.audit.AuditLog;
import com.openchat4u.audit.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditInterceptor implements HandlerInterceptor {

    private final AuditLogService auditLogService;
    private static final String START_TIME_ATTR = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        String path = request.getRequestURI();
        
        if (path.startsWith("/api/")) {
            try {
                Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
                long executionTime = startTime != null ? System.currentTimeMillis() - startTime : 0;

                AuditLog auditLog = new AuditLog();
                auditLog.setTenantCode(extractTenantCode(request));
                auditLog.setAction(extractAction(request));
                auditLog.setDescription(request.getMethod() + " " + path);
                auditLog.setUserIp(getClientIp(request));
                auditLog.setRequestMethod(request.getMethod());
                auditLog.setRequestPath(path);
                auditLog.setResponseStatus(response.getStatus());
                auditLog.setExecutionTimeMs(executionTime);

                auditLogService.save(auditLog);
            } catch (Exception e) {
                log.error("Failed to create audit log", e);
            }
        }
    }

    private String extractTenantCode(HttpServletRequest request) {
        String path = request.getRequestURI();
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("api") && i + 2 < parts.length) {
                String nextPart = parts[i + 1];
                if (!nextPart.equals("admin") && !nextPart.equals("auth") && !nextPart.equals("health")) {
                    return nextPart;
                }
            }
        }
        return "system";
    }

    private String extractAction(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.contains("/query/")) return "QUERY";
        if (path.contains("/schema/")) return "SCHEMA";
        if (path.contains("/dictionary/")) return "DICTIONARY";
        if (path.contains("/masking/")) return "MASKING";
        if (path.contains("/history/")) return "HISTORY";
        if (path.contains("/audit/")) return "AUDIT";
        if (path.contains("/auth/")) return "AUTH";
        if (path.contains("/admin/")) return "ADMIN";
        return "OTHER";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
