package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.ResendActivationRequest;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResendAccountActivationUseCase {

    private final TenantUserRepository tenantUserRepository;
    private final IssueAccountActivationUseCase issueAccountActivationUseCase;

    public ResendAccountActivationUseCase(
            TenantUserRepository tenantUserRepository,
            IssueAccountActivationUseCase issueAccountActivationUseCase
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.issueAccountActivationUseCase = issueAccountActivationUseCase;
    }

    @Transactional
    public void execute(ResendActivationRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        TenantUser tenantUser = tenantUserRepository.findByEmail(normalizedEmail).orElse(null);

        // Do not reveal whether the account exists or is already active.
        if (tenantUser == null
                || (tenantUser.active() && tenantUser.status() == TenantUserStatus.ACTIVE)) {
            return;
        }

        issueAccountActivationUseCase.execute(normalizedEmail);
    }
}
