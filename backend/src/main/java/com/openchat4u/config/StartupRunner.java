package com.openchat4u.config;

import com.openchat4u.datasource.DataSourceRegistry;
import com.openchat4u.rbac.RBACService;
import com.openchat4u.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupRunner implements CommandLineRunner {

    private final TenantService tenantService;
    private final DataSourceRegistry dataSourceRegistry;
    private final RBACService rbacService;

    @Override
    public void run(String... args) {
        tenantService.findAll().stream()
            .filter(t -> "ACTIVE".equals(t.getStatus()))
            .forEach(tenant -> {
                try {
                    dataSourceRegistry.register(
                        tenant.getCode(),
                        tenant.getDbType(),
                        tenant.getJdbcUrl(),
                        tenant.getUsername(),
                        tenant.getPassword(),
                        tenant.getMaxConnections() != null ? tenant.getMaxConnections() : 5,
                        tenant.getConnectionTimeout() != null ? tenant.getConnectionTimeout() : 10000
                    );
                    log.info("Registered DataSource for tenant: {}, dbType: {}", tenant.getCode(), tenant.getDbType());
                } catch (Exception e) {
                    log.error("Failed to register DataSource for tenant: {}", tenant.getCode(), e);
                }
            });
    }
}
