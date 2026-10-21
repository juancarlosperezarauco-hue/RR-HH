package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.TenantRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.TenantRoleMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.TenantAuthorizationRevisionService;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRoleType;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRolePermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
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
class ActivateDeactivateTenantRoleUseCaseTest {

    @Mock
    private TenantRoleRepository tenantRoleRepository;

    @Mock
    private TenantRolePermissionRepository tenantRolePermissionRepository;

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

    private DeactivateTenantRoleUseCase deactivateUseCase;
    private ActivateTenantRoleUseCase activateUseCase;

    @BeforeEach
    void setUp() {
        deactivateUseCase = new DeactivateTenantRoleUseCase(
                tenantRoleRepository,
                tenantRolePermissionRepository,
                tenantRoleMapper,
                auditTrailService,
                tenantOwnershipPolicy,
                revisionService
        );

        activateUseCase = new ActivateTenantRoleUseCase(
                tenantRoleRepository,
                tenantRolePermissionRepository,
                tenantRoleMapper,
                auditTrailService,
                tenantPlanEnforcementService,
                tenantOwnershipPolicy,
                revisionService
        );
    }

    @Test
    @DisplayName("Criterio 5: Permite desactivar un rol sin eliminar su registro histórico")
    void shouldDeactivateRoleWithoutDeletingHistoricalRecord() {
        UUID roleId = UUID.randomUUID();
        TenantRole activeRole = new TenantRole(
                roleId,
                "ROL-00001",
                "Analista de RRHH",
                "Descripción",
                false,
                true,
                Instant.now(),
                Instant.now(),
                TenantRoleType.INTERNAL
        );

        when(tenantRoleRepository.findById(roleId)).thenReturn(Optional.of(activeRole));
        when(tenantRolePermissionRepository.findPermissionCodesByRoleId(roleId)).thenReturn(Set.of("employees.list"));

        TenantRole deactivatedRole = new TenantRole(
                roleId,
                "ROL-00001",
                "Analista de RRHH",
                "Descripción",
                false,
                false,
                activeRole.createdAt(),
                Instant.now(),
                TenantRoleType.INTERNAL
        );
        when(tenantRoleRepository.save(any(TenantRole.class))).thenReturn(deactivatedRole);

        TenantRoleResponse response = new TenantRoleResponse(
                roleId,
                "ROL-00001",
                "Analista de RRHH",
                "Descripción",
                TenantRoleType.INTERNAL,
                false,
                false,
                activeRole.createdAt(),
                Instant.now(),
                List.of("employees.list")
        );
        when(tenantRoleMapper.toResponse(eq(deactivatedRole), anyList())).thenReturn(response);

        TenantRoleResponse result = deactivateUseCase.execute(roleId);

        assertNotNull(result);
        assertFalse(result.active());
        verify(tenantOwnershipPolicy).assertRoleIsCustom(activeRole);
        verify(tenantRoleRepository).save(argThat(role -> !role.active() && role.id().equals(roleId)));
        verify(revisionService).incrementAuthorizationVersion();
        verify(auditTrailService).recordTenantEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Criterio 5: Permite activar un rol inactivo validando cuota del plan contratado")
    void shouldActivateRoleSuccessfully() {
        UUID roleId = UUID.randomUUID();
        TenantRole inactiveRole = new TenantRole(
                roleId,
                "ROL-00001",
                "Analista de RRHH",
                "Descripción",
                false,
                false,
                Instant.now(),
                Instant.now(),
                TenantRoleType.INTERNAL
        );

        when(tenantRoleRepository.findById(roleId)).thenReturn(Optional.of(inactiveRole));
        when(tenantRoleRepository.countActiveCustomRoles()).thenReturn(3L);
        when(tenantRolePermissionRepository.findPermissionCodesByRoleId(roleId)).thenReturn(Set.of("employees.list"));

        TenantRole activatedRole = new TenantRole(
                roleId,
                "ROL-00001",
                "Analista de RRHH",
                "Descripción",
                false,
                true,
                inactiveRole.createdAt(),
                Instant.now(),
                TenantRoleType.INTERNAL
        );
        when(tenantRoleRepository.save(any(TenantRole.class))).thenReturn(activatedRole);

        TenantRoleResponse response = new TenantRoleResponse(
                roleId,
                "ROL-00001",
                "Analista de RRHH",
                "Descripción",
                TenantRoleType.INTERNAL,
                false,
                true,
                inactiveRole.createdAt(),
                Instant.now(),
                List.of("employees.list")
        );
        when(tenantRoleMapper.toResponse(eq(activatedRole), anyList())).thenReturn(response);

        TenantRoleResponse result = activateUseCase.execute(roleId);

        assertNotNull(result);
        assertTrue(result.active());
        verify(tenantPlanEnforcementService).assertCanActivateRole(3L);
        verify(tenantRoleRepository).save(argThat(role -> role.active() && role.id().equals(roleId)));
        verify(revisionService).incrementAuthorizationVersion();
        verify(auditTrailService).recordTenantEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Criterio 5: Impide desactivar el rol reservado OWNER_ADMIN")
    void shouldPreventDeactivatingOwnerAdminSystemRole() {
        UUID ownerRoleId = UUID.randomUUID();
        TenantRole ownerRole = new TenantRole(
                ownerRoleId,
                "OWNER_ADMIN",
                "Propietario",
                "Root",
                true,
                true,
                Instant.now(),
                Instant.now(),
                TenantRoleType.ROOT
        );

        when(tenantRoleRepository.findById(ownerRoleId)).thenReturn(Optional.of(ownerRole));
        doThrow(new IllegalArgumentException("The system role cannot be deactivated"))
                .when(tenantOwnershipPolicy).assertRoleIsCustom(ownerRole);

        assertThrows(IllegalArgumentException.class, () -> deactivateUseCase.execute(ownerRoleId));
        verify(tenantRoleRepository, never()).save(any());
    }
}
