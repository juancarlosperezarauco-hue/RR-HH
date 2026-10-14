package com.hrcoresystem.hrcore_api.modules.identity.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.session.AuthSessionService;
import com.hrcoresystem.hrcore_api.common.security.session.IssuedTokens;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
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
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuditTrailService auditTrailService = mock(AuditTrailService.class);
        AuthSessionService sessions = mock(AuthSessionService.class);

        LoginTenantUserUseCase useCase = new LoginTenantUserUseCase(
                tenantUserRepository,
                passwordEncoder,
                sessions,
                auditTrailService
        );

        TenantContextHolder.set(new TenantContext("talentohumano", "tenant_talentohumano", false));

        TenantUser tenantUser = new TenantUser(
                UUID.randomUUID(),
                "admin@talentohumano.com",
                "encoded-password",
                "Admin",
                "Tenant",
                true,
                TenantUserStatus.ACTIVE,
                1,
                Instant.now(),
                Instant.now()
        );

        when(tenantUserRepository.findByEmail("admin@talentohumano.com")).thenReturn(Optional.of(tenantUser));
        when(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true);
        when(sessions.createTenantSession(tenantUser, "talentohumano"))
                .thenReturn(new IssuedTokens("access", "refresh", 900000));

        AuthTokenResponse response = useCase.execute(
                new LoginRequest("admin@talentohumano.com", "Password123!")
        );

        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        verify(sessions).createTenantSession(tenantUser, "talentohumano");
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
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuditTrailService auditTrailService = mock(AuditTrailService.class);
        AuthSessionService sessions = mock(AuthSessionService.class);

        LoginTenantUserUseCase useCase = new LoginTenantUserUseCase(
                tenantUserRepository,
                passwordEncoder,
                sessions,
                auditTrailService
        );

        TenantContextHolder.set(new TenantContext("talentohumano", "tenant_talentohumano", false));

        TenantUser tenantUser = new TenantUser(
                UUID.randomUUID(),
                "admin@talentohumano.com",
                "encoded-password",
                "Admin",
                "Tenant",
                true,
                TenantUserStatus.ACTIVE,
                1,
                Instant.now(),
                Instant.now()
        );

        when(tenantUserRepository.findByEmail("admin@talentohumano.com")).thenReturn(Optional.of(tenantUser));
        when(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(false);

        assertThrows(AuthenticationFailedException.class, () ->
                useCase.execute(new LoginRequest("admin@talentohumano.com", "Password123!"))
        );
    }
}
