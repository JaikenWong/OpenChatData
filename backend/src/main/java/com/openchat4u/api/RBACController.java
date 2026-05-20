package com.openchat4u.api;

import com.openchat4u.rbac.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rbac")
@RequiredArgsConstructor
public class RBACController {

    private final RBACService rbacService;

    @GetMapping("/roles")
    public List<Role> listRoles(@RequestParam(required = false) String tenantCode) {
        if (tenantCode != null) {
            return rbacService.getRolesByTenant(tenantCode);
        }
        return rbacService.getRolesByTenant("system");
    }

    @PostMapping("/roles")
    public Role createRole(@RequestBody Role role) {
        return rbacService.createRole(role);
    }

    @PutMapping("/roles/{id}")
    public Role updateRole(@PathVariable Long id, @RequestBody Role role) {
        return rbacService.updateRole(id, role);
    }

    @DeleteMapping("/roles/{id}")
    public Map<String, Boolean> deleteRole(@PathVariable Long id) {
        rbacService.deleteRole(id);
        return Map.of("success", true);
    }

    @GetMapping("/permissions")
    public List<Permission> listPermissions() {
        return rbacService.getAllPermissions();
    }

    @GetMapping("/roles/{roleId}/permissions")
    public List<Permission> getRolePermissions(@PathVariable Long roleId) {
        return rbacService.getPermissionsByRole(roleId);
    }

    @PostMapping("/roles/{roleId}/permissions")
    public Map<String, Boolean> assignPermissions(@PathVariable Long roleId, @RequestBody List<Long> permissionIds) {
        rbacService.assignPermissionsToRole(roleId, permissionIds);
        return Map.of("success", true);
    }

    @GetMapping("/users/{userId}/roles")
    public List<Role> getUserRoles(@PathVariable Long userId) {
        return rbacService.getRolesByUser(userId);
    }

    @PostMapping("/users/{userId}/roles")
    public Map<String, Boolean> assignRole(@PathVariable Long userId, @RequestBody Map<String, Object> request) {
        Long roleId = Long.valueOf(request.get("roleId").toString());
        String tenantCode = request.get("tenantCode") != null ? request.get("tenantCode").toString() : "system";
        rbacService.assignRoleToUser(userId, roleId, tenantCode);
        return Map.of("success", true);
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    public Map<String, Boolean> removeRole(@PathVariable Long userId, @PathVariable Long roleId) {
        rbacService.removeRoleFromUser(userId, roleId);
        return Map.of("success", true);
    }

    @GetMapping("/users/{userId}/permissions")
    public List<Permission> getUserPermissions(@PathVariable Long userId) {
        return rbacService.getPermissionsByUser(userId);
    }
}
