package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.CreateTenantRoleRequest;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.TenantRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.TenantRoleMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.PermissionAssignmentPolicy;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateTenantRoleUseCaseTest {

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
    private TenantPlanEnforcementService tenantPlanEnforcementService;

    @Mock
    private TenantOwnershipPolicy tenantOwnershipPolicy;

    @Mock
    private SystemPermissionRepository systemPermissionRepository;

    private CreateTenantRoleUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateTenantRoleUseCase(
                tenantRoleRepository,
                tenantRolePermissionRepository,
                permissionAssignmentPolicy,
                tenantRoleMapper,
                auditTrailService,
                tenantPlanEnforcementService,
                tenantOwnershipPolicy,
                systemPermissionRepository
        );
    }

    @Test
    @DisplayName("Criterios 1 y 2: Permite crear un rol personalizado definiendo código, nombre, descripción y permisos")
    void shouldCreateCustomRoleSuccessfullyWithCodeAndPermissions() {
        CreateTenantRoleRequest request = new CreateTenantRoleRequest(
                "ROL-00005",
                "Gestor de Personal",
                "Encargado de contrataciones y expedientes",
                List.of("employees.create", "employees.list")
        );

        when(tenantRoleRepository.existsByName("Gestor de Personal")).thenReturn(false);
        when(tenantRoleRepository.countActiveCustomRoles()).thenReturn(2L);
        when(systemPermissionRepository.findAllActiveTenant()).thenReturn(List.of(
                new SystemPermission("employees.create", "employees", "Crear", PermissionScope.TENANT, true, true),
                new SystemPermission("employees.list", "employees", "Listar", PermissionScope.TENANT, true, true)
        ));

        UUID roleId = UUID.randomUUID();
        TenantRole savedRole = new TenantRole(
                roleId,
                "ROL-00005",
                "Gestor de Personal",
                "Encargado de contrataciones y expedientes",
                false,
                true,
                Instant.now(),
                Instant.now(),
                TenantRoleType.INTERNAL
        );

        when(tenantRoleRepository.save(any(TenantRole.class))).thenReturn(savedRole);

        TenantRoleResponse expectedResponse = new TenantRoleResponse(
                roleId,
                "ROL-00005",
                "Gestor de Personal",
                "Encargado de contrataciones y expedientes",
                TenantRoleType.INTERNAL,
                false,
                true,
                Instant.now(),
                Instant.now(),
                List.of("employees.create", "employees.list")
        );
        when(tenantRoleMapper.toResponse(eq(savedRole), anyList())).thenReturn(expectedResponse);

        TenantRoleResponse result = useCase.execute(request);

        assertNotNull(result);
        assertEquals("Gestor de Personal", result.name());
        assertEquals("ROL-00005", result.code());
        assertTrue(result.active());
        assertFalse(result.systemRole());

        verify(permissionAssignmentPolicy).validate(List.of("employees.create", "employees.list"));
        verify(tenantPlanEnforcementService).assertCanCreateRole(2L);
        verify(tenantPlanEnforcementService).assertCanAssignPermissions(eq(List.of("employees.create", "employees.list")), anyMap());
        verify(tenantRolePermissionRepository).replacePermissions(roleId, List.of("employees.create", "employees.list"));
        verify(auditTrailService).recordTenantEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Criterio 3: Valida que no se registren roles con nombres duplicados dentro del mismo tenant")
    void shouldRejectCreationWhenRoleNameIsDuplicated() {
        CreateTenantRoleRequest request = new CreateTenantRoleRequest(
                "ROL-00006",
                "Gestor de Personal",
                "Descripción duplicada",
                List.of("employees.list")
        );

        when(tenantRoleRepository.existsByName("Gestor de Personal")).thenReturn(true);

        TenantRoleAlreadyExistsException ex = assertThrows(TenantRoleAlreadyExistsException.class, () -> {
            useCase.execute(request);
        });

        assertTrue(ex.getMessage().contains("Gestor de Personal"));
        verify(tenantRoleRepository, never()).save(any());
        verify(tenantRolePermissionRepository, never()).replacePermissions(any(), any());
    }

    @Test
    @DisplayName("Criterio 6: Impide asignar permisos sobre módulos que no estén incluidos en el plan activo de la organización")
    void shouldRejectCreationWhenPermissionsBelongToExcludedPlanModules() {
        CreateTenantRoleRequest request = new CreateTenantRoleRequest(
                "ROL-00007",
                "Auditor de Incidencias",
                "Rol para plan no autorizado",
                List.of("incidents.create")
        );

        when(tenantRoleRepository.existsByName("Auditor de Incidencias")).thenReturn(false);
        when(systemPermissionRepository.findAllActiveTenant()).thenReturn(List.of(
                new SystemPermission("incidents.create", "incidents", "Crear Incidencia", PermissionScope.TENANT, true, true)
        ));

        doThrow(new TenantSubscriptionAccessDeniedException("El módulo 'incidents' no está incluido en el plan activo 'BASIC'"))
                .when(tenantPlanEnforcementService).assertCanAssignPermissions(eq(List.of("incidents.create")), anyMap());

        TenantSubscriptionAccessDeniedException ex = assertThrows(TenantSubscriptionAccessDeniedException.class, () -> {
            useCase.execute(request);
        });

        assertTrue(ex.getMessage().contains("incidents"));
        verify(tenantRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 6: Valida los límites de roles permitidos por el plan de suscripción activo")
    void shouldEnforcePlanMaxRolesLimit() {
        CreateTenantRoleRequest request = new CreateTenantRoleRequest(
                "ROL-00008",
                "Rol Límite Superado",
                "Descripción",
                List.of("users.list")
        );

        when(tenantRoleRepository.existsByName("Rol Límite Superado")).thenReturn(false);
        when(tenantRoleRepository.countActiveCustomRoles()).thenReturn(5L);

        doThrow(new TenantSubscriptionAccessDeniedException("Your current plan allows a maximum of 5 active custom roles"))
                .when(tenantPlanEnforcementService).assertCanCreateRole(5L);

        TenantSubscriptionAccessDeniedException ex = assertThrows(TenantSubscriptionAccessDeniedException.class, () -> {
            useCase.execute(request);
        });

        assertTrue(ex.getMessage().contains("maximum of 5 active custom roles"));
        verify(tenantRoleRepository, never()).save(any());
    }
}
