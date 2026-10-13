package com.hrcoresystem.hrcore_api.modules.tenant.leaves;

import static org.junit.jupiter.api.Assertions.*;

import com.hrcoresystem.hrcore_api.modules.tenant.leaves.infrastructure.api.LeaveController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

class LeaveControllerPermissionsTest {
    @Test void everyRouteUsesItsDedicatedPermissionAndDocumentsBearerAuth() {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("createType", "leaves.types.create"), Map.entry("types", "leaves.types.list"),
                Map.entry("type", "leaves.types.detail"), Map.entry("updateType", "leaves.types.update"),
                Map.entry("activate", "leaves.types.activate"), Map.entry("deactivate", "leaves.types.deactivate"),
                Map.entry("createBalance", "leaves.balances.create"), Map.entry("balances", "leaves.balances.list"),
                Map.entry("balance", "leaves.balances.detail"), Map.entry("adjust", "leaves.balances.adjust"),
                Map.entry("createRequest", "leaves.requests.create"), Map.entry("requests", "leaves.requests.list"),
                Map.entry("request", "leaves.requests.detail"), Map.entry("updateRequest", "leaves.requests.update"),
                Map.entry("submit", "leaves.requests.submit"), Map.entry("approve", "leaves.requests.approve"),
                Map.entry("reject", "leaves.requests.reject"), Map.entry("cancel", "leaves.requests.cancel"),
                Map.entry("history", "leaves.history.read"), Map.entry("upload", "leaves.attachments.upload"),
                Map.entry("attachments", "leaves.attachments.list"),
                Map.entry("download", "leaves.attachments.download"));
        assertEquals("bearerAuth", LeaveController.class.getAnnotation(SecurityRequirement.class).name());
        int routes = 0;
        for (Method method : LeaveController.class.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(GetMapping.class) && !method.isAnnotationPresent(PostMapping.class)
                    && !method.isAnnotationPresent(PutMapping.class) && !method.isAnnotationPresent(PatchMapping.class))
                continue;
            routes++;
            assertTrue(expected.containsKey(method.getName()), method.getName());
            PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);
            assertNotNull(authorization, method.getName());
            assertEquals("hasAuthority('" + expected.get(method.getName()) + "')", authorization.value());
        }
        assertEquals(expected.size(), routes);
    }
}
