package com.hrcoresystem.hrcore_api.modules.identity.users.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.TenantRoleAssignmentService;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase.IssueAccountActivationUseCase;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.CreateTenantUserRequest;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.TenantUserResponse;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.mapper.TenantUserMapper;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception.TenantUserAlreadyExistsException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception.TenantUserNotFoundException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.exception.TenantSubscriptionAccessDeniedException;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence.EmployeeEntity;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence.EmployeeJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CreateTenantUserUseCaseTest {

    private TenantUserRepository tenantUserRepository;
    private TenantUserMapper tenantUserMapper;
    private PasswordEncoder passwordEncoder;
    private AuditTrailService auditTrailService;
    private TenantPlanEnforcementService tenantPlanEnforcementService;
    private IssueAccountActivationUseCase issueAccountActivationUseCase;
    private TenantRoleAssignmentService roleAssignmentService;
    private TenantOwnershipPolicy ownershipPolicy;
    private EmployeeJpaRepository employeeJpaRepository;

    private CreateTenantUserUseCase useCase;

    @BeforeEach
    void setUp() {
        tenantUserRepository = mock(TenantUserRepository.class);
        employeeJpaRepository = mock(EmployeeJpaRepository.class);
        tenantUserMapper = new TenantUserMapper(employeeJpaRepository);
        passwordEncoder = mock(PasswordEncoder.class);
        auditTrailService = mock(AuditTrailService.class);
        tenantPlanEnforcementService = mock(TenantPlanEnforcementService.class);
        issueAccountActivationUseCase = mock(IssueAccountActivationUseCase.class);
        roleAssignmentService = mock(TenantRoleAssignmentService.class);
        ownershipPolicy = mock(TenantOwnershipPolicy.class);

        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pwd");
        when(roleAssignmentService.assignInitialRoles(any(), any())).thenReturn(List.of());

        useCase = new CreateTenantUserUseCase(
                tenantUserRepository,
                tenantUserMapper,
                passwordEncoder,
                auditTrailService,
                tenantPlanEnforcementService,
                issueAccountActivationUseCase,
                roleAssignmentService,
                ownershipPolicy,
                employeeJpaRepository
        );
    }

    @Test
    @DisplayName("Criterio 1: Registra usuario con displayName, estado inicial y vinculación a empleado")
    void shouldRegisterUserWithDisplayNameInitialStatusAndEmployeeLink() {
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity employee = new EmployeeEntity();
        employee.setId(employeeId);
        employee.setEmployeeNumber("EMP-100");
        employee.setFirstName("Marcos");
        employee.setLastName("Vargas");
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);

        when(employeeJpaRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeJpaRepository.findByTenantUserId(any())).thenReturn(Optional.of(employee));
        when(tenantUserRepository.existsByEmail("marcos@empresa.com")).thenReturn(false);
        when(tenantUserRepository.countActiveAndPendingUsers()).thenReturn(3L);

        when(tenantUserRepository.save(any(TenantUser.class))).thenAnswer(invocation -> {
            TenantUser arg = invocation.getArgument(0);
            return new TenantUser(
                    UUID.randomUUID(),
                    arg.email(),
                    arg.passwordHash(),
                    arg.firstName(),
                    arg.lastName(),
                    arg.active(),
                    arg.status(),
                    arg.securityVersion(),
                    java.time.Instant.now(),
                    java.time.Instant.now()
            );
        });

        CreateTenantUserRequest request = new CreateTenantUserRequest(
                "marcos@empresa.com",
                "Password123!",
                null,
                null,
                "Marcos Vargas",
                TenantUserStatus.ACTIVE,
                employeeId,
                List.of()
        );

        TenantUserResponse response = useCase.execute(request);

        assertNotNull(response);
        assertEquals("marcos@empresa.com", response.email());
        assertEquals("Marcos Vargas", response.displayName());
        assertTrue(response.active());
        assertEquals("ACTIVE", response.status());
        assertEquals(employeeId, response.employeeId());
        assertEquals("EMP-100", response.employeeNumber());

        verify(employeeJpaRepository).save(employee);
        assertEquals(response.id(), employee.getTenantUserId());
        verify(issueAccountActivationUseCase, never()).execute(anyString());
    }

    @Test
    @DisplayName("Criterio 2: Valida que la creación no supere el límite permitido por el plan")
    void shouldEnforcePlanUserLimitOnCreation() {
        when(tenantUserRepository.existsByEmail("nuevo@empresa.com")).thenReturn(false);
        when(tenantUserRepository.countActiveAndPendingUsers()).thenReturn(10L);

        doThrow(new TenantSubscriptionAccessDeniedException("Plan limit reached"))
                .when(tenantPlanEnforcementService).assertCanCreateUser(10L);

        CreateTenantUserRequest request = new CreateTenantUserRequest(
                "nuevo@empresa.com",
                "Password123!",
                "Ana",
                "Gómez",
                List.of()
        );

        assertThrows(TenantSubscriptionAccessDeniedException.class, () -> useCase.execute(request));
        verify(tenantUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza la vinculación si el empleado ya está asociado a otra cuenta")
    void shouldRejectIfEmployeeAlreadyLinked() {
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity employee = new EmployeeEntity();
        employee.setId(employeeId);
        employee.setTenantUserId(UUID.randomUUID()); // Ya tiene usuario

        when(employeeJpaRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(tenantUserRepository.existsByEmail("otro@empresa.com")).thenReturn(false);

        CreateTenantUserRequest request = new CreateTenantUserRequest(
                "otro@empresa.com",
                "Password123!",
                "Otro",
                "Usuario",
                "Otro Usuario",
                TenantUserStatus.ACTIVE,
                employeeId,
                List.of()
        );

        assertThrows(TenantUserAlreadyExistsException.class, () -> useCase.execute(request));
    }
}
