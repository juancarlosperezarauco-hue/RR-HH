package com.hrcoresystem.hrcore_api.modules.identity.users.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase.IssueAccountActivationUseCase;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.CreateTenantUserRequest;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.TenantUserResponse;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.mapper.TenantUserMapper;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception.TenantUserAlreadyExistsException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception.TenantUserNotFoundException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.AssignedRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.TenantRoleAssignmentService;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.policy.TenantOwnershipPolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence.EmployeeEntity;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence.EmployeeJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CreateTenantUserUseCase {

    private final TenantUserRepository tenantUserRepository;
    private final TenantUserMapper tenantUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditTrailService auditTrailService;
    private final TenantPlanEnforcementService tenantPlanEnforcementService;
    private final IssueAccountActivationUseCase issueAccountActivationUseCase;
    private final TenantRoleAssignmentService roleAssignmentService;
    private final TenantOwnershipPolicy ownershipPolicy;
    private final EmployeeJpaRepository employeeJpaRepository;

    @Autowired
    public CreateTenantUserUseCase(
            TenantUserRepository tenantUserRepository,
            TenantUserMapper tenantUserMapper,
            PasswordEncoder passwordEncoder,
            AuditTrailService auditTrailService,
            TenantPlanEnforcementService tenantPlanEnforcementService,
            IssueAccountActivationUseCase issueAccountActivationUseCase,
            TenantRoleAssignmentService roleAssignmentService,
            TenantOwnershipPolicy ownershipPolicy,
            @Autowired(required = false) EmployeeJpaRepository employeeJpaRepository
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.tenantUserMapper = tenantUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditTrailService = auditTrailService;
        this.tenantPlanEnforcementService = tenantPlanEnforcementService;
        this.issueAccountActivationUseCase = issueAccountActivationUseCase;
        this.roleAssignmentService = roleAssignmentService;
        this.ownershipPolicy = ownershipPolicy;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    public CreateTenantUserUseCase(
            TenantUserRepository tenantUserRepository,
            TenantUserMapper tenantUserMapper,
            PasswordEncoder passwordEncoder,
            AuditTrailService auditTrailService,
            TenantPlanEnforcementService tenantPlanEnforcementService,
            IssueAccountActivationUseCase issueAccountActivationUseCase,
            TenantRoleAssignmentService roleAssignmentService,
            TenantOwnershipPolicy ownershipPolicy
    ) {
        this(tenantUserRepository, tenantUserMapper, passwordEncoder, auditTrailService,
                tenantPlanEnforcementService, issueAccountActivationUseCase, roleAssignmentService,
                ownershipPolicy, null);
    }

    @Transactional
    public TenantUserResponse execute(CreateTenantUserRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        List<UUID> roleIds = request.roleIds() == null ? List.of() : request.roleIds();
        if (!roleIds.isEmpty()) {
            ownershipPolicy.requireCurrentOwner();
        }

        if (tenantUserRepository.existsByEmail(normalizedEmail)) {
            throw new TenantUserAlreadyExistsException(
                    "A tenant user with email '" + normalizedEmail + "' already exists"
            );
        }

        tenantPlanEnforcementService.assertCanCreateUser(
                tenantUserRepository.countActiveAndPendingUsers()
        );

        String firstName = normalizeNullable(request.firstName());
        String lastName = normalizeNullable(request.lastName());
        String displayName = normalizeNullable(request.displayName());

        if (displayName != null && (firstName == null || firstName.isBlank())) {
            String[] parts = displayName.trim().split("\\s+", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : "";
        }

        EmployeeEntity employeeToLink = null;
        if (request.employeeId() != null && employeeJpaRepository != null) {
            employeeToLink = employeeJpaRepository.findById(request.employeeId())
                    .orElseThrow(() -> new TenantUserNotFoundException(
                            "Empleado no encontrado con ID: " + request.employeeId()
                    ));

            if (employeeToLink.getTenantUserId() != null) {
                throw new TenantUserAlreadyExistsException(
                        "El empleado ya se encuentra vinculado a una cuenta de usuario"
                );
            }

            if (firstName == null || firstName.isBlank()) {
                firstName = employeeToLink.getFirstName();
                lastName = employeeToLink.getLastName();
            }
        }

        TenantUserStatus initialStatus = request.status() != null ? request.status() : TenantUserStatus.PENDING;
        boolean initialActive = (initialStatus == TenantUserStatus.ACTIVE);

        String rawPassword = request.password() != null && !request.password().isBlank()
                ? request.password()
                : UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "Aa1!";

        TenantUser tenantUserToCreate = new TenantUser(
                null,
                normalizedEmail,
                passwordEncoder.encode(rawPassword),
                firstName,
                lastName,
                initialActive,
                initialStatus,
                1,
                null,
                null
        );

        TenantUser createdTenantUser = tenantUserRepository.save(tenantUserToCreate);

        if (employeeToLink != null) {
            employeeToLink.setTenantUserId(createdTenantUser.id());
            employeeJpaRepository.save(employeeToLink);
        }

        var initialRoles = roleAssignmentService.assignInitialRoles(createdTenantUser.id(), roleIds);

        if (initialStatus == TenantUserStatus.PENDING) {
            issueAccountActivationUseCase.execute(createdTenantUser.email());
        }

        auditTrailService.recordTenantEvent(
                AuditEventTypes.USER_CREATED,
                "USER",
                createdTenantUser.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "CREATE_USER",
                        "email", createdTenantUser.email(),
                        "firstName", createdTenantUser.firstName(),
                        "lastName", createdTenantUser.lastName(),
                        "status", createdTenantUser.status().name(),
                        "active", createdTenantUser.active(),
                        "linkedEmployeeId", employeeToLink != null ? employeeToLink.getId() : null,
                        "defaultRole", "NONE",
                        "initialRoleIds", roleIds,
                        "initialRoleCodes", initialRoles.stream().map(r -> r.code()).toList(),
                        "initialRoleCount", initialRoles.size()
                ),
                null,
                IdentityAuditPayloads.userState(
                        createdTenantUser.email(),
                        createdTenantUser.firstName(),
                        createdTenantUser.lastName(),
                        createdTenantUser.active(),
                        createdTenantUser.status().name()
                )
        );

        List<AssignedRoleResponse> assignedRoleResponses = initialRoles.stream()
                .map(role -> new AssignedRoleResponse(role.id(), role.code(), role.name(), role.active()))
                .toList();
        return tenantUserMapper.toResponse(createdTenantUser, assignedRoleResponses);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
