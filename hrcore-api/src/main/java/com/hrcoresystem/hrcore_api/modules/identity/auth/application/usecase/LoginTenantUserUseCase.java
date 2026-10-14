package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.AuthTokenResponse;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.LoginRequest;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception.AuthenticationFailedException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.hrcoresystem.hrcore_api.common.security.session.AuthSessionService;

import java.util.Locale;

@Service
public class LoginTenantUserUseCase {

    private final TenantUserRepository tenantUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    private final AuditTrailService auditTrailService;

    public LoginTenantUserUseCase(
            TenantUserRepository tenantUserRepository,
            PasswordEncoder passwordEncoder,
            AuthSessionService authSessionService,
            AuditTrailService auditTrailService
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authSessionService = authSessionService;
        this.auditTrailService = auditTrailService;
    }

    public AuthTokenResponse execute(LoginRequest request) {
        String tenantSlug = TenantContextHolder.getRequired().tenantSlug();
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        var tenantUser = tenantUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid credentials"));

        if (tenantUser.status() == TenantUserStatus.PENDING || !tenantUser.active()) {
            if (tenantUser.status() == TenantUserStatus.PENDING) {
                throw new AuthenticationFailedException("Tu cuenta aún no está activada. Revisa tu correo.");
            }
            throw new AuthenticationFailedException("User is inactive");
        }

        if (!passwordEncoder.matches(request.password(), tenantUser.passwordHash())) {
            throw new AuthenticationFailedException("Invalid credentials");
        }

        var issued = authSessionService.createTenantSession(tenantUser, tenantSlug);

        auditTrailService.recordTenantEvent(
                AuditEventTypes.LOGIN,
                "USER",
                tenantUser.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "LOGIN",
                        "email", tenantUser.email(),
                        "tenantSlug", tenantSlug
                ),
                IdentityAuditPayloads.of(
                        "authenticated", false,
                        "tokenIssued", false
                ),
                IdentityAuditPayloads.of(
                        "authenticated", true,
                        "tokenIssued", true,
                        "tokenIssued", true
                )
        );

        return new AuthTokenResponse(
                "Bearer",
                issued.accessToken(),
                issued.refreshToken(),
                issued.accessExpiresInMs()
        );
    }
}
