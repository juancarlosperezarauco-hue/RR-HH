package com.hrcoresystem.hrcore_api.modules.platform.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception.AuthenticationFailedException;
import com.hrcoresystem.hrcore_api.modules.platform.auth.application.dto.AuthenticatedPlatformSuperadminResponse;
import com.hrcoresystem.hrcore_api.modules.platform.auth.domain.model.PlatformAuthConstants;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.model.PlatformSuperadmin;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.repository.PlatformSuperadminRepository;
import org.springframework.stereotype.Service;

@Service
public class GetCurrentAuthenticatedPlatformSuperadminUseCase {

    private final SecurityContextFacade securityContextFacade;
    private final PlatformSuperadminRepository platformSuperadminRepository;

    public GetCurrentAuthenticatedPlatformSuperadminUseCase(
            SecurityContextFacade securityContextFacade,
            PlatformSuperadminRepository platformSuperadminRepository
    ) {
        this.securityContextFacade = securityContextFacade;
        this.platformSuperadminRepository = platformSuperadminRepository;
    }

    public AuthenticatedPlatformSuperadminResponse execute() {
        String currentSubject = securityContextFacade.getCurrentSubject();
        String currentTenantSlug = securityContextFacade.getCurrentTenantSlug();

        if (currentSubject == null || currentSubject.isBlank()) {
            throw new AuthenticationFailedException("Authenticated platform subject is not available");
        }

        if (!PlatformAuthConstants.PLATFORM_TENANT_SLUG.equalsIgnoreCase(currentTenantSlug)) {
            throw new AuthenticationFailedException("Current token does not belong to platform auth");
        }

        PlatformSuperadmin superadmin = platformSuperadminRepository.findByEmail(currentSubject)
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated platform superadmin was not found"));

        return new AuthenticatedPlatformSuperadminResponse(
                superadmin.id(),
                superadmin.email(),
                superadmin.firstName(),
                superadmin.lastName(),
                superadmin.active(),
                PlatformAuthConstants.PLATFORM_ROLES,
                superadmin.createdAt(),
                superadmin.updatedAt()
        );
    }
}
