package com.hrcoresystem.hrcore_api.common.security.jwt;

import com.hrcoresystem.hrcore_api.common.security.JwtProperties;
import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    @Test
    void shouldGenerateAndParseAccessTokenSuccessfully() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("this-is-a-very-secure-jwt-secret-key-with-more-than-32-chars");
        properties.setAccessExpirationMs(900000);
        properties.setRefreshExpirationMs(604800000);

        JwtTokenService jwtTokenService = new JwtTokenService(properties);

        String token = jwtTokenService.generateAccessToken(
                "admin@tenant.com",
                "financruz",
                List.of("ADMIN", "USER")
        );

        AuthenticatedUserPrincipal principal = jwtTokenService.parseAccessToken(token);

        assertEquals("admin@tenant.com", principal.subject());
        assertNull(principal.email());
        assertEquals("financruz", principal.tenantSlug());
        assertEquals(List.of("ADMIN", "USER"), principal.roles());
    }

    @Test
    void shouldGenerateAndParseAccessTokenWithEmailSuccessfully() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("this-is-a-very-secure-jwt-secret-key-with-more-than-32-chars");
        properties.setAccessExpirationMs(900000);
        properties.setRefreshExpirationMs(604800000);

        JwtTokenService jwtTokenService = new JwtTokenService(properties);

        String token = jwtTokenService.generateAccessToken(
                "admin@tenant.com",
                "admin@tenant.com",
                "Admin Tenant",
                "financruz",
                List.of("ADMIN"),
                List.of("users.read")
        );

        AuthenticatedUserPrincipal principal = jwtTokenService.parseAccessToken(token);

        assertEquals("admin@tenant.com", principal.subject());
        assertEquals("admin@tenant.com", principal.email());
        assertEquals("Admin Tenant", principal.displayName());
        assertEquals("financruz", principal.tenantSlug());
        assertEquals(List.of("ADMIN"), principal.roles());
        assertEquals(List.of("users.read"), principal.permissions());
    }

    @Test
    void shouldGenerateAndParseRefreshTokenSuccessfully() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("this-is-a-very-secure-jwt-secret-key-with-more-than-32-chars");
        properties.setAccessExpirationMs(900000);
        properties.setRefreshExpirationMs(604800000);

        JwtTokenService jwtTokenService = new JwtTokenService(properties);

        String token = jwtTokenService.generateRefreshToken(
                "admin@tenant.com",
                "financruz"
        );

        Claims claims = jwtTokenService.parseRefreshToken(token);

        assertEquals("admin@tenant.com", claims.getSubject());
        assertEquals("financruz", claims.get(JwtClaimNames.TENANT, String.class));
        assertEquals(JwtClaimNames.REFRESH, claims.get(JwtClaimNames.TOKEN_TYPE, String.class));
    }
}
