package com.hrcoresystem.hrcore_api.modules.identity.users.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.TenantUserResponse;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.UpdateTenantUserRequest;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.mapper.TenantUserMapper;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception.TenantUserAlreadyExistsException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception.TenantUserNotFoundException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence.EmployeeEntity;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence.EmployeeJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UpdateTenantUserUseCase {

    private final TenantUserRepository tenantUserRepository;
    private final TenantUserMapper tenantUserMapper;
    private final AuditTrailService auditTrailService;
    private final TenantAuthorizationStateRepository authorizationStateRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    @Autowired
    public UpdateTenantUserUseCase(
            TenantUserRepository tenantUserRepository,
            TenantUserMapper tenantUserMapper,
            AuditTrailService auditTrailService,
            TenantAuthorizationStateRepository authorizationStateRepository,
            @Autowired(required = false) EmployeeJpaRepository employeeJpaRepository
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.tenantUserMapper = tenantUserMapper;
        this.auditTrailService = auditTrailService;
        this.authorizationStateRepository = authorizationStateRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    public UpdateTenantUserUseCase(
            TenantUserRepository tenantUserRepository,
            TenantUserMapper tenantUserMapper,
            AuditTrailService auditTrailService,
            TenantAuthorizationStateRepository authorizationStateRepository
    ) {
        this(tenantUserRepository, tenantUserMapper, auditTrailService, authorizationStateRepository, null);
    }

    @Transactional
    public TenantUserResponse execute(UUID id, UpdateTenantUserRequest request) {
        TenantUser existingUser = tenantUserRepository.findById(id)
                .orElseThrow(() -> new TenantUserNotFoundException("Tenant user not found with id: " + id));

        String normalizedEmail = request.email().trim().toLowerCase();

        tenantUserRepository.findByEmail(normalizedEmail)
                .filter(foundUser -> !foundUser.id().equals(id))
                .ifPresent(foundUser -> {
                    throw new TenantUserAlreadyExistsException(
                            "A tenant user with email '" + normalizedEmail + "' already exists"
                    );
                });

        String firstName = normalizeNullable(request.firstName());
        String lastName = normalizeNullable(request.lastName());
        String displayName = normalizeNullable(request.displayName());

        if (displayName != null && (firstName == null || firstName.isBlank())) {
            String[] parts = displayName.trim().split("\\s+", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : "";
        }

        if (firstName == null) {
            firstName = existingUser.firstName();
        }
        if (lastName == null) {
            lastName = existingUser.lastName();
        }

        if (employeeJpaRepository != null) {
            Optional<EmployeeEntity> currentLinkedEmp = employeeJpaRepository.findByTenantUserId(id);
            UUID targetEmployeeId = request.employeeId();

            if (currentLinkedEmp.isPresent() && (targetEmployeeId == null || !currentLinkedEmp.get().getId().equals(targetEmployeeId))) {
                EmployeeEntity oldEmp = currentLinkedEmp.get();
                oldEmp.setTenantUserId(null);
                employeeJpaRepository.save(oldEmp);
            }

            if (targetEmployeeId != null && (currentLinkedEmp.isEmpty() || !currentLinkedEmp.get().getId().equals(targetEmployeeId))) {
                EmployeeEntity newEmp = employeeJpaRepository.findById(targetEmployeeId)
                        .orElseThrow(() -> new TenantUserNotFoundException("Empleado no encontrado con ID: " + targetEmployeeId));

                if (newEmp.getTenantUserId() != null && !newEmp.getTenantUserId().equals(id)) {
                    throw new TenantUserAlreadyExistsException("El empleado ya se encuentra vinculado a otro usuario");
                }

                newEmp.setTenantUserId(id);
                employeeJpaRepository.save(newEmp);
            }
        }

        TenantUser updatedUser = new TenantUser(
                existingUser.id(),
                normalizedEmail,
                existingUser.passwordHash(),
                firstName,
                lastName,
                existingUser.active(),
                existingUser.status(),
                existingUser.securityVersion(),
                existingUser.createdAt(),
                existingUser.updatedAt()
        );

        TenantUser savedUser = tenantUserRepository.save(updatedUser);

        auditTrailService.recordTenantEvent(
                AuditEventTypes.USER_UPDATED,
                "USER",
                savedUser.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "UPDATE_USER",
                        "previousEmail", existingUser.email(),
                        "newEmail", savedUser.email(),
                        "firstName", savedUser.firstName(),
                        "lastName", savedUser.lastName()
                ),
                IdentityAuditPayloads.userState(
                        existingUser.email(),
                        existingUser.firstName(),
                        existingUser.lastName(),
                        existingUser.active(),
                        existingUser.status().name()
                ),
                IdentityAuditPayloads.userState(
                        savedUser.email(),
                        savedUser.firstName(),
                        savedUser.lastName(),
                        savedUser.active(),
                        savedUser.status().name()
                )
        );

        return tenantUserMapper.toResponse(savedUser);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
