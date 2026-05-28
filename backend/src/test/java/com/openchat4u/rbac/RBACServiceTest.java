package com.openchat4u.rbac;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RBACServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private RBACService rbacService;

    @BeforeEach
    void setUp() {
        rbacService = new RBACService(roleRepository, permissionRepository, rolePermissionRepository, userRoleRepository);
    }

    @Test
    void testGetRolesByTenant() {
        String tenantCode = "tenant1";
        List<Role> expected = Arrays.asList(
            createRole(1L, "ADMIN", tenantCode),
            createRole(2L, "VIEWER", tenantCode)
        );

        when(roleRepository.selectList(any(Wrapper.class))).thenReturn(expected);

        List<Role> result = rbacService.getRolesByTenant(tenantCode);

        assertEquals(2, result.size());
        verify(roleRepository).selectList(any(Wrapper.class));
    }

    @Test
    void testCreateRole() {
        Role newRole = createRole(null, "NEW_ROLE", "tenant1");
        when(roleRepository.exists(any(Wrapper.class))).thenReturn(false);
        when(roleRepository.insert(any(Role.class))).thenAnswer(invocation -> {
            Role r = invocation.getArgument(0);
            r.setId(1L);
            return 1;
        });

        Role result = rbacService.createRole(newRole);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(roleRepository).insert(newRole);
    }

    @Test
    void testCreateRoleDuplicate() {
        Role newRole = createRole(null, "EXISTING_ROLE", "tenant1");
        when(roleRepository.exists(any(Wrapper.class))).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> rbacService.createRole(newRole));
        verify(roleRepository, never()).insert(any(Role.class));
    }

    @Test
    void testUpdateRole() {
        Role existing = createRole(1L, "OLD_NAME", "tenant1");
        Role details = createRole(null, "NEW_NAME", "tenant1");
        details.setDescription("New Description");

        when(roleRepository.selectById(1L)).thenReturn(existing);
        when(roleRepository.updateById(any(Role.class))).thenReturn(1);

        Role result = rbacService.updateRole(1L, details);

        assertEquals("NEW_NAME", existing.getName());
        assertEquals("New Description", existing.getDescription());
        assertSame(existing, result);
    }

    @Test
    void testUpdateRoleNotFound() {
        when(roleRepository.selectById(999L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> rbacService.updateRole(999L, new Role()));
    }

    @Test
    void testDeleteRole() {
        when(rolePermissionRepository.delete(any(Wrapper.class))).thenReturn(0);
        when(roleRepository.deleteById(1L)).thenReturn(1);

        rbacService.deleteRole(1L);

        verify(rolePermissionRepository).delete(any(Wrapper.class));
        verify(roleRepository).deleteById(1L);
    }

    @Test
    void testGetAllPermissions() {
        List<Permission> expected = Arrays.asList(
            createPermission(1L, "QUERY_READ", "查询读取", "QUERY", "READ"),
            createPermission(2L, "QUERY_WRITE", "查询写入", "QUERY", "WRITE")
        );

        when(permissionRepository.selectList(any())).thenReturn(expected);

        List<Permission> result = rbacService.getAllPermissions();

        assertEquals(2, result.size());
    }

    @Test
    void testGetPermissionsByRole() {
        Long roleId = 1L;
        List<RolePermission> rps = Arrays.asList(
            createRolePermission(1L, roleId, 1L),
            createRolePermission(2L, roleId, 2L)
        );
        List<Permission> permissions = Arrays.asList(
            createPermission(1L, "PERM1", "Permission 1", "QUERY", "READ"),
            createPermission(2L, "PERM2", "Permission 2", "SCHEMA", "READ")
        );

        when(rolePermissionRepository.selectList(any(Wrapper.class))).thenReturn(rps);
        when(permissionRepository.selectBatchIds(anyCollection())).thenReturn(permissions);

        List<Permission> result = rbacService.getPermissionsByRole(roleId);

        assertEquals(2, result.size());
    }

    @Test
    void testAssignPermissionsToRole() {
        Long roleId = 1L;
        List<Long> permissionIds = Arrays.asList(1L, 2L, 3L);
        when(rolePermissionRepository.delete(any(Wrapper.class))).thenReturn(0);
        when(rolePermissionRepository.insert(any(RolePermission.class))).thenReturn(1);

        rbacService.assignPermissionsToRole(roleId, permissionIds);

        verify(rolePermissionRepository).delete(any(Wrapper.class));
        verify(rolePermissionRepository, times(3)).insert(any(RolePermission.class));
    }

    @Test
    void testGetRolesByUser() {
        Long userId = 1L;
        List<UserRole> userRoles = Arrays.asList(
            createUserRole(1L, userId, 1L, "tenant1"),
            createUserRole(2L, userId, 2L, "tenant1")
        );

        when(userRoleRepository.selectList(any(Wrapper.class))).thenReturn(userRoles);
        when(roleRepository.selectById(1L)).thenReturn(createRole(1L, "ADMIN", "tenant1"));
        when(roleRepository.selectById(2L)).thenReturn(createRole(2L, "VIEWER", "tenant1"));

        List<Role> result = rbacService.getRolesByUser(userId);

        assertEquals(2, result.size());
    }

    @Test
    void testAssignRoleToUser() {
        Long userId = 1L;
        Long roleId = 2L;
        String tenantCode = "tenant1";
        when(userRoleRepository.insert(any(UserRole.class))).thenReturn(1);

        rbacService.assignRoleToUser(userId, roleId, tenantCode);

        verify(userRoleRepository).insert(argThat((UserRole ur) ->
            ur.getUserId().equals(userId) &&
            ur.getRoleId().equals(roleId) &&
            ur.getTenantCode().equals(tenantCode)
        ));
    }

    @Test
    void testRemoveRoleFromUser() {
        Long userId = 1L;
        Long roleId = 2L;
        when(userRoleRepository.delete(any(Wrapper.class))).thenReturn(1);

        rbacService.removeRoleFromUser(userId, roleId);

        verify(userRoleRepository).delete(any(Wrapper.class));
    }

    @Test
    void testGetPermissionsByUser() {
        Long userId = 1L;
        List<UserRole> userRoles = List.of(createUserRole(1L, userId, 1L, "tenant1"));
        List<RolePermission> rps = List.of(
            createRolePermission(1L, 1L, 1L),
            createRolePermission(2L, 1L, 2L)
        );
        List<Permission> permissions = Arrays.asList(
            createPermission(1L, "PERM1", "Permission 1", "QUERY", "READ"),
            createPermission(2L, "PERM2", "Permission 2", "SCHEMA", "READ")
        );

        when(userRoleRepository.selectList(any(Wrapper.class))).thenReturn(userRoles);
        when(roleRepository.selectById(1L)).thenReturn(createRole(1L, "ADMIN", "tenant1"));
        when(rolePermissionRepository.selectList(any(Wrapper.class))).thenReturn(rps);
        when(permissionRepository.selectBatchIds(anyCollection())).thenReturn(permissions);

        List<Permission> result = rbacService.getPermissionsByUser(userId);

        assertEquals(2, result.size());
    }

    @Test
    void testHasPermission() {
        Long userId = 1L;
        List<UserRole> userRoles = List.of(createUserRole(1L, userId, 1L, "tenant1"));
        List<RolePermission> rps = List.of(createRolePermission(1L, 1L, 1L));
        List<Permission> permissions = List.of(
            createPermission(1L, "QUERY_READ", "查询读取", "QUERY", "READ")
        );

        when(userRoleRepository.selectList(any(Wrapper.class))).thenReturn(userRoles);
        when(roleRepository.selectById(1L)).thenReturn(createRole(1L, "ADMIN", "tenant1"));
        when(rolePermissionRepository.selectList(any(Wrapper.class))).thenReturn(rps);
        when(permissionRepository.selectBatchIds(anyCollection())).thenReturn(permissions);

        assertTrue(rbacService.hasPermission(userId, "QUERY_READ"));
        assertFalse(rbacService.hasPermission(userId, "QUERY_WRITE"));
    }

    @Test
    void testInitializeDefaultRoles() {
        when(roleRepository.exists(any(Wrapper.class))).thenReturn(false);
        when(roleRepository.insert(any(Role.class))).thenAnswer(invocation -> 1);

        rbacService.initializeDefaultRoles();

        verify(roleRepository, times(3)).insert(any(Role.class));
    }

    @Test
    void testInitializeDefaultRolesAlreadyExist() {
        when(roleRepository.exists(any(Wrapper.class))).thenReturn(true);

        rbacService.initializeDefaultRoles();

        verify(roleRepository, never()).insert(any(Role.class));
    }

    private Role createRole(Long id, String name, String tenantCode) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setTenantCode(tenantCode);
        role.setIsSystem(false);
        return role;
    }

    private Permission createPermission(Long id, String code, String name, String resourceType, String action) {
        Permission perm = new Permission();
        perm.setId(id);
        perm.setCode(code);
        perm.setName(name);
        perm.setResourceType(resourceType);
        perm.setAction(action);
        return perm;
    }

    private UserRole createUserRole(Long id, Long userId, Long roleId, String tenantCode) {
        UserRole ur = new UserRole();
        ur.setId(id);
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        ur.setTenantCode(tenantCode);
        return ur;
    }

    private RolePermission createRolePermission(Long id, Long roleId, Long permissionId) {
        RolePermission rp = new RolePermission();
        rp.setId(id);
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        return rp;
    }
}
