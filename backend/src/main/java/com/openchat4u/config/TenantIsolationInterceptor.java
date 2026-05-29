package com.openchat4u.config;

import com.openchat4u.auth.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriTemplate;

import java.util.Map;

/**
 * Validates that the tenantCode in the URL path matches the JWT tenantCode.
 * Applied to all /api/{tenantCode}/... endpoints.
 */
@Component
@Slf4j
public class TenantIsolationInterceptor implements HandlerInterceptor {

    private static final UriTemplate TENANT_TEMPLATE = new UriTemplate("/api/{tenantCode}/**");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String jwtTenant = TenantContext.get();
        if (jwtTenant == null) {
            // No JWT — Spring Security will reject anyway
            return true;
        }

        Map<String, String> pathVars = TENANT_TEMPLATE.match(request.getRequestURI());
        if (pathVars == null || !pathVars.containsKey("tenantCode")) {
            // URL doesn't contain tenantCode path variable (e.g. /api/admin/tenants)
            return true;
        }

        String pathTenant = pathVars.get("tenantCode");
        if (!jwtTenant.equals(pathTenant)) {
            log.warn("Tenant isolation violation: JWT tenant={} but path tenant={}, user={}",
                jwtTenant, pathTenant, request.getUserPrincipal());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Tenant access denied: you can only access your own tenant\"}");
            return false;
        }

        return true;
    }
}