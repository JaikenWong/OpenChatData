package com.openchat4u.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantService {
    private final TenantRepository tenantRepository;

    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    public Optional<Tenant> findById(Long id) {
        return tenantRepository.findById(id);
    }

    public Optional<Tenant> findByCode(String code) {
        return tenantRepository.findByCode(code);
    }

    public Tenant create(Tenant tenant) {
        if (tenantRepository.existsByCode(tenant.getCode())) {
            throw new IllegalArgumentException("Tenant code already exists: " + tenant.getCode());
        }
        tenant.setReadOnly(true);
        return tenantRepository.save(tenant);
    }

    public Tenant update(Long id, Tenant details) {
        Tenant tenant = tenantRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));

        tenant.setName(details.getName());
        tenant.setDescription(details.getDescription());
        tenant.setDbType(details.getDbType());
        tenant.setJdbcUrl(details.getJdbcUrl());
        tenant.setUsername(details.getUsername());
        if (details.getPassword() != null && !details.getPassword().isEmpty()) {
            tenant.setPassword(details.getPassword());
        }
        tenant.setStatus(details.getStatus());
        if (details.getMaxConnections() != null) {
            tenant.setMaxConnections(details.getMaxConnections());
        }
        if (details.getConnectionTimeout() != null) {
            tenant.setConnectionTimeout(details.getConnectionTimeout());
        }

        return tenantRepository.save(tenant);
    }

    public void delete(Long id) {
        tenantRepository.deleteById(id);
    }

    public boolean existsByCode(String code) {
        return tenantRepository.existsByCode(code);
    }
}
