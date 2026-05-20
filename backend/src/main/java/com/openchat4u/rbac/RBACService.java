package com.openchat4u.rbac;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return roleRepository.findByTenantCode(tenantCode);
    }

    @Transactional
    public Role createRole(Role role) {
        if (roleRepository.existsByName(role.getName())) {
            throw new IllegalArgumentException("Role name already exists: " + role.getName());
        }
        return roleRepository.save(role);
    }

    @Transactional
    public Role updateRole(Long id, Role details) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
        role.setName(details.getName());
        role.setDescription(details.getDescription());
        return roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        rolePermissionRepository.deleteByRoleId(id);
        roleRepository.deleteById(id);
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    public List<Permission> getPermissionsByRole(Long roleId) {
        List<Long> permissionIds = rolePermissionRepository.findPermissionIdsByRoleId(roleId);
        return permissionRepository.findAllById(permissionIds);
    }

    @Transactional
    public void assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        rolePermissionRepository.deleteByRoleId(roleId);
        for (Long permissionId : permissionIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            rolePermissionRepository.save(rp);
        }
    }

    public List<Role> getRolesByUser(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        return userRoles.stream()
            .map(ur -> roleRepository.findById(ur.getRoleId()).orElse(null))
            .filter(r -> r != null)
            .collect(Collectors.toList());
    }

    @Transactional
    public void assignRoleToUser(Long userId, Long roleId, String tenantCode) {
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        ur.setTenantCode(tenantCode);
        userRoleRepository.save(ur);
    }

    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
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

    public void initializeDefaultRoles() {
        if (!roleRepository.existsByName("ADMIN")) {
            Role admin = new Role();
            admin.setName("ADMIN");
            admin.setDescription("System Administrator");
            admin.setIsSystem(true);
            roleRepository.save(admin);
        }
        if (!roleRepository.existsByName("DATA_ANALYST")) {
            Role analyst = new Role();
            analyst.setName("DATA_ANALYST");
            analyst.setDescription("Data Analyst - Can query and view history");
            analyst.setIsSystem(true);
            roleRepository.save(analyst);
        }
        if (!roleRepository.existsByName("VIEWER")) {
            Role viewer = new Role();
            viewer.setName("VIEWER");
            viewer.setDescription("Viewer - Can only view history");
            viewer.setIsSystem(true);
            roleRepository.save(viewer);
        }
    }
}
