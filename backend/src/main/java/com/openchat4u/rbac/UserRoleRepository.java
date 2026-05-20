package com.openchat4u.rbac;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUserId(Long userId);
    List<UserRole> findByRoleId(Long roleId);
    List<UserRole> findByTenantCode(String tenantCode);
    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}
