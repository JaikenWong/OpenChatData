package com.openchat4u.rbac;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {
    List<Role> findByTenantCode(String tenantCode);
    boolean existsByName(String name);
    Role findByName(String name);
}
