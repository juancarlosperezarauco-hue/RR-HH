package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.TenantRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.TenantRoleMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRoleType;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRolePermissionRepository;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTenantRolesUseCaseTest {

    @Mock
    private TenantRoleRepository tenantRoleRepository;

    @Mock
    private TenantRolePermissionRepository tenantRolePermissionRepository;

    @Mock
    private TenantRoleMapper tenantRoleMapper;

    private ListTenantRolesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListTenantRolesUseCase(
                tenantRoleRepository,
                tenantRolePermissionRepository,
                tenantRoleMapper
        );
    }

    @Test
    @DisplayName("Criterios 1 y 4: Permite consultar el listado de roles con sus permisos asociados en el tenant activo")
    void shouldListTenantRolesForActiveTenant() {
        UUID roleId = UUID.randomUUID();
        TenantRole role = new TenantRole(
                roleId,
                "ROL-00001",
                "Analista de Contratos",
                "Gestión de contratos",
                false,
                true,
                Instant.now(),
                Instant.now(),
                TenantRoleType.INTERNAL
        );

        when(tenantRoleRepository.findAll()).thenReturn(List.of(role));
        when(tenantRolePermissionRepository.findPermissionCodesByRoleId(roleId)).thenReturn(Set.of("contracts.create", "contracts.list"));

        TenantRoleResponse response = new TenantRoleResponse(
                roleId,
                "ROL-00001",
                "Analista de Contratos",
                "Gestión de contratos",
                TenantRoleType.INTERNAL,
                false,
                true,
                Instant.now(),
                Instant.now(),
                List.of("contracts.create", "contracts.list")
        );
        when(tenantRoleMapper.toResponse(eq(role), anyList())).thenReturn(response);

        List<TenantRoleResponse> result = useCase.execute();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Analista de Contratos", result.getFirst().name());
        assertEquals(2, result.getFirst().permissionCodes().size());
    }
}
