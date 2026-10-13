package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.api.WorkShiftController;
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

class ShiftMethodSecurityTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shiftListRequiresAuthenticationAndExactAuthority() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            WorkShiftController controller = context.getBean(WorkShiftController.class);
            assertThrows(AuthenticationException.class,
                    () -> controller.list(null, null, null, 0, 10, "name", "asc"));

            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("shifts.detail"))));
            assertThrows(AccessDeniedException.class,
                    () -> controller.list(null, null, null, 0, 10, "name", "asc"));

            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("shifts.list"))));
            assertDoesNotThrow(() -> controller.list(null, null, null, 0, 10, "name", "asc"));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        WorkShiftController workShiftController() {
            return new WorkShiftController(mock(WorkShiftService.class), mock(ShiftBreakService.class));
        }
    }
}
