package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.api.EffectiveScheduleController;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.api.ShiftAssignmentController;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.api.WorkCalendarExceptionController;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.api.WorkShiftController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

class ShiftControllerPermissionsTest {
    @Test
    void everyShiftRouteHasItsOwnAuthorityAndBearerDocumentation() {
        assertProtected(WorkShiftController.class, Map.ofEntries(
                Map.entry("create", "shifts.create"), Map.entry("list", "shifts.list"),
                Map.entry("detail", "shifts.detail"), Map.entry("update", "shifts.update"),
                Map.entry("activate", "shifts.activate"), Map.entry("deactivate", "shifts.deactivate"),
                Map.entry("createBreak", "shifts.breaks.create"), Map.entry("listBreaks", "shifts.breaks.list"),
                Map.entry("detailBreak", "shifts.breaks.detail"), Map.entry("updateBreak", "shifts.breaks.update"),
                Map.entry("deleteBreak", "shifts.breaks.delete")));
        assertProtected(ShiftAssignmentController.class, Map.of(
                "create", "shifts.assignments.create", "list", "shifts.assignments.list",
                "detail", "shifts.assignments.detail", "update", "shifts.assignments.update",
                "cancel", "shifts.assignments.cancel", "close", "shifts.assignments.close"));
        assertProtected(WorkCalendarExceptionController.class, Map.of(
                "create", "shifts.calendar.create", "list", "shifts.calendar.list",
                "detail", "shifts.calendar.detail", "update", "shifts.calendar.update",
                "delete", "shifts.calendar.delete"));
        assertProtected(EffectiveScheduleController.class, Map.of("get", "shifts.schedule.read"));
    }

    private void assertProtected(Class<?> controller, Map<String, String> expected) {
        SecurityRequirement security = controller.getAnnotation(SecurityRequirement.class);
        assertNotNull(security);
        assertEquals("bearerAuth", security.name());
        int routes = 0;
        for (Method method : controller.getDeclaredMethods()) {
            boolean mapped = method.isAnnotationPresent(GetMapping.class) || method.isAnnotationPresent(PostMapping.class)
                    || method.isAnnotationPresent(PutMapping.class) || method.isAnnotationPresent(PatchMapping.class)
                    || method.isAnnotationPresent(DeleteMapping.class);
            if (!mapped) continue;
            routes++;
            assertTrue(expected.containsKey(method.getName()), method.getName());
            PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);
            assertNotNull(authorization, method.getName());
            assertEquals("hasAuthority('" + expected.get(method.getName()) + "')", authorization.value());
        }
        assertEquals(expected.size(), routes);
    }
}
