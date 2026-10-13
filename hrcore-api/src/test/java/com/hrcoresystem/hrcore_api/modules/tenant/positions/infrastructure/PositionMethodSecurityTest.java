package com.hrcoresystem.hrcore_api.modules.tenant.positions.infrastructure;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.usecase.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.infrastructure.api.PositionController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PositionMethodSecurityTest {
    @AfterEach
    void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void requiresAuthenticationAndExactAuthority() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            PositionController controller = context.getBean(PositionController.class);
            assertThrows(AuthenticationException.class,
                    () -> controller.list(null, null, null, null, PageRequest.of(0, 10)));

            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("positions.detail"))));
            assertThrows(AccessDeniedException.class,
                    () -> controller.list(null, null, null, null, PageRequest.of(0, 10)));

            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("positions.list"))));
            assertDoesNotThrow(() -> controller.list(null, null, null, null, PageRequest.of(0, 10)));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        PositionController positionController() {
            return new PositionController(mock(CreatePositionUseCase.class), mock(ListPositionsUseCase.class),
                    mock(GetPositionUseCase.class), mock(UpdatePositionUseCase.class),
                    mock(ActivatePositionUseCase.class), mock(DeactivatePositionUseCase.class),
                    mock(ListPositionRequirementsUseCase.class), mock(CreatePositionRequirementUseCase.class),
                    mock(UpdatePositionRequirementUseCase.class), mock(DeletePositionRequirementUseCase.class));
        }
    }
}
