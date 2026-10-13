package com.hrcoresystem.hrcore_api.modules.tenant.leaves;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.infrastructure.api.LeaveController;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class LeaveMethodSecurityTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void approvalBalanceAdjustmentAndPrivateDownloadRequireSeparatePermissions() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class)) {
            LeaveController controller = context.getBean(LeaveController.class);
            UUID id = UUID.randomUUID();
            assertThrows(AuthenticationException.class, () -> controller.approve(id, null));
            authenticate("leaves.requests.list");
            assertThrows(AccessDeniedException.class, () -> controller.approve(id, null));
            assertThrows(AccessDeniedException.class, () -> controller.adjust(id, null));
            assertThrows(AccessDeniedException.class, () -> controller.download(id, UUID.randomUUID()));
            authenticate("leaves.requests.approve");
            assertDoesNotThrow(() -> controller.approve(id, null));
            assertThrows(AccessDeniedException.class, () -> controller.adjust(id, null));
        }
    }

    private void authenticate(String permission) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "hr-user", "n/a", List.of(new SimpleGrantedAuthority(permission))));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean LeaveController controller() {
            return new LeaveController(mock(LeaveService.class), mock(LeaveAttachmentService.class));
        }
    }
}
