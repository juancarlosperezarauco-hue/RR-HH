package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.JwtProperties;
import com.hrcoresystem.hrcore_api.common.security.jwt.JwtTokenService;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.config.FaceRecognitionProperties;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.AuthTokenResponse;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.exception.FaceRecognitionUnavailableException;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.port.FaceRecognitionPort;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.port.ProfilePhotoStoragePort;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception.AuthenticationFailedException;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.model.TenantUserFaceLoginProfile;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.repository.TenantUserFaceLoginProfileRepository;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Service
public class LoginTenantUserWithFaceUseCase {

    private final TenantUserRepository tenantUserRepository;
    private final TenantUserRoleRepository tenantUserRoleRepository;
    private final TenantUserFaceLoginProfileRepository faceLoginProfileRepository;
    private final FaceRecognitionPort faceRecognitionPort;
    private final ProfilePhotoStoragePort profilePhotoStoragePort;
    private final FaceRecognitionProperties faceRecognitionProperties;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final AuditTrailService auditTrailService;

    public LoginTenantUserWithFaceUseCase(
            TenantUserRepository tenantUserRepository,
            TenantUserRoleRepository tenantUserRoleRepository,
            TenantUserFaceLoginProfileRepository faceLoginProfileRepository,
            FaceRecognitionPort faceRecognitionPort,
            ProfilePhotoStoragePort profilePhotoStoragePort,
            FaceRecognitionProperties faceRecognitionProperties,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties,
            AuditTrailService auditTrailService
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.tenantUserRoleRepository = tenantUserRoleRepository;
        this.faceLoginProfileRepository = faceLoginProfileRepository;
        this.faceRecognitionPort = faceRecognitionPort;
        this.profilePhotoStoragePort = profilePhotoStoragePort;
        this.faceRecognitionProperties = faceRecognitionProperties;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.auditTrailService = auditTrailService;
    }

    public AuthTokenResponse execute(String email, MultipartFile image) {
        if (!faceRecognitionPort.isConfigured()) {
            throw new FaceRecognitionUnavailableException("Face recognition is not configured", null);
        }

        TenantContext tenantContext = TenantContextHolder.getRequired();
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new AuthenticationFailedException("Email is required");
        }

        TenantUser tenantUser = tenantUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid credentials"));

        if (tenantUser.status() == TenantUserStatus.PENDING || !tenantUser.active()) {
            if (tenantUser.status() == TenantUserStatus.PENDING) {
                throw new AuthenticationFailedException("Tu cuenta aún no está activada. Revisa tu correo.");
            }
            throw new AuthenticationFailedException("User is inactive");
        }

        TenantUserFaceLoginProfile faceProfile = faceLoginProfileRepository.findByUserId(tenantUser.id())
                .filter(TenantUserFaceLoginProfile::enabled)
                .orElseThrow(() -> new AuthenticationFailedException("Face login is not enabled for this user"));

        byte[] imageBytes = readBytes(image);
        FaceRecognitionPort.DetectedFace detectedFace = faceRecognitionPort.detectFace(
                imageBytes,
                image != null ? image.getOriginalFilename() : null,
                image != null ? image.getContentType() : null
        );

        FaceRecognitionPort.FaceComparisonResult comparison = compareWithStoredProfile(
                detectedFace,
                faceProfile,
                tenantContext.tenantSlug()
        );

        if (comparison.confidence() < faceRecognitionProperties.getConfidenceThreshold()) {
            throw new AuthenticationFailedException(formatFaceVerificationFailed(comparison.confidence()));
        }

        List<String> roles = tenantUserRoleRepository.findRoleNamesByUserId(tenantUser.id()).stream()
                .filter(this::isAuthorizableRole)
                .toList();
        List<String> permissions = tenantUserRoleRepository.findPermissionCodesByUserId(tenantUser.id());
        String subject = tenantUser.id().toString();

        String accessToken = jwtTokenService.generateAccessToken(
                subject,
                tenantUser.email(),
                fullName(tenantUser.firstName(), tenantUser.lastName()),
                tenantContext.tenantSlug(),
                roles,
                permissions
        );

        String refreshToken = jwtTokenService.generateRefreshToken(
                subject,
                tenantContext.tenantSlug()
        );

        auditTrailService.recordTenantEvent(
                AuditEventTypes.LOGIN,
                "USER",
                tenantUser.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "FACE_LOGIN",
                        "email", tenantUser.email(),
                        "tenantSlug", tenantContext.tenantSlug(),
                        "roles", roles,
                        "permissionCount", permissions.size(),
                        "confidence", comparison.confidence(),
                        "confidenceThreshold", faceRecognitionProperties.getConfidenceThreshold()
                ),
                IdentityAuditPayloads.of(
                        "authenticated", false,
                        "tokenIssued", false
                ),
                IdentityAuditPayloads.of(
                        "authenticated", true,
                        "tokenIssued", true,
                        "roleCount", roles.size(),
                        "permissionCount", permissions.size()
                )
        );

        return new AuthTokenResponse(
                "Bearer",
                accessToken,
                refreshToken,
                jwtProperties.getAccessExpirationMs()
        );
    }

    private boolean isAuthorizableRole(String roleName) {
        if (roleName == null) {
            return false;
        }

        return "ADMIN".equalsIgnoreCase(roleName.trim())
                || "OWNER_ADMIN".equalsIgnoreCase(roleName.trim());
    }

    private byte[] readBytes(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new AuthenticationFailedException("Face image is required");
        }

        try {
            return image.getBytes();
        } catch (IOException exception) {
            throw new AuthenticationFailedException("Could not read the provided face image");
        }
    }

    private FaceRecognitionPort.FaceComparisonResult compareWithStoredProfile(
            FaceRecognitionPort.DetectedFace detectedFace,
            TenantUserFaceLoginProfile faceProfile,
            String tenantSlug
    ) {
        if (faceProfile.faceToken() != null && !faceProfile.faceToken().isBlank()) {
            return faceRecognitionPort.compareFaceTokens(detectedFace.faceToken(), faceProfile.faceToken());
        }

        if (faceProfile.profilePhotoUrl() == null || faceProfile.profilePhotoUrl().isBlank()) {
            throw new AuthenticationFailedException("Face login is not enabled for this user");
        }

        ProfilePhotoStoragePort.PhotoFile profilePhoto = profilePhotoStoragePort.read(
                tenantSlug,
                faceProfile.userId(),
                faceProfile.profilePhotoContentType()
        );

        FaceRecognitionPort.DetectedFace storedFace = faceRecognitionPort.detectFace(
                profilePhoto.bytes(),
                profilePhoto.filename(),
                profilePhoto.contentType()
        );
        return faceRecognitionPort.compareFaceTokens(detectedFace.faceToken(), storedFace.faceToken());
    }

    private String fullName(String firstName, String lastName) {
        StringBuilder builder = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            builder.append(firstName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(lastName.trim());
        }
        String value = builder.toString().trim();
        return value.isEmpty() ? null : value;
    }

    private String formatFaceVerificationFailed(double confidence) {
        double threshold = faceRecognitionProperties.getConfidenceThreshold();
        return String.format(
                Locale.US,
                "Verificación facial fallida: confianza %.2f%% por debajo del mínimo requerido %.2f%%",
                confidence,
                threshold
        );
    }
}
