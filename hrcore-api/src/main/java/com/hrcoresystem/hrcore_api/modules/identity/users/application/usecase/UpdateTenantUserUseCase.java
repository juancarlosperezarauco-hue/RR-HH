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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UpdateTenantUserUseCase {

    private final TenantUserRepository tenantUserRepository;
    private final TenantUserMapper tenantUserMapper;
    private final AuditTrailService auditTrailService;

    public UpdateTenantUserUseCase(
            TenantUserRepository tenantUserRepository,
            TenantUserMapper tenantUserMapper,
            AuditTrailService auditTrailService
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.tenantUserMapper = tenantUserMapper;
        this.auditTrailService = auditTrailService;
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

        TenantUser updatedUser = new TenantUser(
                existingUser.id(),
                normalizedEmail,
                existingUser.passwordHash(),
                normalizeNullable(request.firstName()),
                normalizeNullable(request.lastName()),
                existingUser.active(),
                existingUser.status(),
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
