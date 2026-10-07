package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.CurrentTenantProfilePhotoResponse;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.port.ProfilePhotoStoragePort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetPublicTenantProfilePhotoUseCase {

    private final ProfilePhotoStoragePort profilePhotoStoragePort;

    public GetPublicTenantProfilePhotoUseCase(ProfilePhotoStoragePort profilePhotoStoragePort) {
        this.profilePhotoStoragePort = profilePhotoStoragePort;
    }

    public CurrentTenantProfilePhotoResponse execute(String tenantSlug, UUID userId) {
        if (tenantSlug == null || tenantSlug.isBlank()) {
            throw new BusinessException("Tenant slug is required");
        }
        if (userId == null) {
            throw new BusinessException("User id is required");
        }

        ProfilePhotoStoragePort.PhotoFile photoFile = profilePhotoStoragePort.readPublic(tenantSlug, userId);
        return new CurrentTenantProfilePhotoResponse(
                photoFile.bytes(),
                photoFile.contentType(),
                photoFile.filename()
        );
    }
}
