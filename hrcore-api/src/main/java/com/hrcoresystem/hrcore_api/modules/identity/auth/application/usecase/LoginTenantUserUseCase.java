package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.JwtProperties;
import com.hrcoresystem.hrcore_api.common.security.jwt.JwtTokenService;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.identity.audit.IdentityAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.AuthTokenResponse;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.LoginRequest;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception.AuthenticationFailedException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoginTenantUserUseCase {

    private final TenantUserRepository tenantUserRepository;
    private final TenantUserRoleRepository tenantUserRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final AuditTrailService auditTrailService;

    public LoginTenantUserUseCase(
            TenantUserRepository tenantUserRepository,
            TenantUserRoleRepository tenantUserRoleRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties,
            AuditTrailService auditTrailService
    ) {
        this.tenantUserRepository = tenantUserRepository;
        this.tenantUserRoleRepository = tenantUserRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.auditTrailService = auditTrailService;
    }

    public AuthTokenResponse execute(LoginRequest request) {
        TenantContext tenantContext = TenantContextHolder.getRequired();
        String normalizedEmail = request.email().trim().toLowerCase();

        TenantUser tenantUser = tenantUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid credentials"));

        if (tenantUser.status() == TenantUserStatus.PENDING || !tenantUser.active()) {
            if (tenantUser.status() == TenantUserStatus.PENDING) {
                throw new AuthenticationFailedException("Tu cuenta aún no está activada. Revisa tu correo.");
            }
            throw new AuthenticationFailedException("User is inactive");
        }

        if (!passwordEncoder.matches(request.password(), tenantUser.passwordHash())) {
            throw new AuthenticationFailedException("Invalid credentials");
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
                        "operation", "LOGIN",
                        "email", tenantUser.email(),
                        "tenantSlug", tenantContext.tenantSlug(),
                        "roles", roles,
                        "permissionCount", permissions.size()
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
