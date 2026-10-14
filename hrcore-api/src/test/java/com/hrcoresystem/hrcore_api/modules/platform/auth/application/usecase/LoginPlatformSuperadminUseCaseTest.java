package com.hrcoresystem.hrcore_api.modules.platform.auth.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.session.AuthSessionService;
import com.hrcoresystem.hrcore_api.common.security.session.IssuedTokens;
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

        AuthSessionService sessions = mock(AuthSessionService.class);

        LoginPlatformSuperadminUseCase useCase = new LoginPlatformSuperadminUseCase(
                platformSuperadminRepository,
                passwordEncoder,
                sessions,
                auditTrailService
        );

        PlatformSuperadmin superadmin = new PlatformSuperadmin(
                UUID.randomUUID(),
                "superadmin@hrcore.local",
                "encoded-password",
                "Platform",
                "Admin",
                true,
                1,
                Instant.now(),
                Instant.now()
        );

        when(platformSuperadminRepository.findByEmail("superadmin@hrcore.local"))
                .thenReturn(Optional.of(superadmin));
        when(passwordEncoder.matches("SuperAdmin123!", "encoded-password")).thenReturn(true);
        when(sessions.createPlatformSession(superadmin))
                .thenReturn(new IssuedTokens("access", "refresh", 900000));

        PlatformAuthTokenResponse response = useCase.execute(
                new PlatformLoginRequest("superadmin@hrcore.local", "SuperAdmin123!")
        );

        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        verify(sessions).createPlatformSession(superadmin);

        verify(auditTrailService).recordPlatformEvent(anyString(), anyString(), anyString(), any());
    }
}
