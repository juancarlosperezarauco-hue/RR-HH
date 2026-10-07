package com.hrcoresystem.hrcore_api.modules.identity.auth.application.port;

import java.util.UUID;

public interface ProfilePhotoStoragePort {

    StoredProfilePhoto store(String tenantSlug, UUID userId, byte[] bytes, String contentType);

    PhotoFile read(String tenantSlug, UUID userId, String contentType);

    PhotoFile readPublic(String tenantSlug, UUID userId);

    void delete(String tenantSlug, UUID userId);

    record StoredProfilePhoto(
            String url
    ) {
    }

    record PhotoFile(
            byte[] bytes,
            String contentType,
            String filename
    ) {
    }
}
