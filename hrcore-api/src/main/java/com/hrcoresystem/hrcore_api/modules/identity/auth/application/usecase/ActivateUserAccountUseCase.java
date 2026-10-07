package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.ActivateAccountRequest;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception.InvalidAccountActivationTokenException;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.model.AccountActivationToken;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.repository.AccountActivationTokenRepository;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Confirms ownership of a tenant user's email address using an activation
 * token and flips the account from PENDING/inactive to ACTIVE. Named with the
 * "UserAccount" prefix to avoid clashing with the bank-account
 * {@code ActivateAccountUseCase} in the tenant.accounts module.
 */
@Service
public class ActivateUserAccountUseCase {

    private final AccountActivationTokenRepository accountActivationTokenRepository;
    private final TenantUserRepository tenantUserRepository;
    private final AuditTrailService auditTrailService;

    public ActivateUserAccountUseCase(
            AccountActivationTokenRepository accountActivationTokenRepository,
            TenantUserRepository tenantUserRepository,
            AuditTrailService auditTrailService
    ) {
        this.accountActivationTokenRepository = accountActivationTokenRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public void execute(ActivateAccountRequest request) {
        AccountActivationToken activationToken = accountActivationTokenRepository.findByToken(request.token().trim())
                .orElseThrow(() -> new InvalidAccountActivationTokenException("Account activation token is invalid"));

        if (activationToken.used()) {
            throw new InvalidAccountActivationTokenException("Account activation token has already been used");
        }

        if (activationToken.expiresAt().isBefore(Instant.now())) {
            throw new InvalidAccountActivationTokenException("Account activation token has expired");
        }

        TenantUser tenantUser = tenantUserRepository.findByEmail(activationToken.email())
                .orElseThrow(() -> new BusinessException("User associated with activation token was not found"));

        Instant now = Instant.now();

        if (tenantUser.active() && tenantUser.status() == TenantUserStatus.ACTIVE) {
            // Already activated: consume the token to avoid replay and exit gracefully.
            accountActivationTokenRepository.markUsed(activationToken.token(), now);
            return;
        }

        TenantUser activatedUser = new TenantUser(
                tenantUser.id(),
                tenantUser.email(),
                tenantUser.passwordHash(),
                tenantUser.firstName(),
                tenantUser.lastName(),
                true,
                TenantUserStatus.ACTIVE,
                tenantUser.createdAt(),
                tenantUser.updatedAt()
        );

        tenantUserRepository.save(activatedUser);

        accountActivationTokenRepository.markUsed(activationToken.token(), now);
        accountActivationTokenRepository.invalidateAllByEmail(activationToken.email(), now);

        auditTrailService.recordTenantEvent(
                AuditEventTypes.USER_ACTIVATED,
                "USER",
                tenantUser.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "ACTIVATE_ACCOUNT",
                        "email", tenantUser.email()
                ),
                IdentityAuditPayloads.of(
                        "active", false,
                        "status", TenantUserStatus.PENDING.name()
                ),
                IdentityAuditPayloads.of(
                        "active", true,
                        "status", TenantUserStatus.ACTIVE.name(),
                        "activatedAt", now.toString()
                )
        );
    }
}
