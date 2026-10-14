package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.session.AuthSessionService;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.AuthTokenResponse;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.RefreshTokenRequest;
import org.springframework.stereotype.Service;

@Service
public class RefreshTenantTokenUseCase {
    private final AuditTrailService auditTrailService;
    private final AuthSessionService authSessionService;

    public RefreshTenantTokenUseCase(AuditTrailService auditTrailService,
                                     AuthSessionService authSessionService) {
        this.auditTrailService = auditTrailService;
        this.authSessionService = authSessionService;
    }

    public AuthTokenResponse execute(RefreshTokenRequest request) {
        String tenantSlug = TenantContextHolder.getRequired().tenantSlug();
        var issued = authSessionService.refreshTenantSession(request.refreshToken(), tenantSlug);
        auditTrailService.recordTenantEvent(
                AuditEventTypes.TOKEN_REFRESHED,
                "USER",
                "session",
                IdentityAuditPayloads.of("operation", "REFRESH_TOKEN", "tenantSlug", tenantSlug),
                IdentityAuditPayloads.of("refreshTokenValidated", true),
                IdentityAuditPayloads.of("accessTokenIssued", true, "refreshTokenRotated", true));
        return new AuthTokenResponse("Bearer", issued.accessToken(), issued.refreshToken(), issued.accessExpiresInMs());
    }
}
