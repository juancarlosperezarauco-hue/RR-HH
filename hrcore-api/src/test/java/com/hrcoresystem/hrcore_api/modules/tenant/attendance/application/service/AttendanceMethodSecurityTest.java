package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.api.AttendanceRecordController;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AttendanceMethodSecurityTest {
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void attendanceListRequiresAuthenticationAndExactAuthority() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            AttendanceRecordController controller = context.getBean(AttendanceRecordController.class);
            assertThrows(AuthenticationException.class,
                    () -> controller.list(null, null, null, null, 0, 10, "workDate", "asc"));
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("attendance.records.detail"))));
            assertThrows(AccessDeniedException.class,
                    () -> controller.list(null, null, null, null, 0, 10, "workDate", "asc"));
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
            assertThrows(AccessDeniedException.class,
                    () -> controller.list(null, null, null, null, 0, 10, "workDate", "asc"));
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("attendance.records.list"))));
            assertDoesNotThrow(() -> controller.list(null, null, null, null, 0, 10, "workDate", "asc"));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfig {
        @Bean AttendanceRecordController attendanceRecordController() {
            return new AttendanceRecordController(mock(AttendanceCalculationService.class));
        }
    }
}
