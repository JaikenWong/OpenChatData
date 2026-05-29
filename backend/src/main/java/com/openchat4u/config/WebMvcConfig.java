package com.openchat4u.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantIsolationInterceptor tenantIsolationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Interceptor only matches /api/{tenantCode}/... patterns (path-var based).
        // /api/query/ask handles tenant isolation internally via TenantContext check
        // (QueryController.ask() compares JWT tenantCode against TenantContext).
        registry.addInterceptor(tenantIsolationInterceptor)
            .addPathPatterns("/api/schema/*/**", "/api/history/*/**", "/api/audit/*/**",
                             "/api/dictionary/*/**", "/api/masking/*/**");
    }
}
