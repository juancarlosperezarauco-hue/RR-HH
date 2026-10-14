package com.hrcoresystem.hrcore_api.modules.platform.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.session.AuthSessionService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.platform.auth.application.dto.PlatformAuthTokenResponse;
import com.hrcoresystem.hrcore_api.modules.platform.auth.application.dto.PlatformRefreshTokenRequest;
import org.springframework.stereotype.Service;

@Service
public class RefreshPlatformTokenUseCase {
    private final AuditTrailService auditTrailService;
    private final AuthSessionService authSessionService;

    public RefreshPlatformTokenUseCase(AuditTrailService auditTrailService,
                                       AuthSessionService authSessionService) {
        this.auditTrailService = auditTrailService;
        this.authSessionService = authSessionService;
    }

    public PlatformAuthTokenResponse execute(PlatformRefreshTokenRequest request) {
        var issued = authSessionService.refreshPlatformSession(request.refreshToken());
        auditTrailService.recordPlatformEvent(
                AuditEventTypes.TOKEN_REFRESHED,
                "PLATFORM_SUPERADMIN",
                "session",
                PlatformAuditPayloads.details("operation", "REFRESH_TOKEN"));
        return new PlatformAuthTokenResponse("Bearer", issued.accessToken(), issued.refreshToken(), issued.accessExpiresInMs());
    }
}
