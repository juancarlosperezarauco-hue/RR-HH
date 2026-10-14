package com.hrcoresystem.hrcore_api.common.security.jwt;

import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import com.hrcoresystem.hrcore_api.common.security.session.AuthenticatedSessionValidator;
import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.NoSuchElementException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenService jwtTokenService;
  private final TenancyProperties tenancyProperties;
  private final AuthenticationEntryPoint authenticationEntryPoint;
  private final AuthenticatedSessionValidator sessionValidator;

  private final JwtAuthorityMapper authorityMapper = new JwtAuthorityMapper();

  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  // Único constructor: todas las dependencias son obligatorias.
  public JwtAuthenticationFilter(
      JwtTokenService jwtTokenService,
      TenancyProperties tenancyProperties,
      AuthenticationEntryPoint authenticationEntryPoint,
      AuthenticatedSessionValidator sessionValidator) {
    this.jwtTokenService = jwtTokenService;
    this.tenancyProperties = tenancyProperties;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.sessionValidator = sessionValidator;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();

    if (tenancyProperties.isPublicPath(uri)) {
      return true;
    }

    return pathMatcher.match("/api/auth/login", uri)
        || pathMatcher.match("/api/auth/refresh", uri)
        || pathMatcher.match("/api/auth/forgot-password", uri)
        || pathMatcher.match("/api/auth/reset-password", uri)
        || pathMatcher.match("/api/auth/activate-account", uri)
        || pathMatcher.match("/api/auth/resend-activation", uri)
        || pathMatcher.match("/api/auth/face/login", uri)
        || pathMatcher.match("/api/platform/auth/login", uri)
        || pathMatcher.match("/api/platform/auth/refresh", uri);
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    try {
      String authorizationHeader = request.getHeader("Authorization");

      if (!StringUtils.hasText(authorizationHeader)
          || !authorizationHeader.startsWith(BEARER_PREFIX)) {

        throw new InsufficientAuthenticationException(
            "Missing or invalid Authorization header");
      }

      String token = authorizationHeader.substring(
          BEARER_PREFIX.length());

      if (!StringUtils.hasText(token)) {
        throw new BadCredentialsException(
            "Access token is missing");
      }

      // 1. Validar firma, expiración y claims del JWT.
      AuthenticatedUserPrincipal principal = jwtTokenService.parseAccessToken(token);

      // 2. Validar obligatoriamente la sesión persistida.
      validateSession(principal);

      // 3. Verificar que el token pertenece al ámbito correcto.
      validateRequestScope(principal, request);

      // 4. Construir las autoridades del usuario.
      var authorities = authorityMapper.toAuthorities(principal);

      // 5. Crear la autenticación.
      UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
          principal,
          null,
          authorities);

      SecurityContextHolder.getContext()
          .setAuthentication(authentication);

    } catch (JwtException | AuthenticationException ex) {

      SecurityContextHolder.clearContext();

      authenticationEntryPoint.commence(
          request,
          response,
          new BadCredentialsException(
              "Invalid or expired authentication",
              ex));

      return;
    }

    // No capturar aquí errores de controladores o casos de uso.
    filterChain.doFilter(request, response);
  }

  private void validateSession(
      AuthenticatedUserPrincipal principal) {

    try {
      sessionValidator.validate(principal);

    } catch (IllegalStateException | NoSuchElementException ex) {

      throw new BadCredentialsException(
          "Authentication session is invalid",
          ex);
    }
  }

  private void validateRequestScope(
      AuthenticatedUserPrincipal principal,
      HttpServletRequest request) {

    String scope = principal.scope();
    String tenantSlug = principal.tenantSlug();
    String requestUri = request.getRequestURI();

    boolean platformRequest = isPlatformRequest(requestUri);

    if ("PLATFORM".equals(scope)) {

      if (!"platform".equals(tenantSlug)) {
        throw new BadCredentialsException(
            "Invalid platform token");
      }

      if (!platformRequest) {
        throw new BadCredentialsException(
            "Platform token cannot access tenant resources");
      }

      return;
    }

    if (!"TENANT".equals(scope)) {
      throw new BadCredentialsException(
          "Invalid authentication scope");
    }

    if (!StringUtils.hasText(tenantSlug)
        || "platform".equalsIgnoreCase(tenantSlug)) {

      throw new BadCredentialsException(
          "Invalid tenant token");
    }

    if (platformRequest) {
      throw new BadCredentialsException(
          "Tenant token cannot access platform resources");
    }

    // El tenant debe haber sido resuelto por el filtro anterior.
    TenantContext tenantContext = TenantContextHolder.get();

    if (tenantContext == null
        || tenantContext.publicRequest()
        || !StringUtils.hasText(tenantContext.tenantSlug())) {

      throw new BadCredentialsException(
          "Tenant context is missing");
    }

    if (!tenantSlug.equals(tenantContext.tenantSlug())) {
      throw new BadCredentialsException(
          "Token tenant does not match request tenant");
    }

    String tenantHeader = request.getHeader(
        tenancyProperties.getHeaderName());

    if (!StringUtils.hasText(tenantHeader)) {
      throw new BadCredentialsException(
          "Tenant header is missing");
    }
  }

  private boolean isPlatformRequest(String requestUri) {

    return tenancyProperties.isGlobalPath(requestUri)
        || pathMatcher.match(
            "/api/dashboard/superadmin/**",
            requestUri);
  }
}
