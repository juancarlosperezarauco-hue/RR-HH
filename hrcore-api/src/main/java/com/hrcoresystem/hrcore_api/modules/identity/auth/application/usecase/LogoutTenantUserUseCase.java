package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import org.springframework.stereotype.Service;
import com.hrcoresystem.hrcore_api.common.security.session.AuthSessionService;

@Service
public class LogoutTenantUserUseCase {

    private final SecurityContextFacade securityContextFacade;
    private final AuditTrailService auditTrailService;
    private final AuthSessionService authSessionService;

    public LogoutTenantUserUseCase(
            SecurityContextFacade securityContextFacade,
            AuditTrailService auditTrailService, AuthSessionService authSessionService
    ) {
        this.securityContextFacade = securityContextFacade;
        this.auditTrailService = auditTrailService;
        this.authSessionService = authSessionService;
    }
    public LogoutTenantUserUseCase(SecurityContextFacade securityContextFacade,AuditTrailService auditTrailService){this(securityContextFacade,auditTrailService,null);}

    public void execute() {
        if(authSessionService!=null&&securityContextFacade.getCurrentSessionId()!=null)authSessionService.revokeTenantSession(securityContextFacade.getCurrentSessionId());
        String currentSubject = securityContextFacade.getCurrentSubject();

        auditTrailService.recordTenantEvent(
                AuditEventTypes.LOGOUT,
                "USER",
                currentSubject,
                IdentityAuditPayloads.of(
                        "operation", "LOGOUT",
                        "subject", currentSubject,
                        "tenantSlug", securityContextFacade.getCurrentTenantSlug()
                ),
                IdentityAuditPayloads.of(
                        "authenticated", true
                ),
                IdentityAuditPayloads.of(
                        "authenticated", false
                )
        );
    }
}
