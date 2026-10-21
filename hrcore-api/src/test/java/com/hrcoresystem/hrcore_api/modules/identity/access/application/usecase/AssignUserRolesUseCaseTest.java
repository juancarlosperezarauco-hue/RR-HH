package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.AssignUserRolesRequest;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.TenantRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.UserRolesResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.TenantRoleMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantRoleAssignmentPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.TenantRoleAssignmentService;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRoleType;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRolePermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception.TenantUserNotFoundException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignUserRolesUseCaseTest {

    @Mock
    private TenantUserRepository tenantUserRepository;

    @Mock
    private TenantRoleRepository tenantRoleRepository;

    @Mock
    private TenantRolePermissionRepository tenantRolePermissionRepository;

    @Mock
    private TenantUserRoleRepository tenantUserRoleRepository;

    @Mock
    private TenantRoleMapper tenantRoleMapper;

    @Mock
    private AuditTrailService auditTrailService;

    @Mock
    private TenantRoleAssignmentService assignmentService;

    @Mock
    private TenantOwnershipPolicy ownershipPolicy;

    @Mock
    private TenantAuthorizationStateRepository authorizationStateRepository;

    private AssignUserRolesUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID roleId1 = UUID.randomUUID();
    private final UUID roleId2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new AssignUserRolesUseCase(
                tenantUserRepository,
                tenantRoleRepository,
                tenantRolePermissionRepository,
                tenantUserRoleRepository,
                tenantRoleMapper,
                auditTrailService,
                assignmentService,
                ownershipPolicy
        );
    }

    @Test
    @DisplayName("Criterios 1, 2, 7: Debe asignar exitosamente roles activos a un usuario y registrar auditoría")
    void shouldSuccessfullyAssignActiveRolesToUserInSameTenant() {
        TenantUser user = new TenantUser(
                userId, "developer@example.com", "hash", "Carlos", "Perez",
                true, TenantUserStatus.ACTIVE, 1L, Instant.now(), Instant.now()
        );
        TenantRole role1 = new TenantRole(
                roleId1, "ROL-00001", "Developer", "Dev role",
                false, true, Instant.now(), Instant.now(), TenantRoleType.INTERNAL
        );
        TenantRole role2 = new TenantRole(
                roleId2, "ROL-00002", "QA", "QA role",
                false, true, Instant.now(), Instant.now(), TenantRoleType.INTERNAL
        );

        TenantRoleResponse resp1 = new TenantRoleResponse(
                roleId1, "ROL-00001", "Developer", "Dev role",
                TenantRoleType.INTERNAL, false, true, Instant.now(), Instant.now(), List.of("contracts.read")
        );
        TenantRoleResponse resp2 = new TenantRoleResponse(
                roleId2, "ROL-00002", "QA", "QA role",
                TenantRoleType.INTERNAL, false, true, Instant.now(), Instant.now(), List.of("attendance.records.list")
        );

        AssignUserRolesRequest request = new AssignUserRolesRequest(List.of(roleId1, roleId2));

        when(tenantUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tenantUserRoleRepository.findRoleCodesByUserId(userId)).thenReturn(List.of("ROL-00001"));
        when(assignmentService.replaceRoles(userId, List.of(roleId1, roleId2))).thenReturn(List.of(role1, role2));
        when(tenantRolePermissionRepository.findPermissionCodesByRoleId(roleId1)).thenReturn(Set.of("contracts.read"));
        when(tenantRolePermissionRepository.findPermissionCodesByRoleId(roleId2)).thenReturn(Set.of("attendance.records.list"));
        when(tenantRoleMapper.toResponse(eq(role1), anyList())).thenReturn(resp1);
        when(tenantRoleMapper.toResponse(eq(role2), anyList())).thenReturn(resp2);

        UserRolesResponse response = useCase.execute(userId, request);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals(2, response.roles().size());
        verify(ownershipPolicy).requireCurrentOwner();
        verify(assignmentService).replaceRoles(userId, List.of(roleId1, roleId2));
        verify(auditTrailService).recordTenantEvent(
                eq(AuditEventTypes.USER_ROLES_ASSIGNED),
                eq("USER"),
                eq(userId.toString()),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Criterio 3: Debe permitir revocar roles cuando la lista de roles es vacía")
    void shouldSuccessfullyRevokeRolesWhenRoleListIsEmpty() {
        TenantUser user = new TenantUser(
                userId, "user@example.com", "hash", "Maria", "Lopez",
                true, TenantUserStatus.ACTIVE, 1L, Instant.now(), Instant.now()
        );

        AssignUserRolesRequest request = new AssignUserRolesRequest(List.of());

        when(tenantUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tenantUserRoleRepository.findRoleCodesByUserId(userId)).thenReturn(List.of("ROL-00001"));
        when(assignmentService.replaceRoles(userId, List.of())).thenReturn(List.of());

        UserRolesResponse response = useCase.execute(userId, request);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertTrue(response.roles().isEmpty());
        verify(assignmentService).replaceRoles(userId, List.of());
        verify(auditTrailService).recordTenantEvent(
                eq(AuditEventTypes.USER_ROLES_ASSIGNED),
                eq("USER"),
                eq(userId.toString()),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Criterio 4: Debe impedir asignar roles inactivos")
    void shouldRejectAssignmentOfInactiveRole() {
        TenantUser user = new TenantUser(
                userId, "user@example.com", "hash", "Carlos", "Perez",
                true, TenantUserStatus.ACTIVE, 1L, Instant.now(), Instant.now()
        );
        AssignUserRolesRequest request = new AssignUserRolesRequest(List.of(roleId1));

        when(tenantUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(assignmentService.replaceRoles(userId, List.of(roleId1)))
                .thenThrow(new BusinessException("No es posible asignar el rol 'Inactivo' porque se encuentra inactivo"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> useCase.execute(userId, request)
        );

        assertTrue(exception.getMessage().contains("porque se encuentra inactivo"));
        verify(auditTrailService, never()).recordTenantEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Criterio 4: Debe impedir asignar roles que no existen o pertenezcan a otro tenant")
    void shouldRejectRoleBelongingToAnotherTenantOrNonexistent() {
        TenantUser user = new TenantUser(
                userId, "user@example.com", "hash", "Carlos", "Perez",
                true, TenantUserStatus.ACTIVE, 1L, Instant.now(), Instant.now()
        );
        AssignUserRolesRequest request = new AssignUserRolesRequest(List.of(roleId1));

        when(tenantUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(assignmentService.replaceRoles(userId, List.of(roleId1)))
                .thenThrow(new BusinessException("Uno o más roles no existen en la organización"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> useCase.execute(userId, request)
        );

        assertTrue(exception.getMessage().contains("no existen en la organización"));
        verify(auditTrailService, never()).recordTenantEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Criterio 5: Debe impedir retirar el rol administrativo cuando el usuario es el único administrador activo")
    void shouldPreventRevokingAdministrativeRoleWhenUserIsSoleActiveAdmin() {
        TenantUser user = new TenantUser(
                userId, "admin@example.com", "hash", "Admin", "User",
                true, TenantUserStatus.ACTIVE, 1L, Instant.now(), Instant.now()
        );
        AssignUserRolesRequest request = new AssignUserRolesRequest(List.of());

        when(tenantUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(assignmentService.replaceRoles(userId, List.of()))
                .thenThrow(new BusinessException("No es posible retirar el rol administrativo cuando el usuario es el único administrador activo de la organización"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> useCase.execute(userId, request)
        );

        assertEquals("No es posible retirar el rol administrativo cuando el usuario es el único administrador activo de la organización", exception.getMessage());
        verify(auditTrailService, never()).recordTenantEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Criterio 5: Debe impedir modificar roles del propietario de la organización")
    void shouldPreventModifyingTenantOwnerRoles() {
        TenantUser user = new TenantUser(
                userId, "owner@example.com", "hash", "Owner", "User",
                true, TenantUserStatus.ACTIVE, 1L, Instant.now(), Instant.now()
        );
        AssignUserRolesRequest request = new AssignUserRolesRequest(List.of(roleId1));

        when(tenantUserRepository.findById(userId)).thenReturn(Optional.of(user));
        doThrow(new AccessDeniedException("Current tenant owner cannot be modified"))
                .when(ownershipPolicy).assertNotCurrentOwner(userId);

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> useCase.execute(userId, request)
        );

        assertEquals("Current tenant owner cannot be modified", exception.getMessage());
        verify(assignmentService, never()).replaceRoles(any(), any());
        verify(auditTrailService, never()).recordTenantEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe fallar si el usuario objetivo no existe en el tenant")
    void shouldThrowWhenTargetUserNotFoundInTenant() {
        AssignUserRolesRequest request = new AssignUserRolesRequest(List.of(roleId1));
        when(tenantUserRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(TenantUserNotFoundException.class, () -> useCase.execute(userId, request));
        verify(assignmentService, never()).replaceRoles(any(), any());
    }

    @Test
    @DisplayName("Debe fallar si quien intenta asignar no posee privilegios de propietario/administrador")
    void shouldRequireCurrentOwnerPrivileges() {
        AssignUserRolesRequest request = new AssignUserRolesRequest(List.of(roleId1));
        doThrow(new AccessDeniedException("Current tenant owner required"))
                .when(ownershipPolicy).requireCurrentOwner();

        assertThrows(AccessDeniedException.class, () -> useCase.execute(userId, request));
        verify(tenantUserRepository, never()).findById(any());
        verify(assignmentService, never()).replaceRoles(any(), any());
    }
}
