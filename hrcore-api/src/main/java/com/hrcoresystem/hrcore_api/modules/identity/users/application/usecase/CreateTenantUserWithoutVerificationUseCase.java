package com.hrcoresystem.hrcore_api.modules.identity.users.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.CreateTenantUserRequest;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.TenantUserResponse;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.mapper.TenantUserMapper;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception.TenantUserAlreadyExistsException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class CreateTenantUserWithoutVerificationUseCase {

    private final TenantUserRepository tenantUserRepository;
    private final TenantUserMapper tenantUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditTrailService auditTrailService;
    private final TenantPlanEnforcementService tenantPlanEnforcementService;

    public CreateTenantUserWithoutVerificationUseCase(
            TenantUserRepository tenantUserRepository,
            TenantUserMapper tenantUserMapper,
            PasswordEncoder passwordEncoder,
            AuditTrailService auditTrailService,
            TenantPlanEnforcementService tenantPlanEnforcementService
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.tenantUserMapper = tenantUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditTrailService = auditTrailService;
        this.tenantPlanEnforcementService = tenantPlanEnforcementService;
    }

    @Transactional
    public TenantUserResponse execute(CreateTenantUserRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (tenantUserRepository.existsByEmail(normalizedEmail)) {
            throw new TenantUserAlreadyExistsException(
                    "A tenant user with email '" + normalizedEmail + "' already exists"
            );
        }

        tenantPlanEnforcementService.assertCanCreateUser(
                tenantUserRepository.countActiveAndPendingUsers()
        );

        TenantUser tenantUserToCreate = new TenantUser(
                null,
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                normalizeNullable(request.firstName()),
                normalizeNullable(request.lastName()),
                true,
                TenantUserStatus.ACTIVE,
                null,
                null
        );

        TenantUser createdTenantUser = tenantUserRepository.save(tenantUserToCreate);

        auditTrailService.recordTenantEvent(
                AuditEventTypes.USER_CREATED,
                "USER",
                createdTenantUser.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "CREATE_USER_NO_VERIFICATION",
                        "email", createdTenantUser.email(),
                        "firstName", createdTenantUser.firstName(),
                        "lastName", createdTenantUser.lastName(),
                        "defaultRole", "NONE",
                        "defaultRoleAssigned", false,
                        "verificationRequired", false
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

        return tenantUserMapper.toResponse(createdTenantUser);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
