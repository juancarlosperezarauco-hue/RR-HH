
package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hrcoresystem.hrcore_api.common.security.jwt.JwtAuthenticationFilter;
import com.hrcoresystem.hrcore_api.common.security.jwt.JwtTokenService;
import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import com.hrcoresystem.hrcore_api.common.security.session.AuthenticatedSessionValidator;
import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;

import jakarta.servlet.FilterChain;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

class AttendanceJwtFilterTest {

  private final JwtTokenService tokens = mock(JwtTokenService.class);

  private final AuthenticationEntryPoint entry = mock(AuthenticationEntryPoint.class);

  private final AuthenticatedSessionValidator sessionValidator = mock(AuthenticatedSessionValidator.class);

  private final TenancyProperties tenancyProperties = new TenancyProperties();

  private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
      tokens,
      tenancyProperties,
      entry,
      sessionValidator);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
    TenantContextHolder.clear();
  }

  @Test
  void missingBearerTokenCannotReachAttendanceRoute()
      throws Exception {

    MockHttpServletRequest request = request();
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(entry).commence(
        eq(request),
        eq(response),
        any());

    verifyNoInteractions(chain);
    verifyNoInteractions(sessionValidator);
  }

  @Test
  void validTenantTokenCarriesGranularAttendanceAuthority()
      throws Exception {

    UUID userId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();

    AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
        userId,
        sessionId,
        "test@example.test",
        "Test",
        "acme",
        "TENANT",
        List.of("HR_OPERATOR"),
        List.of("attendance.records.list"),
        1L,
        1L,
        1L);

    when(tokens.parseAccessToken("valid"))
        .thenReturn(principal);

    TenantContextHolder.set(
        new TenantContext(
            "acme",
            "tenant_acme",
            false));

    MockHttpServletRequest request = request();

    request.addHeader(
        "Authorization",
        "Bearer valid");

    request.addHeader(
        "X-Tenant-Slug",
        "acme");

    AtomicReference<Authentication> authentication = new AtomicReference<>();

    FilterChain chain = (req, res) -> authentication.set(
        SecurityContextHolder.getContext()
            .getAuthentication());

    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        chain);

    assertNotNull(authentication.get());

    AuthenticatedUserPrincipal actualPrincipal = (AuthenticatedUserPrincipal) authentication.get().getPrincipal();

    assertEquals(
        userId,
        actualPrincipal.subjectId());

    assertEquals(
        sessionId,
        actualPrincipal.sessionId());

    assertEquals(
        "TENANT",
        actualPrincipal.scope());

    assertTrue(
        authentication.get()
            .getAuthorities()
            .stream()
            .anyMatch(authority -> authority.getAuthority()
                .equals("attendance.records.list")));

    assertTrue(
        authentication.get()
            .getAuthorities()
            .stream()
            .anyMatch(authority -> authority.getAuthority()
                .equals("ROLE_HR_OPERATOR")));

    verify(sessionValidator).validate(principal);
    verifyNoInteractions(entry);
  }

  @Test
  void tokenForOtherTenantIsRejected()
      throws Exception {

    AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "test@example.test",
        "Test",
        "tenant_a",
        "TENANT",
        List.of(),
        List.of("attendance.records.list"),
        1L,
        1L,
        1L);

    when(tokens.parseAccessToken("valid"))
        .thenReturn(principal);

    TenantContextHolder.set(
        new TenantContext(
            "tenant_b",
            "tenant_tenant_b",
            false));

    MockHttpServletRequest request = request();

    request.addHeader(
        "Authorization",
        "Bearer valid");

    request.addHeader(
        "X-Tenant-Slug",
        "tenant_b");

    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(entry).commence(
        eq(request),
        eq(response),
        any());

    verifyNoInteractions(chain);
  }

  @Test
  void revokedSessionCannotReachAttendanceRoute()
      throws Exception {

    AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "test@example.test",
        "Test",
        "acme",
        "TENANT",
        List.of("HR_OPERATOR"),
        List.of("attendance.records.list"),
        1L,
        1L,
        1L);

    when(tokens.parseAccessToken("valid"))
        .thenReturn(principal);

    doThrow(new IllegalStateException(
        "Session is not active")).when(sessionValidator).validate(principal);

    TenantContextHolder.set(
        new TenantContext(
            "acme",
            "tenant_acme",
            false));

    MockHttpServletRequest request = request();

    request.addHeader(
        "Authorization",
        "Bearer valid");

    request.addHeader(
        "X-Tenant-Slug",
        "acme");

    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(sessionValidator).validate(principal);

    verify(entry).commence(
        eq(request),
        eq(response),
        any());

    verifyNoInteractions(chain);

    assertNull(
        SecurityContextHolder.getContext()
            .getAuthentication());
  }

  private MockHttpServletRequest request() {

    MockHttpServletRequest request = new MockHttpServletRequest(
        "GET",
        "/api/attendance/records");

    request.setRequestURI(
        "/api/attendance/records");

    return request;
  }
}
