package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.JwtProperties;
import com.hrcoresystem.hrcore_api.common.security.jwt.JwtClaimNames;
import com.hrcoresystem.hrcore_api.common.security.jwt.JwtTokenService;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.AuthTokenResponse;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.RefreshTokenRequest;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception.AuthenticationFailedException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RefreshTenantTokenUseCase {

    private final TenantUserRepository tenantUserRepository;
    private final TenantUserRoleRepository tenantUserRoleRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final AuditTrailService auditTrailService;

    public RefreshTenantTokenUseCase(
            TenantUserRepository tenantUserRepository,
            TenantUserRoleRepository tenantUserRoleRepository,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties,
            AuditTrailService auditTrailService
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.tenantUserRoleRepository = tenantUserRoleRepository;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.auditTrailService = auditTrailService;
    }

    public AuthTokenResponse execute(RefreshTokenRequest request) {
        TenantContext tenantContext = TenantContextHolder.getRequired();

        Claims claims = jwtTokenService.parseRefreshToken(request.refreshToken());

        String subject = claims.getSubject();
        String tokenTenant = claims.get(JwtClaimNames.TENANT, String.class);

        if (subject == null || subject.isBlank()) {
            throw new AuthenticationFailedException("Invalid refresh token");
        }

        if (tokenTenant == null || tokenTenant.isBlank()) {
            throw new AuthenticationFailedException("Invalid refresh token tenant");
        }

        if (!tokenTenant.equalsIgnoreCase(tenantContext.tenantSlug())) {
            throw new AuthenticationFailedException("Refresh token tenant does not match current tenant");
        }

        UUID userId = parseSubjectAsUserId(subject);

        TenantUser tenantUser = tenantUserRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("User not found for refresh token"));

        if (tenantUser.status() == TenantUserStatus.PENDING || !tenantUser.active()) {
            if (tenantUser.status() == TenantUserStatus.PENDING) {
                throw new AuthenticationFailedException("Tu cuenta aún no está activada. Revisa tu correo.");
            }
            throw new AuthenticationFailedException("User is inactive");
        }

        List<String> roles = tenantUserRoleRepository.findRoleNamesByUserId(tenantUser.id()).stream()
                .filter(this::isAuthorizableRole)
                .toList();
        List<String> permissions = tenantUserRoleRepository.findPermissionCodesByUserId(tenantUser.id());

        String newAccessToken = jwtTokenService.generateAccessToken(
                tenantUser.id().toString(),
                tenantUser.email(),
                fullName(tenantUser.firstName(), tenantUser.lastName()),
                tenantContext.tenantSlug(),
                roles,
                permissions
        );

        String newRefreshToken = jwtTokenService.generateRefreshToken(
                tenantUser.id().toString(),
                tenantContext.tenantSlug()
        );

        auditTrailService.recordTenantEvent(
                AuditEventTypes.TOKEN_REFRESHED,
                "USER",
                tenantUser.id().toString(),
                IdentityAuditPayloads.of(
                        "operation", "REFRESH_TOKEN",
                        "email", tenantUser.email(),
                        "tenantSlug", tenantContext.tenantSlug(),
                        "roles", roles,
                        "permissionCount", permissions.size()
                ),
                IdentityAuditPayloads.of(
                        "refreshTokenValidated", true,
                        "tenantSlug", tenantContext.tenantSlug()
                ),
                IdentityAuditPayloads.of(
                        "accessTokenIssued", true,
                        "refreshTokenRotated", true,
                        "roleCount", roles.size(),
                        "permissionCount", permissions.size()
                )
        );

        return new AuthTokenResponse(
                "Bearer",
                newAccessToken,
                newRefreshToken,
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

    private UUID parseSubjectAsUserId(String subject) {
        try {
            return UUID.fromString(subject.trim());
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationFailedException("Invalid refresh token subject");
        }
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
}
