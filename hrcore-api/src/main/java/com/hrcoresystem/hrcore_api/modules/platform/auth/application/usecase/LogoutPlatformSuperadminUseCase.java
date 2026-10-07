package com.hrcoresystem.hrcore_api.modules.platform.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import org.springframework.stereotype.Service;

@Service
public class LogoutPlatformSuperadminUseCase {

    private final SecurityContextFacade securityContextFacade;
    private final AuditTrailService auditTrailService;

    public LogoutPlatformSuperadminUseCase(
            SecurityContextFacade securityContextFacade,
            AuditTrailService auditTrailService
    ) {
        this.securityContextFacade = securityContextFacade;
        this.auditTrailService = auditTrailService;
    }

    public void execute() {
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
