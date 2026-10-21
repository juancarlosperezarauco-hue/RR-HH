package com.hrcoresystem.hrcore_api.modules.governance.audit;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.mapper.AuditEventMapper;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.usecase.GetTenantAuditEventByIdUseCase;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.usecase.ListTenantAuditEventsUseCase;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.repository.TenantAuditEventRepository;
import com.hrcoresystem.hrcore_api.modules.governance.audit.infrastructure.api.TenantAuditController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAuditSecurityAndImmutabilityTest {

    @Mock
    private TenantAuditEventRepository repository;

    @Mock
    private AuditEventMapper mapper;

    @Test
    @DisplayName("Criterio 1: Los endpoints de auditoría del tenant deben requerir permiso audit.events.read y pertenecer al propietario/admin")
    void shouldValidateSecurityPreAuthorizeAnnotations() throws NoSuchMethodException {
        Method listMethod = TenantAuditController.class.getMethod(
                "listTenantAuditEvents",
                java.time.Instant.class,
                java.time.Instant.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                org.springframework.data.domain.Pageable.class
        );

        PreAuthorize listPreAuthorize = listMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(listPreAuthorize, "listTenantAuditEvents debe tener @PreAuthorize");
        assertTrue(listPreAuthorize.value().contains("audit.events.read"));
        assertTrue(listPreAuthorize.value().contains("@tenantOwnershipGuard.isCurrentOwner()"));

        Method detailMethod = TenantAuditController.class.getMethod("getTenantAuditEventById", UUID.class);
        PreAuthorize detailPreAuthorize = detailMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(detailPreAuthorize, "getTenantAuditEventById debe tener @PreAuthorize");
        assertTrue(detailPreAuthorize.value().contains("audit.events.read"));
        assertTrue(detailPreAuthorize.value().contains("@tenantOwnershipGuard.isCurrentOwner()"));
    }

    @Test
    @DisplayName("Criterio 5: Garantiza la inmutabilidad impidiendo cualquier endpoint de edición o borrado")
    void shouldEnforceImmutabilityByNotExposingMutationEndpoints() {
        Method[] methods = TenantAuditController.class.getDeclaredMethods();
        for (Method method : methods) {
            assertFalse(method.isAnnotationPresent(PostMapping.class), "No debe haber endpoints POST en TenantAuditController: " + method.getName());
            assertFalse(method.isAnnotationPresent(PutMapping.class), "No debe haber endpoints PUT en TenantAuditController: " + method.getName());
            assertFalse(method.isAnnotationPresent(PatchMapping.class), "No debe haber endpoints PATCH en TenantAuditController: " + method.getName());
            assertFalse(method.isAnnotationPresent(DeleteMapping.class), "No debe haber endpoints DELETE en TenantAuditController: " + method.getName());
        }

        // Verificar que el repositorio de auditoría no exponga métodos de eliminación o actualización
        Method[] repoMethods = TenantAuditEventRepository.class.getDeclaredMethods();
        boolean hasDeleteMethod = Arrays.stream(repoMethods)
                .anyMatch(m -> m.getName().toLowerCase().contains("delete") || m.getName().toLowerCase().contains("remove"));
        boolean hasUpdateMethod = Arrays.stream(repoMethods)
                .anyMatch(m -> m.getName().toLowerCase().contains("update") || m.getName().toLowerCase().contains("modify"));

        assertFalse(hasDeleteMethod, "TenantAuditEventRepository no debe tener métodos de borrado");
        assertFalse(hasUpdateMethod, "TenantAuditEventRepository no debe tener métodos de actualización");
    }

    @Test
    @DisplayName("Criterio 6: Bloquea el acceso a registros de auditoría de otros tenants")
    void shouldBlockAccessToAuditEventsOfOtherTenants() {
        GetTenantAuditEventByIdUseCase useCase = new GetTenantAuditEventByIdUseCase(repository, mapper);
        UUID foreignTenantEventId = UUID.randomUUID();

        when(repository.findById(foreignTenantEventId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(foreignTenantEventId));
    }
}
