package com.hrcoresystem.hrcore_api.modules.tenant.contracts.infrastructure;

import com.hrcoresystem.hrcore_api.modules.tenant.contracts.infrastructure.api.ContractController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Method;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ContractControllerPermissionsTest {
    @Test void everyEndpointRequiresItsSpecificAuthority() {
        Map<String, String> expected = Map.of(
                "create", "contracts.create", "list", "contracts.list", "get", "contracts.detail",
                "update", "contracts.update", "changeStatus", "contracts.status.update",
                "renew", "contracts.renew", "history", "contracts.history.read",
                "document", "contracts.document.download");
        assertEquals("bearerAuth", ContractController.class.getAnnotation(SecurityRequirement.class).name());
        int count = 0;
        for (Method method : ContractController.class.getDeclaredMethods()) {
            if (!expected.containsKey(method.getName())) continue;
            count++;
            assertNotNull(method.getAnnotation(PreAuthorize.class));
            assertEquals("hasAuthority('" + expected.get(method.getName()) + "')",
                    method.getAnnotation(PreAuthorize.class).value());
            assertTrue(method.isAnnotationPresent(GetMapping.class) || method.isAnnotationPresent(PostMapping.class)
                    || method.isAnnotationPresent(PutMapping.class) || method.isAnnotationPresent(PatchMapping.class));
        }
        assertEquals(expected.size(), count);
    }
}
