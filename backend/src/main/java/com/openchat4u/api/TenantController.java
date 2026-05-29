package com.openchat4u.api;

import com.openchat4u.datasource.DataSourceRegistry;
import com.openchat4u.tenant.Tenant;
import com.openchat4u.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final DataSourceRegistry dataSourceRegistry;

    @GetMapping
    public List<Tenant> listTenants() {
        return tenantService.findAll().stream()
            .map(TenantController::maskPassword)
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public Tenant getTenant(@PathVariable Long id) {
        return maskPassword(
            tenantService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id))
        );
    }

    @PostMapping
    public Tenant createTenant(@RequestBody Tenant tenant) {
        Tenant created = tenantService.create(tenant);
        dataSourceRegistry.register(
            created.getCode(),
            created.getDbType(),
            created.getJdbcUrl(),
            created.getUsername(),
            created.getPassword(),
            created.getMaxConnections() != null ? created.getMaxConnections() : 5,
            created.getConnectionTimeout() != null ? created.getConnectionTimeout() : 10000
        );
        return maskPassword(created);
    }

    @PutMapping("/{id}")
    public Tenant updateTenant(@PathVariable Long id, @RequestBody Tenant tenant) {
        Tenant updated = tenantService.update(id, tenant);
        dataSourceRegistry.remove(updated.getCode());
        dataSourceRegistry.register(
            updated.getCode(),
            updated.getDbType(),
            updated.getJdbcUrl(),
            updated.getUsername(),
            updated.getPassword(),
            updated.getMaxConnections() != null ? updated.getMaxConnections() : 5,
            updated.getConnectionTimeout() != null ? updated.getConnectionTimeout() : 10000
        );
        return maskPassword(updated);
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> deleteTenant(@PathVariable Long id) {
        Tenant tenant = tenantService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
        tenantService.delete(id);
        dataSourceRegistry.remove(tenant.getCode());
        return Map.of("success", true);
    }

    @PostMapping("/{id}/connect")
    public Map<String, Object> connectTenant(@PathVariable Long id) {
        Tenant tenant = tenantService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));

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
            return Map.of("success", true, "message", "Connected successfully");
        } catch (Exception e) {
            return Map.of("success", false, "message", "Connection failed: " + e.getMessage());
        }
    }

    @GetMapping("/db-types")
    public List<Map<String, String>> getDbTypes() {
        return List.of(
            Map.of("value", "POSTGRESQL", "label", "PostgreSQL"),
            Map.of("value", "MYSQL", "label", "MySQL"),
            Map.of("value", "ORACLE", "label", "Oracle"),
            Map.of("value", "SQLSERVER", "label", "SQL Server")
        );
    }

    /**
     * Returns a shallow copy of the tenant with the password field masked.
     * Original entity is left untouched to avoid corrupting cached/persisted state.
     */
    private static Tenant maskPassword(Tenant t) {
        Tenant copy = new Tenant();
        copy.setId(t.getId());
        copy.setName(t.getName());
        copy.setCode(t.getCode());
        copy.setDescription(t.getDescription());
        copy.setDbType(t.getDbType());
        copy.setJdbcUrl(t.getJdbcUrl());
        copy.setUsername(t.getUsername());
        copy.setPassword(t.getPassword() != null && !t.getPassword().isEmpty() ? "********" : null);
        copy.setReadOnly(t.getReadOnly());
        copy.setStatus(t.getStatus());
        copy.setMaxConnections(t.getMaxConnections());
        copy.setConnectionTimeout(t.getConnectionTimeout());
        copy.setCreatedAt(t.getCreatedAt());
        copy.setUpdatedAt(t.getUpdatedAt());
        return copy;
    }
}