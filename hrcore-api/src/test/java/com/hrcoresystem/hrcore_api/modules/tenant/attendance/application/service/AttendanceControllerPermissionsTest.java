package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.api.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

class AttendanceControllerPermissionsTest {
    @Test
    void everyAttendanceRouteHasItsPermissionAndBearerDocumentation() {
        assertProtected(AttendanceRecordController.class, Map.of(
                "list", "hasAuthority('attendance.records.list')",
                "detail", "hasAuthority('attendance.records.detail')",
                "calculate", "hasAuthority('attendance.records.calculate')"));
        assertProtected(AttendancePunchController.class, Map.of(
                "create", "hasAnyAuthority('attendance.punches.create','attendance.punches.manage')",
                "list", "hasAuthority('attendance.punches.list')",
                "detail", "hasAuthority('attendance.punches.detail')"));
        assertProtected(AttendanceAdjustmentController.class, Map.of(
                "create", "hasAuthority('attendance.adjustments.create')",
                "list", "hasAuthority('attendance.adjustments.list')",
                "detail", "hasAuthority('attendance.adjustments.detail')",
                "approve", "hasAuthority('attendance.adjustments.approve')",
                "reject", "hasAuthority('attendance.adjustments.reject')",
                "cancel", "hasAuthority('attendance.adjustments.cancel')"));
        assertProtected(AttendanceOvertimeController.class, Map.of(
                "create", "hasAuthority('attendance.overtime.create')",
                "list", "hasAuthority('attendance.overtime.list')",
                "detail", "hasAuthority('attendance.overtime.detail')",
                "approve", "hasAuthority('attendance.overtime.approve')",
                "reject", "hasAuthority('attendance.overtime.reject')",
                "cancel", "hasAuthority('attendance.overtime.cancel')"));
    }

    private void assertProtected(Class<?> controller, Map<String, String> expected) {
        SecurityRequirement security = controller.getAnnotation(SecurityRequirement.class);
        assertNotNull(security);
        assertEquals("bearerAuth", security.name());
        int routes = 0;
        for (Method method : controller.getDeclaredMethods()) {
            boolean mapped = method.isAnnotationPresent(GetMapping.class) || method.isAnnotationPresent(PostMapping.class)
                    || method.isAnnotationPresent(PatchMapping.class);
            if (!mapped) continue;
            routes++;
            assertTrue(expected.containsKey(method.getName()), method.getName());
            PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);
            assertNotNull(authorization, method.getName());
            assertEquals(expected.get(method.getName()), authorization.value());
        }
        assertEquals(expected.size(), routes);
    }
}
