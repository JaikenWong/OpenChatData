package com.openchat4u.rbac;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        
        when(roleRepository.findByTenantCode(tenantCode)).thenReturn(expected);

        List<Role> result = rbacService.getRolesByTenant(tenantCode);

        assertEquals(2, result.size());
        verify(roleRepository).findByTenantCode(tenantCode);
    }

    @Test
    void testCreateRole() {
        Role newRole = createRole(null, "NEW_ROLE", "tenant1");
        when(roleRepository.existsByName("NEW_ROLE")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        Role result = rbacService.createRole(newRole);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(roleRepository).save(newRole);
    }

    @Test
    void testCreateRoleDuplicate() {
        Role newRole = createRole(null, "EXISTING_ROLE", "tenant1");
        when(roleRepository.existsByName("EXISTING_ROLE")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> rbacService.createRole(newRole));
        verify(roleRepository, never()).save(any());
    }

    @Test
    void testUpdateRole() {
        Role existing = createRole(1L, "OLD_NAME", "tenant1");
        Role details = createRole(null, "NEW_NAME", "tenant1");
        details.setDescription("New Description");
        
        when(roleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roleRepository.save(any(Role.class))).thenReturn(existing);

        Role result = rbacService.updateRole(1L, details);

        assertEquals("NEW_NAME", existing.getName());
        assertEquals("New Description", existing.getDescription());
    }

    @Test
    void testUpdateRoleNotFound() {
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> rbacService.updateRole(999L, new Role()));
    }

    @Test
    void testDeleteRole() {
        rbacService.deleteRole(1L);

        verify(rolePermissionRepository).deleteByRoleId(1L);
        verify(roleRepository).deleteById(1L);
    }

    @Test
    void testGetAllPermissions() {
        List<Permission> expected = Arrays.asList(
            createPermission(1L, "QUERY_READ", "查询读取", "QUERY", "READ"),
            createPermission(2L, "QUERY_WRITE", "查询写入", "QUERY", "WRITE")
        );
        
        when(permissionRepository.findAll()).thenReturn(expected);

        List<Permission> result = rbacService.getAllPermissions();

        assertEquals(2, result.size());
    }

    @Test
    void testGetPermissionsByRole() {
        Long roleId = 1L;
        List<Long> permissionIds = Arrays.asList(1L, 2L);
        List<Permission> permissions = Arrays.asList(
            createPermission(1L, "PERM1", "Permission 1", "QUERY", "READ"),
            createPermission(2L, "PERM2", "Permission 2", "SCHEMA", "READ")
        );
        
        when(rolePermissionRepository.findPermissionIdsByRoleId(roleId)).thenReturn(permissionIds);
        when(permissionRepository.findAllById(permissionIds)).thenReturn(permissions);

        List<Permission> result = rbacService.getPermissionsByRole(roleId);

        assertEquals(2, result.size());
    }

    @Test
    void testAssignPermissionsToRole() {
        Long roleId = 1L;
        List<Long> permissionIds = Arrays.asList(1L, 2L, 3L);

        rbacService.assignPermissionsToRole(roleId, permissionIds);

        verify(rolePermissionRepository).deleteByRoleId(roleId);
        verify(rolePermissionRepository, times(3)).save(any(RolePermission.class));
    }

    @Test
    void testGetRolesByUser() {
        Long userId = 1L;
        List<UserRole> userRoles = Arrays.asList(
            createUserRole(1L, userId, 1L, "tenant1"),
            createUserRole(2L, userId, 2L, "tenant1")
        );
        List<Role> roles = Arrays.asList(
            createRole(1L, "ADMIN", "tenant1"),
            createRole(2L, "VIEWER", "tenant1")
        );
        
        when(userRoleRepository.findByUserId(userId)).thenReturn(userRoles);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(roles.get(0)));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(roles.get(1)));

        List<Role> result = rbacService.getRolesByUser(userId);

        assertEquals(2, result.size());
    }

    @Test
    void testAssignRoleToUser() {
        Long userId = 1L;
        Long roleId = 2L;
        String tenantCode = "tenant1";

        rbacService.assignRoleToUser(userId, roleId, tenantCode);

        verify(userRoleRepository).save(argThat(ur ->
            ur.getUserId().equals(userId) &&
            ur.getRoleId().equals(roleId) &&
            ur.getTenantCode().equals(tenantCode)
        ));
    }

    @Test
    void testRemoveRoleFromUser() {
        Long userId = 1L;
        Long roleId = 2L;

        rbacService.removeRoleFromUser(userId, roleId);

        verify(userRoleRepository).deleteByUserIdAndRoleId(userId, roleId);
    }

    @Test
    void testGetPermissionsByUser() {
        Long userId = 1L;
        List<UserRole> userRoles = List.of(createUserRole(1L, userId, 1L, "tenant1"));
        List<Long> permissionIds = List.of(1L, 2L);
        List<Permission> permissions = Arrays.asList(
            createPermission(1L, "PERM1", "Permission 1", "QUERY", "READ"),
            createPermission(2L, "PERM2", "Permission 2", "SCHEMA", "READ")
        );
        
        when(userRoleRepository.findByUserId(userId)).thenReturn(userRoles);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(createRole(1L, "ADMIN", "tenant1")));
        when(rolePermissionRepository.findPermissionIdsByRoleId(1L)).thenReturn(permissionIds);
        when(permissionRepository.findAllById(permissionIds)).thenReturn(permissions);

        List<Permission> result = rbacService.getPermissionsByUser(userId);

        assertEquals(2, result.size());
    }

    @Test
    void testHasPermission() {
        Long userId = 1L;
        List<UserRole> userRoles = List.of(createUserRole(1L, userId, 1L, "tenant1"));
        List<Long> permissionIds = List.of(1L);
        List<Permission> permissions = List.of(
            createPermission(1L, "QUERY_READ", "查询读取", "QUERY", "READ")
        );
        
        when(userRoleRepository.findByUserId(userId)).thenReturn(userRoles);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(createRole(1L, "ADMIN", "tenant1")));
        when(rolePermissionRepository.findPermissionIdsByRoleId(1L)).thenReturn(permissionIds);
        when(permissionRepository.findAllById(permissionIds)).thenReturn(permissions);

        assertTrue(rbacService.hasPermission(userId, "QUERY_READ"));
        assertFalse(rbacService.hasPermission(userId, "QUERY_WRITE"));
    }

    @Test
    void testInitializeDefaultRoles() {
        when(roleRepository.existsByName("ADMIN")).thenReturn(false);
        when(roleRepository.existsByName("DATA_ANALYST")).thenReturn(false);
        when(roleRepository.existsByName("VIEWER")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        rbacService.initializeDefaultRoles();

        verify(roleRepository, times(3)).save(any(Role.class));
    }

    @Test
    void testInitializeDefaultRolesAlreadyExist() {
        when(roleRepository.existsByName("ADMIN")).thenReturn(true);
        when(roleRepository.existsByName("DATA_ANALYST")).thenReturn(true);
        when(roleRepository.existsByName("VIEWER")).thenReturn(true);

        rbacService.initializeDefaultRoles();

        verify(roleRepository, never()).save(any(Role.class));
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
}
