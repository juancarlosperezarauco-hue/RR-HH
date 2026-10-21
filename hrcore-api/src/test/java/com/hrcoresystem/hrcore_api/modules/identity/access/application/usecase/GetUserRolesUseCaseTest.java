package com.hrcoresystem.hrcore_api.modules.identity.access.application.usecase;

import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.TenantRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.UserRolesResponse;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.mapper.TenantRoleMapper;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
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
class GetUserRolesUseCaseTest {

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
    private TenantOwnershipPolicy ownershipPolicy;

    private GetUserRolesUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new GetUserRolesUseCase(
                tenantUserRepository,
                tenantRoleRepository,
                tenantRolePermissionRepository,
                tenantUserRoleRepository,
                tenantRoleMapper,
                ownershipPolicy
        );
    }

    @Test
    @DisplayName("Debe consultar exitosamente los roles asignados a un usuario del tenant")
    void shouldSuccessfullyReturnUserRoles() {
        TenantUser user = new TenantUser(
                userId, "user@example.com", "hash", "John", "Doe",
                true, TenantUserStatus.ACTIVE, 1L, Instant.now(), Instant.now()
        );
        TenantRole role = new TenantRole(
                roleId, "ROL-00001", "Supervisor", "Rol operativo",
                false, true, Instant.now(), Instant.now(), TenantRoleType.INTERNAL
        );
        TenantRoleResponse roleResponse = new TenantRoleResponse(
                roleId, "ROL-00001", "Supervisor", "Rol operativo",
                TenantRoleType.INTERNAL, false, true, Instant.now(), Instant.now(), List.of("employees.list")
        );

        when(tenantUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tenantUserRoleRepository.findRoleIdsByUserId(userId)).thenReturn(List.of(roleId));
        when(tenantRoleRepository.findAllByIds(List.of(roleId))).thenReturn(List.of(role));
        when(tenantRolePermissionRepository.findPermissionCodesByRoleId(roleId)).thenReturn(Set.of("employees.list"));
        when(tenantRoleMapper.toResponse(eq(role), anyList())).thenReturn(roleResponse);

        UserRolesResponse response = useCase.execute(userId);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals(1, response.roles().size());
        assertEquals("Supervisor", response.roles().get(0).name());
        verify(ownershipPolicy).requireCurrentOwner();
        verify(tenantUserRepository).findById(userId);
    }

    @Test
    @DisplayName("Debe fallar al consultar roles si el usuario no existe en el tenant")
    void shouldThrowWhenUserNotFound() {
        when(tenantUserRepository.findById(userId)).thenReturn(Optional.empty());

        TenantUserNotFoundException exception = assertThrows(
                TenantUserNotFoundException.class,
                () -> useCase.execute(userId)
        );

        assertTrue(exception.getMessage().contains("Tenant user not found with id: " + userId));
        verify(tenantUserRoleRepository, never()).findRoleIdsByUserId(any());
    }

    @Test
    @DisplayName("Debe fallar si quien consulta no posee privilegios de propietario/administrador")
    void shouldRequireCurrentOwnerPrivileges() {
        doThrow(new AccessDeniedException("Current tenant owner required"))
                .when(ownershipPolicy).requireCurrentOwner();

        assertThrows(AccessDeniedException.class, () -> useCase.execute(userId));
        verify(tenantUserRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Debe retornar lista vacía si el usuario no tiene roles asignados")
    void shouldReturnEmptyRolesWhenUserHasNoAssignedRoles() {
        TenantUser user = new TenantUser(
                userId, "user@example.com", "hash", "Jane", "Doe",
                true, TenantUserStatus.ACTIVE, 1L, Instant.now(), Instant.now()
        );

        when(tenantUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tenantUserRoleRepository.findRoleIdsByUserId(userId)).thenReturn(List.of());
        when(tenantRoleRepository.findAllByIds(List.of())).thenReturn(List.of());

        UserRolesResponse response = useCase.execute(userId);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertTrue(response.roles().isEmpty());
    }
}
