package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.CurrentTenantProfileResponse;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.repository.TenantUserFaceLoginProfileRepository;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception.AuthenticationFailedException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetCurrentTenantProfileUseCase {

    private final SecurityContextFacade securityContextFacade;
    private final TenantUserRepository tenantUserRepository;
    private final TenantUserFaceLoginProfileRepository faceLoginProfileRepository;

    public GetCurrentTenantProfileUseCase(
            SecurityContextFacade securityContextFacade,
            TenantUserRepository tenantUserRepository,
            TenantUserFaceLoginProfileRepository faceLoginProfileRepository
    ) {
        this.securityContextFacade = securityContextFacade;
        this.tenantUserRepository = tenantUserRepository;
        this.faceLoginProfileRepository = faceLoginProfileRepository;
    }

    public CurrentTenantProfileResponse execute() {
        String currentSubject = securityContextFacade.getCurrentSubject();
        String currentTenantSlug = securityContextFacade.getCurrentTenantSlug();

        if (currentSubject == null || currentSubject.isBlank()) {
            throw new AuthenticationFailedException("Authenticated subject is not available");
        }

        TenantUser tenantUser = tenantUserRepository.findById(parseSubjectAsUserId(currentSubject))
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));

        var faceProfile = faceLoginProfileRepository.findByUserId(tenantUser.id()).orElse(null);

        return new CurrentTenantProfileResponse(
                tenantUser.id(),
                tenantUser.email(),
                tenantUser.firstName(),
                tenantUser.lastName(),
                tenantUser.active(),
                tenantUser.status().name(),
                currentTenantSlug,
                faceProfile != null && faceProfile.enabled(),
                faceProfile != null && faceProfile.profilePhotoUrl() != null && !faceProfile.profilePhotoUrl().isBlank(),
                faceProfile != null ? faceProfile.profilePhotoUrl() : null,
                faceProfile != null ? faceProfile.profilePhotoContentType() : null,
                tenantUser.createdAt(),
                tenantUser.updatedAt()
        );
    }

    private UUID parseSubjectAsUserId(String subject) {
        try {
            return UUID.fromString(subject.trim());
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationFailedException("Authenticated subject is not a valid user id");
        }
    }
}
