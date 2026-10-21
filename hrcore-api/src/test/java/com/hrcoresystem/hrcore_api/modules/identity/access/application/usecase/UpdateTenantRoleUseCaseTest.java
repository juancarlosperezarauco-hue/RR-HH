package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.TenantRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.UpdateTenantRoleRequest;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.TenantRoleMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.PermissionAssignmentPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.TenantAuthorizationRevisionService;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.exception.TenantRoleAlreadyExistsException;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.PermissionScope;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.SystemPermission;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRoleType;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.SystemPermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRolePermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.exception.TenantSubscriptionAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateTenantRoleUseCaseTest {

    @Mock
    private TenantRoleRepository tenantRoleRepository;

    @Mock
    private TenantRolePermissionRepository tenantRolePermissionRepository;

    @Mock
    private PermissionAssignmentPolicy permissionAssignmentPolicy;

    @Mock
    private TenantRoleMapper tenantRoleMapper;

    @Mock
    private AuditTrailService auditTrailService;

    @Mock
    private TenantOwnershipPolicy tenantOwnershipPolicy;

    @Mock
    private TenantAuthorizationRevisionService revisionService;

    @Mock
    private TenantPlanEnforcementService tenantPlanEnforcementService;

    @Mock
    private SystemPermissionRepository systemPermissionRepository;

    private UpdateTenantRoleUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateTenantRoleUseCase(
                tenantRoleRepository,
                tenantRolePermissionRepository,
                permissionAssignmentPolicy,
                tenantRoleMapper,
                auditTrailService,
                tenantOwnershipPolicy,
                revisionService,
                tenantPlanEnforcementService,
                systemPermissionRepository
        );
    }

    @Test
    @DisplayName("Criterio 4: Permite consultar y modificar los datos y los permisos asociados a roles existentes")
    void shouldUpdateRoleDetailsAndPermissionsSuccessfully() {
        UUID roleId = UUID.randomUUID();
        TenantRole existingRole = new TenantRole(
                roleId,
                "ROL-00001",
                "Supervisor",
                "Descripción previa",
                false,
                true,
                Instant.now(),
                Instant.now(),
                TenantRoleType.INTERNAL
        );

        when(tenantRoleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(tenantRoleRepository.findByName("Supervisor General")).thenReturn(Optional.empty());
        when(tenantRolePermissionRepository.findPermissionCodesByRoleId(roleId)).thenReturn(Set.of("employees.list"));
        when(systemPermissionRepository.findAllActiveTenant()).thenReturn(List.of(
                new SystemPermission("employees.list", "employees", "Listar", PermissionScope.TENANT, true, true),
                new SystemPermission("employees.detail", "employees", "Detalle", PermissionScope.TENANT, true, true)
        ));

        TenantRole updatedRole = new TenantRole(
                roleId,
                "ROL-00001",
                "Supervisor General",
                "Nueva descripción",
                false,
                true,
                existingRole.createdAt(),
                Instant.now(),
                TenantRoleType.INTERNAL
        );
        when(tenantRoleRepository.save(any(TenantRole.class))).thenReturn(updatedRole);

        TenantRoleResponse response = new TenantRoleResponse(
                roleId,
                "ROL-00001",
                "Supervisor General",
                "Nueva descripción",
                TenantRoleType.INTERNAL,
                false,
                true,
                existingRole.createdAt(),
                Instant.now(),
                List.of("employees.list", "employees.detail")
        );
        when(tenantRoleMapper.toResponse(eq(updatedRole), anyList())).thenReturn(response);

        UpdateTenantRoleRequest request = new UpdateTenantRoleRequest(
                "Supervisor General",
                "Nueva descripción",
                List.of("employees.list", "employees.detail")
        );

        TenantRoleResponse result = useCase.execute(roleId, request);

        assertNotNull(result);
        assertEquals("Supervisor General", result.name());
        verify(tenantOwnershipPolicy).assertRoleIsCustom(existingRole);
        verify(tenantRolePermissionRepository).replacePermissions(roleId, List.of("employees.list", "employees.detail"));
        verify(revisionService).incrementAuthorizationVersion();
        verify(auditTrailService).recordTenantEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Criterio 3: Valida que no se dupliquen nombres de rol al modificar uno existente")
    void shouldRejectUpdateWhenRenamingToExistingRoleName() {
        UUID roleId = UUID.randomUUID();
        UUID otherRoleId = UUID.randomUUID();

        TenantRole existingRole = new TenantRole(
                roleId,
                "ROL-00001",
                "Rol Actual",
                "Desc",
                false,
                true,
                Instant.now(),
                Instant.now(),
                TenantRoleType.INTERNAL
        );

        TenantRole anotherRole = new TenantRole(
                otherRoleId,
                "ROL-00002",
                "Rol Existente",
                "Desc",
                false,
                true,
                Instant.now(),
                Instant.now(),
                TenantRoleType.INTERNAL
        );

        when(tenantRoleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(tenantRoleRepository.findByName("Rol Existente")).thenReturn(Optional.of(anotherRole));

        UpdateTenantRoleRequest request = new UpdateTenantRoleRequest(
                "Rol Existente",
                "Nueva Desc",
                List.of("users.list")
        );

        TenantRoleAlreadyExistsException ex = assertThrows(TenantRoleAlreadyExistsException.class, () -> {
            useCase.execute(roleId, request);
        });

        assertTrue(ex.getMessage().contains("Rol Existente"));
        verify(tenantRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 6: Impide modificar permisos asignando módulos excluidos del plan activo")
    void shouldRejectUpdateWhenAssigningPermissionsExcludedByPlan() {
        UUID roleId = UUID.randomUUID();
        TenantRole existingRole = new TenantRole(
                roleId,
                "ROL-00001",
                "Rol Interno",
                "Desc",
                false,
                true,
                Instant.now(),
                Instant.now(),
                TenantRoleType.INTERNAL
        );

        when(tenantRoleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(tenantRoleRepository.findByName("Rol Interno")).thenReturn(Optional.of(existingRole));
        when(systemPermissionRepository.findAllActiveTenant()).thenReturn(List.of(
                new SystemPermission("backups.create", "backups", "Respaldos", PermissionScope.TENANT, true, true)
        ));

        doThrow(new TenantSubscriptionAccessDeniedException("El módulo 'backups' no está incluido en tu plan activo"))
                .when(tenantPlanEnforcementService).assertCanAssignPermissions(eq(List.of("backups.create")), anyMap());

        UpdateTenantRoleRequest request = new UpdateTenantRoleRequest(
                "Rol Interno",
                "Desc",
                List.of("backups.create")
        );

        TenantSubscriptionAccessDeniedException ex = assertThrows(TenantSubscriptionAccessDeniedException.class, () -> {
            useCase.execute(roleId, request);
        });

        assertTrue(ex.getMessage().contains("backups"));
        verify(tenantRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 4: Bloquea la modificación del rol de sistema OWNER_ADMIN")
    void shouldPreventModifyingSystemRole() {
        UUID ownerRoleId = UUID.randomUUID();
        TenantRole rootRole = new TenantRole(
                ownerRoleId,
                "OWNER_ADMIN",
                "Propietario",
                "System Root Role",
                true,
                true,
                Instant.now(),
                Instant.now(),
                TenantRoleType.ROOT
        );

        when(tenantRoleRepository.findById(ownerRoleId)).thenReturn(Optional.of(rootRole));
        doThrow(new IllegalArgumentException("System roles cannot be modified"))
                .when(tenantOwnershipPolicy).assertRoleIsCustom(rootRole);

        UpdateTenantRoleRequest request = new UpdateTenantRoleRequest(
                "Nuevo Nombre Root",
                "Desc",
                List.of("access.permissions.read")
        );

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(ownerRoleId, request));
        verify(tenantRoleRepository, never()).save(any());
    }
}
