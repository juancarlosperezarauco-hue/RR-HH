package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles;

import static org.junit.jupiter.api.Assertions.*;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.api.EmployeeFileController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

class EmployeeFileControllerPermissionsTest {
  @Test
  void everyEndpointHasPermissionAndBearerDocumentation() {
    assertNotNull(EmployeeFileController.class.getAnnotation(SecurityRequirement.class));
    Set<String> authorities = new HashSet<>();
    int routes = 0;
    for (var method : EmployeeFileController.class.getDeclaredMethods()) {
      if (method.isAnnotationPresent(GetMapping.class)
          || method.isAnnotationPresent(PostMapping.class)
          || method.isAnnotationPresent(PutMapping.class)
          || method.isAnnotationPresent(PatchMapping.class)) {
        routes++;
        var rule = method.getAnnotation(PreAuthorize.class);
        assertNotNull(rule, method.getName());
        assertTrue(rule.value().startsWith("hasAuthority('employee_files."));
        authorities.add(rule.value());
      }
    }
    assertEquals(13, routes);
    assertEquals(11, authorities.size());
  }
}
