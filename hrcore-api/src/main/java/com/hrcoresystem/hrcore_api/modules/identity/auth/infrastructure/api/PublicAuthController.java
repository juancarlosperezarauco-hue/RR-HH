package com.hrcoresystem.hrcore_api.modules.identity.auth.infrastructure.api;

import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.CurrentTenantProfilePhotoResponse;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase.GetPublicTenantProfilePhotoUseCase;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/auth")
public class PublicAuthController {

    private final GetPublicTenantProfilePhotoUseCase getPublicTenantProfilePhotoUseCase;

    public PublicAuthController(GetPublicTenantProfilePhotoUseCase getPublicTenantProfilePhotoUseCase) {
        this.getPublicTenantProfilePhotoUseCase = getPublicTenantProfilePhotoUseCase;
    }

    @GetMapping({"/profile/photo/{tenantSlug}/{userId}", "/profile/photo/{tenantSlug}/{userId}.{extension}"})
    public ResponseEntity<byte[]> profilePhoto(
            @PathVariable String tenantSlug,
            @PathVariable String userId,
            @PathVariable(value = "extension", required = false) String extension
    ) {
        CurrentTenantProfilePhotoResponse photo = getPublicTenantProfilePhotoUseCase.execute(tenantSlug, parseUuid(userId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + (photo.filename() == null ? "profile-photo" : photo.filename()) + "\"")
                .contentType(MediaType.parseMediaType(
                        photo.contentType() == null || photo.contentType().isBlank()
                                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                                : photo.contentType()
                ))
                .body(photo.bytes());
    }

    private UUID parseUuid(String raw) {
        String value = raw == null ? null : raw.trim();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }

        int dotIndex = value.indexOf('.');
        if (dotIndex > 0) {
            value = value.substring(0, dotIndex);
        }

        return UUID.fromString(value);
    }
}
