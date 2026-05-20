package com.openchat4u.rbac;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRoleId(Long roleId);
    
    @Query("SELECT rp.permissionId FROM RolePermission rp WHERE rp.roleId = ?1")
    List<Long> findPermissionIdsByRoleId(Long roleId);
    
    void deleteByRoleId(Long roleId);
}
