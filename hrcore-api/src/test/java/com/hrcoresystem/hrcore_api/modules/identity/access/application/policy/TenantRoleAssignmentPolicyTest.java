package com.hrcoresystem.hrcore_api.modules.identity.access.application.policy;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRoleType;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantRoleAssignmentPolicyTest {

    @Mock
    private TenantRoleRepository repo;

    @Mock
    private TenantOwnershipPolicy ownership;

    private TenantRoleAssignmentPolicy policy;

    private final UUID roleId1 = UUID.randomUUID();
    private final UUID roleId2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        policy = new TenantRoleAssignmentPolicy(repo, ownership);
    }

    @Test
    @DisplayName("Criterio 4: Debe fallar si se intenta asignar un rol inactivo")
    void shouldFailWhenRoleIsInactive() {
        TenantRole inactiveRole = new TenantRole(
                roleId1, "ROL-00001", "Inactive Role", "Desc",
                false, false, Instant.now(), Instant.now(), TenantRoleType.INTERNAL
        );

        when(repo.findAllByIds(List.of(roleId1))).thenReturn(List.of(inactiveRole));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> policy.validateAssignableRoleIds(List.of(roleId1))
        );

        assertTrue(exception.getMessage().contains("porque se encuentra inactivo"));
        verify(ownership, never()).assertRoleAssignable(any());
    }

    @Test
    @DisplayName("Criterio 4: Debe fallar si un rol no existe o pertenece a otro tenant")
    void shouldFailWhenRoleDoesNotExistInCurrentTenant() {
        when(repo.findAllByIds(List.of(roleId1, roleId2))).thenReturn(List.of());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> policy.validateAssignableRoleIds(List.of(roleId1, roleId2))
        );

        assertEquals("Uno o más roles no existen en la organización", exception.getMessage());
    }

    @Test
    @DisplayName("Criterio 4: Debe fallar si se intenta asignar el rol reservado del sistema (ROOT/OWNER_ADMIN)")
    void shouldFailWhenAttemptingToAssignSystemRole() {
        TenantRole systemRole = new TenantRole(
                roleId1, "OWNER_ADMIN", "Owner Admin", "Desc",
                true, true, Instant.now(), Instant.now(), TenantRoleType.ROOT
        );

        when(repo.findAllByIds(List.of(roleId1))).thenReturn(List.of(systemRole));

        assertThrows(
                AccessDeniedException.class,
                () -> policy.validateAssignableRoleIds(List.of(roleId1))
        );
    }

    @Test
    @DisplayName("Debe fallar si los IDs de roles son nulos o contienen duplicados")
    void shouldFailWhenRoleIdsContainDuplicates() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> policy.validateAssignableRoleIds(List.of(roleId1, roleId1))
        );

        assertEquals("Role ids must be unique and non-null", exception.getMessage());
    }

    @Test
    @DisplayName("Criterio 2: Debe validar y retornar lista de roles activos asignables")
    void shouldValidateAndReturnActiveAssignableRoles() {
        TenantRole role = new TenantRole(
                roleId1, "ROL-00001", "Supervisor", "Desc",
                false, true, Instant.now(), Instant.now(), TenantRoleType.INTERNAL
        );

        when(repo.findAllByIds(List.of(roleId1))).thenReturn(List.of(role));

        List<TenantRole> result = policy.validateAssignableRoleIds(List.of(roleId1));

        assertEquals(1, result.size());
        assertEquals(roleId1, result.get(0).id());
        verify(ownership).assertRoleAssignable(role);
    }
}
