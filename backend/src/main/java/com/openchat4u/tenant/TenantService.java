package com.openchat4u.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openchat4u.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantService {
    private final TenantRepository tenantRepository;
    private final EncryptionUtil encryptionUtil;

    public List<Tenant> findAll() {
        List<Tenant> all = tenantRepository.selectList(null);
        all.forEach(this::decryptPassword);
        return all;
    }

    public Optional<Tenant> findById(Long id) {
        return Optional.ofNullable(tenantRepository.selectById(id)).map(this::decryptPassword);
    }

    public Optional<Tenant> findByCode(String code) {
        return Optional.ofNullable(
            tenantRepository.selectOne(new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, code))
        ).map(this::decryptPassword);
    }

    public Tenant create(Tenant tenant) {
        if (existsByCode(tenant.getCode())) {
            throw new IllegalArgumentException("Tenant code already exists: " + tenant.getCode());
        }
        tenant.setReadOnly(true);
        tenant.setPassword(encryptionUtil.encrypt(tenant.getPassword()));
        tenantRepository.insert(tenant);
        decryptPassword(tenant);
        return tenant;
    }

    public Tenant update(Long id, Tenant details) {
        Tenant tenant = tenantRepository.selectById(id);
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant not found: " + id);
        }

        tenant.setName(details.getName());
        tenant.setDescription(details.getDescription());
        tenant.setDbType(details.getDbType());
        tenant.setJdbcUrl(details.getJdbcUrl());
        tenant.setUsername(details.getUsername());
        if (details.getPassword() != null && !details.getPassword().isEmpty()) {
            tenant.setPassword(encryptionUtil.encrypt(details.getPassword()));
        }
        tenant.setStatus(details.getStatus());
        if (details.getMaxConnections() != null) {
            tenant.setMaxConnections(details.getMaxConnections());
        }
        if (details.getConnectionTimeout() != null) {
            tenant.setConnectionTimeout(details.getConnectionTimeout());
        }

        tenantRepository.updateById(tenant);
        decryptPassword(tenant);
        return tenant;
    }

    public void delete(Long id) {
        tenantRepository.deleteById(id);
    }

    public boolean existsByCode(String code) {
        return tenantRepository.exists(new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, code));
    }

    private Tenant decryptPassword(Tenant tenant) {
        if (tenant != null && tenant.getPassword() != null) {
            tenant.setPassword(encryptionUtil.decrypt(tenant.getPassword()));
        }
        return tenant;
    }
}
