package com.hrcoresystem.hrcore_api.modules.governance.audit.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto.AuditEventResponse;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.mapper.AuditEventMapper;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.TenantAuditEvent;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.repository.TenantAuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetTenantAuditEventByIdUseCaseTest {

    @Mock
    private TenantAuditEventRepository tenantAuditEventRepository;

    @Mock
    private AuditEventMapper auditEventMapper;

    private GetTenantAuditEventByIdUseCase useCase;

    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new GetTenantAuditEventByIdUseCase(tenantAuditEventRepository, auditEventMapper);
    }

    @Test
    @DisplayName("Criterio 4: Debe consultar el detalle completo de un evento específico (createdAt, ipAddress, userAgent, metadata)")
    void shouldReturnAuditEventDetailsSuccessfully() {
        Instant timestamp = Instant.now();
        TenantAuditEvent event = new TenantAuditEvent(
                eventId, "admin@tenant.com", UUID.randomUUID(), "admin@tenant.com", "tenant-slug",
                "USER_ROLES_ASSIGNED", "USER", "usr-123", "{\"operation\":\"ASSIGN_USER_ROLES\"}", null,
                "Mozilla/5.0 Chrome/120.0", "req-abc", "corr-xyz", "APPLICATION", "SUCCESS", null, null, timestamp
        );
        AuditEventResponse response = new AuditEventResponse(
                eventId, "admin@tenant.com", UUID.randomUUID(), "admin@tenant.com", "tenant-slug",
                "USER_ROLES_ASSIGNED", "USER", "usr-123", "{\"operation\":\"ASSIGN_USER_ROLES\"}", "192.168.1.10",
                "Mozilla/5.0 Chrome/120.0", "req-abc", "corr-xyz", "APPLICATION", "SUCCESS", null, null, timestamp
        );

        when(tenantAuditEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(auditEventMapper.toResponse(event)).thenReturn(response);

        AuditEventResponse result = useCase.execute(eventId);

        assertNotNull(result);
        assertEquals(eventId, result.id());
        assertEquals("USER_ROLES_ASSIGNED", result.eventType());
        assertEquals("192.168.1.10", result.ipAddress());
        assertEquals("Mozilla/5.0 Chrome/120.0", result.userAgent());
        assertEquals("{\"operation\":\"ASSIGN_USER_ROLES\"}", result.eventDetails());
        assertEquals(timestamp, result.createdAt());
        verify(tenantAuditEventRepository).findById(eventId);
    }

    @Test
    @DisplayName("Criterio 6: Debe lanzar excepción si el evento no existe o pertenece a otro tenant")
    void shouldThrowResourceNotFoundExceptionWhenEventNotFoundOrBelongsToAnotherTenant() {
        when(tenantAuditEventRepository.findById(eventId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> useCase.execute(eventId)
        );

        assertTrue(exception.getMessage().contains("Tenant audit event not found with id: " + eventId));
        verify(tenantAuditEventRepository).findById(eventId);
        verify(auditEventMapper, never()).toResponse(any(TenantAuditEvent.class));
    }
}
