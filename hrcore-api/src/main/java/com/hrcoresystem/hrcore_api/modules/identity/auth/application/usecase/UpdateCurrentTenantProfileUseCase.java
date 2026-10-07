package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.CurrentTenantProfileResponse;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.UpdateCurrentTenantProfileRequest;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.port.FaceRecognitionPort;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.port.ProfilePhotoStoragePort;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.model.TenantUserFaceLoginProfile;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.repository.TenantUserFaceLoginProfileRepository;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.exception.TenantUserNotFoundException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class UpdateCurrentTenantProfileUseCase {

    private final SecurityContextFacade securityContextFacade;
    private final TenantUserRepository tenantUserRepository;
    private final TenantUserFaceLoginProfileRepository faceLoginProfileRepository;
    private final FaceRecognitionPort faceRecognitionPort;
    private final ProfilePhotoStoragePort profilePhotoStoragePort;

    public UpdateCurrentTenantProfileUseCase(
            SecurityContextFacade securityContextFacade,
            TenantUserRepository tenantUserRepository,
            TenantUserFaceLoginProfileRepository faceLoginProfileRepository,
            FaceRecognitionPort faceRecognitionPort,
            ProfilePhotoStoragePort profilePhotoStoragePort
    ) {
        this.securityContextFacade = securityContextFacade;
        this.tenantUserRepository = tenantUserRepository;
        this.faceLoginProfileRepository = faceLoginProfileRepository;
        this.faceRecognitionPort = faceRecognitionPort;
        this.profilePhotoStoragePort = profilePhotoStoragePort;
    }

    @Transactional
    public CurrentTenantProfileResponse execute(UpdateCurrentTenantProfileRequest request, MultipartFile photo) {
        UUID currentUserId = parseCurrentUserId();

        if ((request.firstName() == null || request.firstName().isBlank())
                && (request.lastName() == null || request.lastName().isBlank())
                && (photo == null || photo.isEmpty())) {
            throw new BusinessException("No profile fields were provided");
        }

        TenantUser existingUser = tenantUserRepository.findById(currentUserId)
                .orElseThrow(() -> new TenantUserNotFoundException("Authenticated user not found"));

        TenantUser updatedUser = new TenantUser(
                existingUser.id(),
                existingUser.email(),
                existingUser.passwordHash(),
                normalizeOptional(request.firstName(), existingUser.firstName()),
                normalizeOptional(request.lastName(), existingUser.lastName()),
                existingUser.active(),
                existingUser.status(),
                existingUser.createdAt(),
                existingUser.updatedAt()
        );

        tenantUserRepository.save(updatedUser);

        if (photo != null && !photo.isEmpty()) {
            updateFaceProfile(currentUserId, photo);
        }

        return toResponse(currentUserId);
    }

    @Transactional
    public CurrentTenantProfileResponse updatePhoto(MultipartFile photo) {
        UUID currentUserId = parseCurrentUserId();
        updateFaceProfile(currentUserId, photo);
        return toResponse(currentUserId);
    }

    @Transactional
    public CurrentTenantProfileResponse removePhoto() {
        UUID currentUserId = parseCurrentUserId();
        String tenantSlug = currentTenantSlug();
        faceLoginProfileRepository.findByUserId(currentUserId).ifPresent(profile -> {
            if (profile.profilePhotoUrl() != null && !profile.profilePhotoUrl().isBlank()) {
                profilePhotoStoragePort.delete(tenantSlug, currentUserId);
            }
            faceLoginProfileRepository.save(new TenantUserFaceLoginProfile(
                    profile.userId(),
                    profile.faceToken(),
                    profile.faceId(),
                    null,
                    null,
                    false,
                    profile.enrolledAt(),
                    profile.updatedAt()
            ));
        });
        return toResponse(currentUserId);
    }

    private void updateFaceProfile(UUID userId, MultipartFile photo) {
        byte[] bytes = readBytes(photo);
        String contentType = photo.getContentType();
        String tenantSlug = currentTenantSlug();

        String faceToken = null;
        String faceId = null;
        try {
            FaceRecognitionPort.DetectedFace detectedFace = faceRecognitionPort.detectFace(
                    bytes,
                    photo != null ? photo.getOriginalFilename() : null,
                    contentType
            );
            faceToken = detectedFace.faceToken();
            faceId = detectedFace.faceId();
        } catch (RuntimeException exception) {
            // Save the photo anyway; Face++ can be retried later without losing the profile image.
        }

        ProfilePhotoStoragePort.StoredProfilePhoto storedPhoto = profilePhotoStoragePort.store(
                tenantSlug,
                userId,
                bytes,
                contentType
        );

        faceLoginProfileRepository.save(new TenantUserFaceLoginProfile(
                userId,
                faceToken,
                faceId,
                storedPhoto.url(),
                contentType,
                true,
                null,
                null
        ));
    }

    private CurrentTenantProfileResponse toResponse(UUID currentUserId) {
        TenantUser tenantUser = tenantUserRepository.findById(currentUserId)
                .orElseThrow(() -> new TenantUserNotFoundException("Authenticated user not found"));
        var faceProfile = faceLoginProfileRepository.findByUserId(currentUserId).orElse(null);
        return new CurrentTenantProfileResponse(
                tenantUser.id(),
                tenantUser.email(),
                tenantUser.firstName(),
                tenantUser.lastName(),
                tenantUser.active(),
                tenantUser.status().name(),
                securityContextFacade.getCurrentTenantSlug(),
                faceProfile != null && faceProfile.enabled() && faceProfile.faceToken() != null && !faceProfile.faceToken().isBlank(),
                faceProfile != null && faceProfile.profilePhotoUrl() != null && !faceProfile.profilePhotoUrl().isBlank(),
                faceProfile != null ? faceProfile.profilePhotoUrl() : null,
                faceProfile != null ? faceProfile.profilePhotoContentType() : null,
                tenantUser.createdAt(),
                tenantUser.updatedAt()
        );
    }

    private UUID parseCurrentUserId() {
        String currentSubject = securityContextFacade.getCurrentSubject();
        if (currentSubject == null || currentSubject.isBlank()) {
            throw new BusinessException("Authenticated subject is not available");
        }

        try {
            return UUID.fromString(currentSubject.trim());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Authenticated subject is not a valid user id");
        }
    }

    private byte[] readBytes(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new BusinessException("La foto de perfil es obligatoria");
        }

        try {
            return photo.getBytes();
        } catch (IOException exception) {
            throw new BusinessException("No se pudo leer la foto de perfil");
        }
    }

    private String normalizeOptional(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String currentTenantSlug() {
        String tenantSlug = securityContextFacade.getCurrentTenantSlug();
        if (tenantSlug == null || tenantSlug.isBlank()) {
            throw new BusinessException("Tenant slug is not available");
        }
        return tenantSlug;
    }
}
