package com.hrcoresystem.hrcore_api.modules.platform.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception.AuthenticationFailedException;
import com.hrcoresystem.hrcore_api.modules.platform.auth.application.dto.AuthenticatedPlatformSuperadminResponse;
import com.hrcoresystem.hrcore_api.modules.platform.auth.domain.model.PlatformAuthConstants;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.model.PlatformSuperadmin;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.repository.PlatformSuperadminRepository;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.AuthorizationCatalogStateRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GetCurrentAuthenticatedPlatformSuperadminUseCase {

    private final SecurityContextFacade securityContextFacade;
    private final PlatformSuperadminRepository platformSuperadminRepository;
    private final AuthorizationCatalogStateRepository catalogStateRepository;

    public GetCurrentAuthenticatedPlatformSuperadminUseCase(
            SecurityContextFacade securityContextFacade,
            PlatformSuperadminRepository platformSuperadminRepository,
            AuthorizationCatalogStateRepository catalogStateRepository
    ) {
        this.securityContextFacade = securityContextFacade;
        this.platformSuperadminRepository = platformSuperadminRepository;
        this.catalogStateRepository = catalogStateRepository;
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

        PlatformSuperadmin superadmin = platformSuperadminRepository.findById(securityContextFacade.requireCurrentUserId())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated platform superadmin was not found"));

        return new AuthenticatedPlatformSuperadminResponse(
                superadmin.id(),
                superadmin.email(),
                superadmin.firstName(),
                superadmin.lastName(),
                superadmin.active(),
                PlatformAuthConstants.PLATFORM_TENANT_SLUG,
                "PLATFORM",
                PlatformAuthConstants.PLATFORM_ROLES,
                List.of(),
                superadmin.securityVersion(),
                catalogStateRepository.find().orElseThrow(() -> new AuthenticationFailedException("Authorization catalog state is missing")).catalogVersion(),
                superadmin.createdAt(),
                superadmin.updatedAt()
        );
    }
}
