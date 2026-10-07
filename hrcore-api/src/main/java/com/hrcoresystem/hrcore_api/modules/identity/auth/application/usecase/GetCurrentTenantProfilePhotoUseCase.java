package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.CurrentTenantProfilePhotoResponse;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.port.ProfilePhotoStoragePort;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception.AuthenticationFailedException;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.model.TenantUserFaceLoginProfile;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.repository.TenantUserFaceLoginProfileRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetCurrentTenantProfilePhotoUseCase {

    private final SecurityContextFacade securityContextFacade;
    private final TenantUserFaceLoginProfileRepository faceLoginProfileRepository;
    private final ProfilePhotoStoragePort profilePhotoStoragePort;

    public GetCurrentTenantProfilePhotoUseCase(
            SecurityContextFacade securityContextFacade,
            TenantUserFaceLoginProfileRepository faceLoginProfileRepository,
            ProfilePhotoStoragePort profilePhotoStoragePort
    ) {
        this.securityContextFacade = securityContextFacade;
        this.faceLoginProfileRepository = faceLoginProfileRepository;
        this.profilePhotoStoragePort = profilePhotoStoragePort;
    }

    public CurrentTenantProfilePhotoResponse execute() {
        UUID currentUserId = parseCurrentUserId();
        String tenantSlug = securityContextFacade.getCurrentTenantSlug();

        TenantUserFaceLoginProfile profile = faceLoginProfileRepository.findByUserId(currentUserId)
                .filter(found -> found.profilePhotoUrl() != null && !found.profilePhotoUrl().isBlank())
                .orElseThrow(() -> new AuthenticationFailedException("Profile photo not found"));

        ProfilePhotoStoragePort.PhotoFile photoFile = profilePhotoStoragePort.read(
                tenantSlug,
                currentUserId,
                profile.profilePhotoContentType()
        );
        return new CurrentTenantProfilePhotoResponse(
                photoFile.bytes(),
                photoFile.contentType(),
                photoFile.filename()
        );
    }

    private UUID parseCurrentUserId() {
        String currentSubject = securityContextFacade.getCurrentSubject();
        if (currentSubject == null || currentSubject.isBlank()) {
            throw new AuthenticationFailedException("Authenticated subject is not available");
        }

        try {
            return UUID.fromString(currentSubject.trim());
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationFailedException("Authenticated subject is not a valid user id");
        }
    }
}
