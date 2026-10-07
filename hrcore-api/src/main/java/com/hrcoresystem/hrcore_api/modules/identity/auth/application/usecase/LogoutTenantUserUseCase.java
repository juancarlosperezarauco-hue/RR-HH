package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import org.springframework.stereotype.Service;

@Service
public class LogoutTenantUserUseCase {

    private final SecurityContextFacade securityContextFacade;
    private final AuditTrailService auditTrailService;

    public LogoutTenantUserUseCase(
            SecurityContextFacade securityContextFacade,
            AuditTrailService auditTrailService
    ) {
        this.securityContextFacade = securityContextFacade;
        this.auditTrailService = auditTrailService;
    }

    public void execute() {
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
