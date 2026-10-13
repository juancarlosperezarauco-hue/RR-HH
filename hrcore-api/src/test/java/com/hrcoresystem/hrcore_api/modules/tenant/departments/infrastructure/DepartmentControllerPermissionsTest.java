package com.hrcoresystem.hrcore_api.modules.tenant.departments.infrastructure;

import com.hrcoresystem.hrcore_api.modules.tenant.departments.infrastructure.api.DepartmentController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.reflect.Method;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DepartmentControllerPermissionsTest {
    @Test
    void everyEndpointRequiresItsOwnAuthority() {
        assertEquals("bearerAuth", DepartmentController.class.getAnnotation(SecurityRequirement.class).name());
        Map<String, String> expected = Map.of(
                "create", "departments.create", "list", "departments.list", "get", "departments.detail",
                "update", "departments.update", "activate", "departments.activate",
                "deactivate", "departments.deactivate", "history", "departments.history.read");
        for (Method method : DepartmentController.class.getDeclaredMethods()) {
            if (expected.containsKey(method.getName())) {
                PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
                assertNotNull(annotation, method.getName());
                assertEquals("hasAuthority('" + expected.get(method.getName()) + "')", annotation.value());
            }
        }
    }
}
