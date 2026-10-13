package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hrcoresystem.hrcore_api.common.security.jwt.JwtAuthenticationFilter;
import com.hrcoresystem.hrcore_api.common.security.jwt.JwtTokenService;
import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
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
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokens, new TenancyProperties(), entry);

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void missingBearerTokenCannotReachAttendanceRoute() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(entry).commence(eq(request), eq(response), any());
        verifyNoInteractions(chain);
    }

    @Test
    void validTenantTokenCarriesGranularAttendanceAuthority() throws Exception {
        String userId = UUID.randomUUID().toString();
        when(tokens.parseAccessToken("valid")).thenReturn(new AuthenticatedUserPrincipal(
                userId, "test@example.test", "Test", "acme", List.of("ADMIN"),
                List.of("attendance.records.list")));
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Bearer valid");
        request.addHeader("X-Tenant-Slug", "acme");
        AtomicReference<Authentication> authentication = new AtomicReference<>();
        FilterChain chain = (req, res) -> authentication.set(SecurityContextHolder.getContext().getAuthentication());
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertNotNull(authentication.get());
        assertEquals(userId, ((AuthenticatedUserPrincipal) authentication.get().getPrincipal()).subject());
        assertTrue(authentication.get().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("attendance.records.list")));
        verifyNoInteractions(entry);
    }

    @Test
    void tokenForOtherTenantIsRejected() throws Exception {
        when(tokens.parseAccessToken("valid")).thenReturn(new AuthenticatedUserPrincipal(
                UUID.randomUUID().toString(), "test@example.test", "Test", "tenant_a",
                List.of(), List.of("attendance.records.list")));
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Bearer valid");
        request.addHeader("X-Tenant-Slug", "tenant_b");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(entry).commence(eq(request), eq(response), any());
        verifyNoInteractions(chain);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/attendance/records");
        request.setRequestURI("/api/attendance/records");
        return request;
    }
}
