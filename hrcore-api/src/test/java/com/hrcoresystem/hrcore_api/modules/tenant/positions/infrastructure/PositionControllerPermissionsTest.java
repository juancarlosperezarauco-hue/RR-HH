package com.hrcoresystem.hrcore_api.modules.tenant.positions.infrastructure;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.infrastructure.api.PositionController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Method;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PositionControllerPermissionsTest {
    @Test
    void everyEndpointRequiresItsOwnAuthority() {
        assertEquals("bearerAuth", PositionController.class.getAnnotation(SecurityRequirement.class).name());
        Map<String, String> expected = Map.of(
                "create", "positions.create", "list", "positions.list", "get", "positions.detail",
                "update", "positions.update", "activate", "positions.activate",
                "deactivate", "positions.deactivate", "listRequirements", "positions.requirements.list",
                "createRequirement", "positions.requirements.create",
                "updateRequirement", "positions.requirements.update",
                "deleteRequirement", "positions.requirements.delete");
        int protectedCount = 0;
        for (Method method : PositionController.class.getDeclaredMethods()) {
            if (!expected.containsKey(method.getName())) continue;
            protectedCount++;
            PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);
            assertNotNull(authorization, method.getName());
            assertEquals("hasAuthority('" + expected.get(method.getName()) + "')", authorization.value());
            assertTrue(method.isAnnotationPresent(GetMapping.class) || method.isAnnotationPresent(PostMapping.class)
                    || method.isAnnotationPresent(PutMapping.class) || method.isAnnotationPresent(PatchMapping.class)
                    || method.isAnnotationPresent(DeleteMapping.class));
        }
        assertEquals(expected.size(), protectedCount);
    }
}
