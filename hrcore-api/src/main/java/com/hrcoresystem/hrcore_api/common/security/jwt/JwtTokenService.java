package com.hrcoresystem.hrcore_api.common.security.jwt;

import com.hrcoresystem.hrcore_api.common.security.JwtProperties;
import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class JwtTokenService {

    private static final Pattern ROLE_NAME_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");
    private static final Pattern PERMISSION_CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9-]*(?:[.][a-z0-9-]+)*$");
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String subject, String tenantSlug, List<String> roles) {
        return generateAccessToken(subject, null, null, tenantSlug, roles, List.of());
    }

    public String generateAccessToken(String subject, String email, String tenantSlug, List<String> roles) {
        return generateAccessToken(subject, email, null, tenantSlug, roles, List.of());
    }

    public String generateAccessToken(
            String subject,
            String email,
            String displayName,
            String tenantSlug,
            List<String> roles,
            List<String> permissions
    ) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtProperties.getAccessExpirationMs());

        return Jwts.builder()
                .setSubject(requireText(subject, "subject"))
                .addClaims(buildAccessClaims(email, displayName, tenantSlug, roles, permissions))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String subject, String tenantSlug) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtProperties.getRefreshExpirationMs());

        return Jwts.builder()
                .setSubject(requireText(subject, "subject"))
                .addClaims(Map.of(
                        JwtClaimNames.TENANT, requireText(tenantSlug, "tenantSlug"),
                        JwtClaimNames.TOKEN_TYPE, JwtClaimNames.REFRESH
                ))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public AuthenticatedUserPrincipal parseAccessToken(String token) {
        Claims claims = parseClaims(token);

        String tokenType = claims.get(JwtClaimNames.TOKEN_TYPE, String.class);
        if (!JwtClaimNames.ACCESS.equals(tokenType)) {
            throw new JwtException("Invalid token type");
        }

        String subject = claims.getSubject();
        String email = claims.get(JwtClaimNames.EMAIL, String.class);
        String displayName = claims.get(JwtClaimNames.DISPLAY_NAME, String.class);
        String tenantSlug = claims.get(JwtClaimNames.TENANT, String.class);

        List<String> roles = normalizeRoleNames(readStringListClaim(claims, JwtClaimNames.ROLES));
        List<String> permissions = normalizePermissionCodes(readStringListClaim(claims, JwtClaimNames.PERMISSIONS));

        if (!StringUtils.hasText(subject)) {
            throw new JwtException("Token subject is missing");
        }

        if (!StringUtils.hasText(tenantSlug)) {
            throw new JwtException("Token tenant is missing");
        }

        return new AuthenticatedUserPrincipal(
                subject,
                normalizeOptionalText(email),
                displayName,
                tenantSlug,
                roles,
                permissions
        );
    }

    public Claims parseRefreshToken(String token) {
        Claims claims = parseClaims(token);

        String tokenType = claims.get(JwtClaimNames.TOKEN_TYPE, String.class);
        if (!JwtClaimNames.REFRESH.equals(tokenType)) {
            throw new JwtException("Invalid refresh token type");
        }

        return claims;
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new JwtException("Token " + fieldName + " is missing");
        }

        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private Map<String, Object> buildAccessClaims(
            String email,
            String displayName,
            String tenantSlug,
            List<String> roles,
            List<String> permissions
    ) {
        Map<String, Object> claims = new LinkedHashMap<>();
        String normalizedEmail = normalizeOptionalText(email);
        if (normalizedEmail != null) {
            claims.put(JwtClaimNames.EMAIL, normalizedEmail);
        }
        String normalizedDisplayName = normalizeOptionalText(displayName);
        if (normalizedDisplayName != null) {
            claims.put(JwtClaimNames.DISPLAY_NAME, normalizedDisplayName);
        }
        claims.put(JwtClaimNames.TENANT, requireText(tenantSlug, "tenantSlug"));
        claims.put(JwtClaimNames.ROLES, normalizeRoleNames(roles));
        claims.put(JwtClaimNames.PERMISSIONS, normalizePermissionCodes(permissions));
        claims.put(JwtClaimNames.TOKEN_TYPE, JwtClaimNames.ACCESS);
        return claims;
    }

    private List<String> readStringListClaim(Claims claims, String claimName) {
        Object claimValue = claims.get(claimName);
        if (claimValue == null) {
            return List.of();
        }

        if (!(claimValue instanceof List<?> rawList)) {
            throw new JwtException("Token claim '" + claimName + "' must be an array");
        }

        List<String> values = rawList.stream()
                .map(item -> {
                    if (!(item instanceof String stringValue)) {
                        throw new JwtException("Token claim '" + claimName + "' must contain only strings");
                    }
                    return stringValue;
                })
                .toList();

        return values;
    }

    private List<String> normalizeRoleNames(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalizedRoles = new LinkedHashSet<>();
        for (String roleName : roleNames) {
            String normalizedRole = normalizeRoleName(roleName);
            if (!ROLE_NAME_PATTERN.matcher(normalizedRole).matches()) {
                throw new JwtException("Invalid role name in token: " + roleName);
            }
            normalizedRoles.add(normalizedRole);
        }

        return List.copyOf(normalizedRoles);
    }

    private String normalizeRoleName(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            throw new JwtException("Token roles contain blank values");
        }

        String normalizedRole = roleName.trim();
        if (normalizedRole.startsWith(ROLE_PREFIX)) {
            normalizedRole = normalizedRole.substring(ROLE_PREFIX.length());
        }

        if (!StringUtils.hasText(normalizedRole)) {
            throw new JwtException("Token roles contain blank values");
        }

        return normalizedRole;
    }

    private List<String> normalizePermissionCodes(List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalizedPermissions = new LinkedHashSet<>();
        for (String permissionCode : permissionCodes) {
            if (!StringUtils.hasText(permissionCode)) {
                throw new JwtException("Token permissions contain blank values");
            }

            String normalizedPermission = permissionCode.trim();
            if (!PERMISSION_CODE_PATTERN.matcher(normalizedPermission).matches()) {
                throw new JwtException("Invalid permission code in token: " + permissionCode);
            }

            normalizedPermissions.add(normalizedPermission);
        }

        return List.copyOf(normalizedPermissions);
    }
}
