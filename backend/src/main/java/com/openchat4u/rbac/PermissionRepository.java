package com.openchat4u.rbac;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findByResourceType(String resourceType);
    List<Permission> findByAction(String action);
}
