package com.openchat4u.rbac;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RBACService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;

    public List<Role> getRolesByTenant(String tenantCode) {
        return roleRepository.selectList(
            new LambdaQueryWrapper<Role>().eq(Role::getTenantCode, tenantCode)
        );
    }

    @Transactional
    public Role createRole(Role role) {
        if (existsRoleByName(role.getName())) {
            throw new IllegalArgumentException("Role name already exists: " + role.getName());
        }
        roleRepository.insert(role);
        return role;
    }

    @Transactional
    public Role updateRole(Long id, Role details) {
        Role role = roleRepository.selectById(id);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + id);
        }
        role.setName(details.getName());
        role.setDescription(details.getDescription());
        roleRepository.updateById(role);
        return role;
    }

    @Transactional
    public void deleteRole(Long id) {
        rolePermissionRepository.delete(
            new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id)
        );
        roleRepository.deleteById(id);
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.selectList(null);
    }

    public List<Permission> getPermissionsByRole(Long roleId) {
        List<RolePermission> rps = rolePermissionRepository.selectList(
            new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
        );
        if (rps.isEmpty()) return Collections.emptyList();
        List<Long> permissionIds = rps.stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
        return permissionRepository.selectBatchIds(permissionIds);
    }

    @Transactional
    public void assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        rolePermissionRepository.delete(
            new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
        );
        for (Long permissionId : permissionIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            rolePermissionRepository.insert(rp);
        }
    }

    public List<Role> getRolesByUser(Long userId) {
        List<UserRole> userRoles = userRoleRepository.selectList(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );
        return userRoles.stream()
            .map(ur -> roleRepository.selectById(ur.getRoleId()))
            .filter(r -> r != null)
            .collect(Collectors.toList());
    }

    @Transactional
    public void assignRoleToUser(Long userId, Long roleId, String tenantCode) {
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        ur.setTenantCode(tenantCode);
        userRoleRepository.insert(ur);
    }

    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        userRoleRepository.delete(
            new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, roleId)
        );
    }

    public List<Permission> getPermissionsByUser(Long userId) {
        List<Role> roles = getRolesByUser(userId);
        return roles.stream()
            .flatMap(role -> getPermissionsByRole(role.getId()).stream())
            .distinct()
            .collect(Collectors.toList());
    }

    public boolean hasPermission(Long userId, String permissionCode) {
        List<Permission> permissions = getPermissionsByUser(userId);
        return permissions.stream().anyMatch(p -> p.getCode().equals(permissionCode));
    }

    /**
     * Permission check stub.
     *
     * SECURITY NOTE: this is a TRANSITION-PERIOD stub. It currently:
     *   1. Allows if tenant has no roles defined (bootstrap mode).
     *   2. Allows if tenant has roles defined (TODO: resolve userId from
     *      username + tenantCode and call hasPermission()).
     *
     * To enforce real RBAC, the caller must:
     *   - inject UserRepository, look up user by (username, tenantCode)
     *   - call hasPermission(userId, permissionCode)
     *   - throw org.springframework.security.access.AccessDeniedException on deny.
     *
     * @return true = allowed, false = denied. Currently always returns true.
     */
    public boolean checkPermission(String username, String tenantCode, String permissionCode) {
        List<Role> roles = getRolesByTenant(tenantCode);
        if (roles.isEmpty()) {
            // No roles configured yet — transition mode allows
            return true;
        }
        // TODO: resolve userId from username + tenantCode, then hasPermission()
        return true;
    }

    public void initializeDefaultRoles() {
        if (!existsRoleByName("ADMIN")) {
            Role admin = new Role();
            admin.setName("ADMIN");
            admin.setDescription("System Administrator");
            admin.setIsSystem(true);
            roleRepository.insert(admin);
        }
        if (!existsRoleByName("DATA_ANALYST")) {
            Role analyst = new Role();
            analyst.setName("DATA_ANALYST");
            analyst.setDescription("Data Analyst - Can query and view history");
            analyst.setIsSystem(true);
            roleRepository.insert(analyst);
        }
        if (!existsRoleByName("VIEWER")) {
            Role viewer = new Role();
            viewer.setName("VIEWER");
            viewer.setDescription("Viewer - Can only view history");
            viewer.setIsSystem(true);
            roleRepository.insert(viewer);
        }
    }

    private boolean existsRoleByName(String name) {
        return roleRepository.exists(new LambdaQueryWrapper<Role>().eq(Role::getName, name));
    }
}
