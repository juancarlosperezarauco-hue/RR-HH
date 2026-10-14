package com.hrcoresystem.hrcore_api.common.security.jwt;

import com.hrcoresystem.hrcore_api.common.security.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {
    private JwtTokenService service() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("this-is-a-very-secure-jwt-secret-key-with-more-than-32-chars");
        properties.setAccessExpirationMs(900000);
        properties.setRefreshExpirationMs(604800000);
        return new JwtTokenService(properties);
    }

    @Test
    void shouldGenerateAndParseAccessTokenSuccessfully() {
        UUID subject = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        Instant now = Instant.now();
        var token = service().generateAccessToken(new AccessTokenPayload(subject, session, "talentohumano", "TENANT",
                "admin@tenant.com", "Admin Tenant", List.of("ADMIN"), List.of("users.read"), 2, 3L, 4, now,
                now.plusSeconds(900), UUID.randomUUID()));
        var principal = service().parseAccessToken(token);
        assertEquals(subject, principal.subjectId());
        assertEquals(session, principal.sessionId());
        assertEquals("talentohumano", principal.tenantSlug());
        assertEquals(2, principal.securityVersion());
        assertEquals(3L, principal.authorizationVersion());
        assertEquals(4, principal.catalogVersion());
    }

    @Test
    void shouldGenerateAndParseRefreshTokenSuccessfully() {
        UUID subject = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        Instant now = Instant.now();
        var token = service().generateRefreshToken(new RefreshTokenPayload(subject, session, "talentohumano", "TENANT",
                1, now, now.plusSeconds(3600), UUID.randomUUID()));
        var parsed = service().parseRefreshTokenPayload(token);
        assertEquals(subject, parsed.subjectId());
        assertEquals(session, parsed.sessionId());
        assertEquals(1, parsed.generation());
    }
}
