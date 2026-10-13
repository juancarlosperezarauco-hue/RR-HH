package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.api.EmployeeFileController;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class EmployeeFileMethodSecurityTest {
  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void requiresAuthenticationAndExactAuthority() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var controller = context.getBean(EmployeeFileController.class);
      assertThrows(
          AuthenticationException.class,
          () -> controller.list(null, null, null, PageRequest.of(0, 10)));
      SecurityContextHolder.getContext()
          .setAuthentication(
              new UsernamePasswordAuthenticationToken(
                  "user", "n/a", List.of(new SimpleGrantedAuthority("employee_files.detail"))));
      assertThrows(
          AccessDeniedException.class,
          () -> controller.list(null, null, null, PageRequest.of(0, 10)));
      SecurityContextHolder.getContext()
          .setAuthentication(
              new UsernamePasswordAuthenticationToken(
                  "user", "n/a", List.of(new SimpleGrantedAuthority("employee_files.list"))));
      assertDoesNotThrow(() -> controller.list(null, null, null, PageRequest.of(0, 10)));
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableMethodSecurity
  static class Config {
    @Bean
    EmployeeFileController controller() {
      return new EmployeeFileController(
          mock(EmployeeFileManagementService.class),
          mock(EmployeeDocumentManagementService.class),
          mock(DocumentVersionService.class));
    }
  }
}
