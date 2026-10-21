package com.hrcoresystem.hrcore_api.modules.identity.users.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.TenantUserResponse;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.UpdateTenantUserRequest;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.mapper.TenantUserMapper;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception.TenantUserAlreadyExistsException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence.EmployeeEntity;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence.EmployeeJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateTenantUserUseCaseTest {

    private TenantUserRepository tenantUserRepository;
    private TenantUserMapper tenantUserMapper;
    private AuditTrailService auditTrailService;
    private TenantAuthorizationStateRepository authorizationStateRepository;
    private EmployeeJpaRepository employeeJpaRepository;

    private UpdateTenantUserUseCase useCase;

    @BeforeEach
    void setUp() {
        tenantUserRepository = mock(TenantUserRepository.class);
        employeeJpaRepository = mock(EmployeeJpaRepository.class);
        tenantUserMapper = new TenantUserMapper(employeeJpaRepository);
        auditTrailService = mock(AuditTrailService.class);
        authorizationStateRepository = mock(TenantAuthorizationStateRepository.class);

        when(authorizationStateRepository.ownerUserId()).thenReturn(UUID.randomUUID());

        useCase = new UpdateTenantUserUseCase(
                tenantUserRepository,
                tenantUserMapper,
                auditTrailService,
                authorizationStateRepository,
                employeeJpaRepository
        );
    }

    @Test
    @DisplayName("Criterio 4: Modifica la información autorizada de un usuario y actualiza la vinculación con un empleado")
    void shouldUpdateAuthorizedInfoAndEmployeeLink() {
        UUID userId = UUID.randomUUID();
        UUID oldEmployeeId = UUID.randomUUID();
        UUID newEmployeeId = UUID.randomUUID();

        TenantUser existing = new TenantUser(
                userId,
                "viejo@empresa.com",
                "hashed",
                "Carlos",
                "Viejo",
                true,
                TenantUserStatus.ACTIVE,
                1,
                Instant.now(),
                Instant.now()
        );

        EmployeeEntity oldEmp = new EmployeeEntity();
        oldEmp.setId(oldEmployeeId);
        oldEmp.setTenantUserId(userId);

        EmployeeEntity newEmp = new EmployeeEntity();
        newEmp.setId(newEmployeeId);
        newEmp.setEmployeeNumber("EMP-200");
        newEmp.setFirstName("Carlos");
        newEmp.setLastName("Nuevo");

        when(tenantUserRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(tenantUserRepository.findByEmail("nuevo@empresa.com")).thenReturn(Optional.empty());
        when(employeeJpaRepository.findByTenantUserId(userId)).thenReturn(Optional.of(oldEmp), Optional.of(newEmp));
        when(employeeJpaRepository.findById(newEmployeeId)).thenReturn(Optional.of(newEmp));

        when(tenantUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTenantUserRequest request = new UpdateTenantUserRequest(
                "nuevo@empresa.com",
                "Carlos",
                "Nuevo",
                "Carlos Nuevo",
                newEmployeeId
        );

        TenantUserResponse response = useCase.execute(userId, request);

        assertEquals("nuevo@empresa.com", response.email());
        assertEquals("Carlos Nuevo", response.displayName());
        assertEquals(newEmployeeId, response.employeeId());
        assertEquals("EMP-200", response.employeeNumber());

        assertNull(oldEmp.getTenantUserId(), "El empleado anterior debe quedar desvinculado");
        assertEquals(userId, newEmp.getTenantUserId(), "El nuevo empleado debe quedar vinculado");
        verify(employeeJpaRepository).save(oldEmp);
        verify(employeeJpaRepository).save(newEmp);
    }

    @Test
    @DisplayName("Rechaza modificación si el correo nuevo ya pertenece a otro usuario")
    void shouldRejectWhenEmailBelongsToAnotherUser() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        TenantUser existing = new TenantUser(
                userId,
                "usuario1@empresa.com",
                "hashed",
                "User",
                "One",
                true,
                TenantUserStatus.ACTIVE,
                1,
                Instant.now(),
                Instant.now()
        );

        TenantUser otherUser = new TenantUser(
                otherUserId,
                "existente@empresa.com",
                "hashed",
                "User",
                "Two",
                true,
                TenantUserStatus.ACTIVE,
                1,
                Instant.now(),
                Instant.now()
        );

        when(tenantUserRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(tenantUserRepository.findByEmail("existente@empresa.com")).thenReturn(Optional.of(otherUser));

        UpdateTenantUserRequest request = new UpdateTenantUserRequest(
                "existente@empresa.com",
                "User",
                "Updated"
        );

        assertThrows(TenantUserAlreadyExistsException.class, () -> useCase.execute(userId, request));
    }
}
