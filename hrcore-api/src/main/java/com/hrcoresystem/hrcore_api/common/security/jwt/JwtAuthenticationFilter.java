package com.hrcoresystem.hrcore_api.common.security.jwt;

import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.modules.platform.auth.domain.model.PlatformAuthConstants;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final TenancyProperties tenancyProperties;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            TenancyProperties tenancyProperties,
            AuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.jwtTokenService = jwtTokenService;
        this.tenancyProperties = tenancyProperties;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        if (tenancyProperties.isPublicPath(requestUri)) {
            return true;
        }

        return antPathMatcher.match("/api/auth/login", requestUri)
                || antPathMatcher.match("/api/auth/refresh", requestUri)
                || antPathMatcher.match("/api/auth/forgot-password", requestUri)
                || antPathMatcher.match("/api/auth/reset-password", requestUri)
                || antPathMatcher.match("/api/auth/activate-account", requestUri)
                || antPathMatcher.match("/api/auth/resend-activation", requestUri)
                || antPathMatcher.match("/api/auth/face/login", requestUri)
                || antPathMatcher.match("/api/platform/auth/login", requestUri)
                || antPathMatcher.match("/api/platform/auth/refresh", requestUri);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String authorizationHeader = request.getHeader("Authorization");

            if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
                throw new InsufficientAuthenticationException("Missing or invalid Authorization header");
            }

            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            AuthenticatedUserPrincipal principal = jwtTokenService.parseAccessToken(token);

            String requestUri = request.getRequestURI();
            if (isPlatformToken(principal)) {
                if (!isPlatformRequest(requestUri)) {
                    throw new BadCredentialsException("Platform token cannot be used for tenant resources");
                }
            } else {
                String tenantHeaderValue = request.getHeader(tenancyProperties.getHeaderName());
                if (StringUtils.hasText(tenantHeaderValue)
                        && !tenantHeaderValue.equalsIgnoreCase(principal.tenantSlug())) {
                    throw new BadCredentialsException("Token tenant does not match request tenant header");
                }
            }

            List<SimpleGrantedAuthority> authorities = buildAuthorities(principal);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (JwtException | InsufficientAuthenticationException | BadCredentialsException ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException(ex.getMessage(), ex)
            );
        }
    }

    private List<SimpleGrantedAuthority> buildAuthorities(AuthenticatedUserPrincipal principal) {
        LinkedHashSet<String> authorityValues = new LinkedHashSet<>();

        for (String role : principal.roles()) {
            if (!StringUtils.hasText(role)) {
                throw new JwtException("Token roles contain blank values");
            }

            String normalizedRole = role.trim();
            if (!normalizedRole.startsWith("ROLE_")) {
                normalizedRole = "ROLE_" + normalizedRole.toUpperCase();
            }

            authorityValues.add(normalizedRole);
        }

        for (String permission : principal.permissions()) {
            if (!StringUtils.hasText(permission)) {
                throw new JwtException("Token permissions contain blank values");
            }

            authorityValues.add(permission.trim());
        }

        return authorityValues.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private boolean isPlatformToken(AuthenticatedUserPrincipal principal) {
        return principal != null
                && principal.tenantSlug() != null
                && PlatformAuthConstants.PLATFORM_TENANT_SLUG.equalsIgnoreCase(principal.tenantSlug());
    }

    private boolean isPlatformRequest(String requestUri) {
        return antPathMatcher.match("/api/platform/**", requestUri)
                || antPathMatcher.match("/api/dashboard/superadmin/**", requestUri);
    }
}
