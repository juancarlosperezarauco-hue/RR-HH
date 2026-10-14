package com.hrcoresystem.hrcore_api.modules.platform.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import org.springframework.stereotype.Service;
import com.hrcoresystem.hrcore_api.common.security.session.AuthSessionService;

@Service
public class LogoutPlatformSuperadminUseCase {

    private final SecurityContextFacade securityContextFacade;
    private final AuditTrailService auditTrailService;
    private final AuthSessionService authSessionService;

    public LogoutPlatformSuperadminUseCase(SecurityContextFacade securityContextFacade,
                                           AuditTrailService auditTrailService,
                                           AuthSessionService authSessionService) {
        this.securityContextFacade = securityContextFacade;
        this.auditTrailService = auditTrailService;
        this.authSessionService = authSessionService;
    }
    public void execute() {
        if (!securityContextFacade.isPlatformAuthenticated()
                || securityContextFacade.getCurrentSessionId() == null) {
            throw new IllegalStateException("Platform session is required");
        }
        authSessionService.revokePlatformSession(
                securityContextFacade.getCurrentSessionId(),
                securityContextFacade.requireCurrentUserId());
        String currentSubject = securityContextFacade.getCurrentSubject();

        auditTrailService.recordPlatformEvent(
                AuditEventTypes.LOGOUT,
                "PLATFORM_SUPERADMIN",
                currentSubject,
                PlatformAuditPayloads.details(
                        "subject", currentSubject,
                        "email", securityContextFacade.getCurrentEmail()
                )
        );
    }
}
