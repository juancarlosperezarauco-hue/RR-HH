package com.hrcoresystem.hrcore_api.modules.platform.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.JwtProperties;
import com.hrcoresystem.hrcore_api.common.security.jwt.JwtTokenService;
import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.auth.application.dto.PlatformAuthTokenResponse;
import com.hrcoresystem.hrcore_api.modules.platform.auth.application.dto.PlatformLoginRequest;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.model.PlatformSuperadmin;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.repository.PlatformSuperadminRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LoginPlatformSuperadminUseCaseTest {

    @Test
    void shouldLoginPlatformSuperadminSuccessfully() {
        PlatformSuperadminRepository platformSuperadminRepository = mock(PlatformSuperadminRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuditTrailService auditTrailService = mock(AuditTrailService.class);

        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("this-is-a-very-secure-jwt-secret-key-with-more-than-32-chars");
        jwtProperties.setAccessExpirationMs(900000);
        jwtProperties.setRefreshExpirationMs(604800000);

        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        LoginPlatformSuperadminUseCase useCase = new LoginPlatformSuperadminUseCase(
                platformSuperadminRepository,
                passwordEncoder,
                jwtTokenService,
                jwtProperties,
                auditTrailService
        );

        PlatformSuperadmin superadmin = new PlatformSuperadmin(
                UUID.randomUUID(),
                "superadmin@hrcore.local",
                "encoded-password",
                "Platform",
                "Admin",
                true,
                Instant.now(),
                Instant.now()
        );

        when(platformSuperadminRepository.findByEmail("superadmin@hrcore.local"))
                .thenReturn(Optional.of(superadmin));
        when(passwordEncoder.matches("SuperAdmin123!", "encoded-password")).thenReturn(true);

        PlatformAuthTokenResponse response = useCase.execute(
                new PlatformLoginRequest("superadmin@hrcore.local", "SuperAdmin123!")
        );

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals("Bearer", response.tokenType());

        AuthenticatedUserPrincipal principal = jwtTokenService.parseAccessToken(response.accessToken());
        assertEquals("superadmin@hrcore.local", principal.subject());
        assertEquals("platform", principal.tenantSlug());
        assertTrue(principal.roles().contains("SUPERADMIN"));

        verify(auditTrailService).recordPlatformEvent(anyString(), anyString(), anyString(), any());
    }
}