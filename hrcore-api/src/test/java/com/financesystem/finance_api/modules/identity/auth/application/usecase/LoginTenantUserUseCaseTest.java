package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.JwtProperties;
import com.hrcoresystem.hrcore_api.common.security.jwt.JwtTokenService;
import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.AuthTokenResponse;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto.LoginRequest;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception.AuthenticationFailedException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginTenantUserUseCaseTest {

    @AfterEach
    void cleanTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldLoginSuccessfully() {
        TenantUserRepository tenantUserRepository = mock(TenantUserRepository.class);
        TenantUserRoleRepository tenantUserRoleRepository = mock(TenantUserRoleRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuditTrailService auditTrailService = mock(AuditTrailService.class);

        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("this-is-a-very-secure-jwt-secret-key-with-more-than-32-chars");
        jwtProperties.setAccessExpirationMs(900000);
        jwtProperties.setRefreshExpirationMs(604800000);

        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        LoginTenantUserUseCase useCase = new LoginTenantUserUseCase(
                tenantUserRepository,
                tenantUserRoleRepository,
                passwordEncoder,
                jwtTokenService,
                jwtProperties,
                auditTrailService
        );

        TenantContextHolder.set(new TenantContext("financruz", "tenant_financruz", false));

        TenantUser tenantUser = new TenantUser(
                UUID.randomUUID(),
                "admin@financruz.com",
                "encoded-password",
                "Admin",
                "Tenant",
                true,
                TenantUserStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        when(tenantUserRepository.findByEmail("admin@financruz.com")).thenReturn(Optional.of(tenantUser));
        when(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true);
        when(tenantUserRoleRepository.findRoleNamesByUserId(tenantUser.id())).thenReturn(List.of("ADMIN"));

        AuthTokenResponse response = useCase.execute(
                new LoginRequest("admin@financruz.com", "Password123!")
        );

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        AuthenticatedUserPrincipal principal = jwtTokenService.parseAccessToken(response.accessToken());
        assertEquals(tenantUser.id().toString(), principal.subject());
        assertEquals("financruz", principal.tenantSlug());
        assertTrue(principal.roles().contains("ADMIN"));
        verify(auditTrailService).recordTenantEvent(
                anyString(),
                eq("USER"),
                eq(tenantUser.id().toString()),
                any(),
                any(),
                any()
        );
    }

    @Test
    void shouldThrowWhenPasswordIsInvalid() {
        TenantUserRepository tenantUserRepository = mock(TenantUserRepository.class);
        TenantUserRoleRepository tenantUserRoleRepository = mock(TenantUserRoleRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuditTrailService auditTrailService = mock(AuditTrailService.class);

        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("this-is-a-very-secure-jwt-secret-key-with-more-than-32-chars");
        jwtProperties.setAccessExpirationMs(900000);
        jwtProperties.setRefreshExpirationMs(604800000);

        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        LoginTenantUserUseCase useCase = new LoginTenantUserUseCase(
                tenantUserRepository,
                tenantUserRoleRepository,
                passwordEncoder,
                jwtTokenService,
                jwtProperties,
                auditTrailService
        );

        TenantContextHolder.set(new TenantContext("financruz", "tenant_financruz", false));

        TenantUser tenantUser = new TenantUser(
                UUID.randomUUID(),
                "admin@financruz.com",
                "encoded-password",
                "Admin",
                "Tenant",
                true,
                TenantUserStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        when(tenantUserRepository.findByEmail("admin@financruz.com")).thenReturn(Optional.of(tenantUser));
        when(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(false);

        assertThrows(AuthenticationFailedException.class, () ->
                useCase.execute(new LoginRequest("admin@financruz.com", "Password123!"))
        );
    }
}
