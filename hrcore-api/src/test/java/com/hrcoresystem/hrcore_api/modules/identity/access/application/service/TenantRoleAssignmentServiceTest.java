package com.hrcoresystem.hrcore_api.modules.identity.access.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantRoleAssignmentPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRoleType;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRolePermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantRoleAssignmentServiceTest {

    @Mock
    private TenantRoleAssignmentPolicy policy;

    @Mock
    private TenantUserRoleRepository assignments;

    @Mock
    private TenantUserRepository users;

    @Mock
    private TenantOwnershipPolicy ownership;

    @Mock
    private TenantAuthorizationStateRepository authorizationState;

    @Mock
    private TenantRoleRepository roleRepository;

    @Mock
    private TenantRolePermissionRepository rolePermissionRepository;

    private TenantRoleAssignmentService service;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID adminUserId = UUID.randomUUID();
    private final UUID standardUserId = UUID.randomUUID();
    private final UUID adminRoleId = UUID.randomUUID();
    private final UUID standardRoleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TenantRoleAssignmentService(
                policy,
                assignments,
                users,
                ownership,
                authorizationState,
                roleRepository,
                rolePermissionRepository
        );
    }

    @Test
    @DisplayName("Criterio 6: Debe incrementar securityVersion del usuario al cambiar roles exitosamente")
    void shouldIncrementSecurityVersionWhenRolesChange() {
        TenantUser user = new TenantUser(
                standardUserId, "user@test.com", "hash", "Standard", "User",
                true, TenantUserStatus.ACTIVE, 1L, Instant.now(), Instant.now()
        );
        TenantRole role = new TenantRole(
                standardRoleId, "ROL-00001", "Standard Role", "Desc",
                false, true, Instant.now(), Instant.now(), TenantRoleType.INTERNAL
        );

        when(users.findById(standardUserId)).thenReturn(Optional.of(user));
        when(authorizationState.ownerUserId()).thenReturn(ownerId);
        when(assignments.findRoleIdsByUserId(standardUserId)).thenReturn(List.of());
        when(policy.validateAssignableRoleIds(List.of(standardRoleId))).thenReturn(List.of(role));
        when(assignments.replaceUserRoles(standardUserId, List.of(standardRoleId))).thenReturn(true);

        List<TenantRole> result = service.replaceRoles(standardUserId, List.of(standardRoleId));

        assertEquals(1, result.size());
        verify(ownership).assertNotCurrentOwner(standardUserId);
        verify(users).incrementSecurityVersion(standardUserId);
    }

    @Test
    @DisplayName("Criterio 5: Debe bloquear el retiro de rol administrativo al único administrador activo")
    void shouldBlockRevokingAdministrativeRoleWhenUserIsSoleActiveAdmin() {
        TenantUser user = new TenantUser(
                adminUserId, "admin@test.com", "hash", "Sole", "Admin",
                true, TenantUserStatus.ACTIVE, 1L, Instant.now(), Instant.now()
        );
        TenantRole adminRole = new TenantRole(
                adminRoleId, "ADMIN", "Administrador", "Admin role",
                false, true, Instant.now(), Instant.now(), TenantRoleType.INTERNAL
        );

        when(users.findById(adminUserId)).thenReturn(Optional.of(user));
        // ownerId es nulo o el mismo usuario
        when(authorizationState.ownerUserId()).thenReturn(null);
        when(assignments.findRoleIdsByUserId(adminUserId)).thenReturn(List.of(adminRoleId));
        when(roleRepository.findAllByIds(List.of(adminRoleId))).thenReturn(List.of(adminRole));
        when(users.findAll()).thenReturn(List.of(user));

        // Intenta retirar todos los roles
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.replaceRoles(adminUserId, List.of())
        );

        assertEquals("No es posible retirar el rol administrativo cuando el usuario es el único administrador activo de la organización", exception.getMessage());
        verify(assignments, never()).replaceUserRoles(any(), any());
    }

    @Test
    @DisplayName("Criterio 5: Debe permitir el retiro de rol administrativo si existe otro administrador activo")
    void shouldAllowRevokingAdminRoleIfAnotherActiveAdminExists() {
        TenantUser user = new TenantUser(
                adminUserId, "admin2@test.com", "hash", "Second", "Admin",
                true, TenantUserStatus.ACTIVE, 1L, Instant.now(), Instant.now()
        );
        TenantUser ownerUser = new TenantUser(
                ownerId, "owner@test.com", "hash", "Owner", "Admin",
                true, TenantUserStatus.ACTIVE, 1L, Instant.now(), Instant.now()
        );
        TenantRole adminRole = new TenantRole(
                adminRoleId, "ADMIN", "Administrador", "Admin role",
                false, true, Instant.now(), Instant.now(), TenantRoleType.INTERNAL
        );

        when(users.findById(adminUserId)).thenReturn(Optional.of(user));
        when(authorizationState.ownerUserId()).thenReturn(ownerId);
        when(users.findById(ownerId)).thenReturn(Optional.of(ownerUser));
        when(assignments.findRoleIdsByUserId(adminUserId)).thenReturn(List.of(adminRoleId));
        when(roleRepository.findAllByIds(List.of(adminRoleId))).thenReturn(List.of(adminRole));
        when(policy.validateAssignableRoleIds(List.of())).thenReturn(List.of());
        when(assignments.replaceUserRoles(adminUserId, List.of())).thenReturn(true);

        List<TenantRole> result = service.replaceRoles(adminUserId, List.of());

        assertTrue(result.isEmpty());
        verify(assignments).replaceUserRoles(adminUserId, List.of());
        verify(users).incrementSecurityVersion(adminUserId);
    }

    @Test
    @DisplayName("Criterio 5: Debe bloquear modificar roles del propietario")
    void shouldBlockModifyingRolesOfOwner() {
        TenantUser ownerUser = new TenantUser(
                ownerId, "owner@test.com", "hash", "Owner", "Admin",
                true, TenantUserStatus.ACTIVE, 1L, Instant.now(), Instant.now()
        );

        when(users.findById(ownerId)).thenReturn(Optional.of(ownerUser));
        when(authorizationState.ownerUserId()).thenReturn(ownerId);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.replaceRoles(ownerId, List.of(standardRoleId))
        );

        assertEquals("No es posible retirar el rol administrativo cuando el usuario es el único administrador activo de la organización", exception.getMessage());
        verify(assignments, never()).replaceUserRoles(any(), any());
    }
}
