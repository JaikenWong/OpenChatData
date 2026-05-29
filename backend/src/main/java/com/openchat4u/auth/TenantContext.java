package com.openchat4u.auth;

/**
 * ThreadLocal holder for the current tenant code extracted from JWT.
 * Set by JwtAuthenticationFilter, cleared after request.
 */
public final class TenantContext {

    private TenantContext() {}

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public static void set(String tenantCode) {
        CURRENT.set(tenantCode);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
