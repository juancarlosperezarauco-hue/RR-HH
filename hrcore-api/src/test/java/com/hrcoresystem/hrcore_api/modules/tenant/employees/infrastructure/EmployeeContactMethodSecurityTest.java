package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.api.EmployeeContactController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeContactMethodSecurityTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void privateContactNeedsAuthenticationAndExactPermission() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            EmployeeContactController controller = context.getBean(EmployeeContactController.class);
            UUID employeeId = UUID.randomUUID(), contactId = UUID.randomUUID();
            assertThrows(AuthenticationException.class, () -> controller.get(employeeId, contactId));
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("employees.detail"))));
            assertThrows(AccessDeniedException.class, () -> controller.get(employeeId, contactId));
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("employees.contacts.detail"))));
            assertDoesNotThrow(() -> controller.get(employeeId, contactId));
        }
    }
    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfig {
        @Bean EmployeeContactController contactController() {
            return new EmployeeContactController(mock(CreateEmployeeContactUseCase.class),
                    mock(ListEmployeeContactsUseCase.class), mock(GetEmployeeContactUseCase.class),
                    mock(UpdateEmployeeContactUseCase.class), mock(DeleteEmployeeContactUseCase.class));
        }
    }
}
