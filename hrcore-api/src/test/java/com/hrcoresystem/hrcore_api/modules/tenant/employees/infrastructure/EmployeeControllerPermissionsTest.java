package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.api.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Method;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EmployeeControllerPermissionsTest {
    @Test void allEmployeeEndpointsHaveRequiredAuthorities() {
        verify(EmployeeController.class, Map.of(
                "create", "employees.create", "list", "employees.list", "get", "employees.detail",
                "update", "employees.update", "status", "employees.status.update",
                "createWithAssignment", "hasAuthority('employees.create') and hasAuthority('employees.assignments.create')"));
        verify(EmployeeAssignmentController.class, Map.of(
                "create", "employees.assignments.create", "list", "employees.assignments.list",
                "get", "employees.assignments.detail", "update", "employees.assignments.update",
                "close", "employees.assignments.close"));
        verify(EmployeeContactController.class, Map.of(
                "create", "employees.contacts.create", "list", "employees.contacts.list",
                "get", "employees.contacts.detail", "update", "employees.contacts.update",
                "delete", "employees.contacts.delete"));
    }
    private void verify(Class<?> controller, Map<String, String> expected) {
        assertEquals("bearerAuth", controller.getAnnotation(SecurityRequirement.class).name());
        int count = 0;
        for (Method method : controller.getDeclaredMethods()) {
            if (!expected.containsKey(method.getName())) continue;
            count++;
            String expectedAuthorization = expected.get(method.getName());
            if (!expectedAuthorization.startsWith("hasAuthority(")) {
                expectedAuthorization = "hasAuthority('" + expectedAuthorization + "')";
            }
            assertEquals(expectedAuthorization, method.getAnnotation(PreAuthorize.class).value());
            assertTrue(method.isAnnotationPresent(GetMapping.class) || method.isAnnotationPresent(PostMapping.class)
                    || method.isAnnotationPresent(PutMapping.class) || method.isAnnotationPresent(PatchMapping.class)
                    || method.isAnnotationPresent(DeleteMapping.class));
        }
        assertEquals(expected.size(), count);
    }
}
