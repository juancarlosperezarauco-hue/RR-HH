package com.hrcoresystem.hrcore_api.modules.governance.audit.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto.AuditEventResponse;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto.TenantAuditFilterCriteria;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.mapper.AuditEventMapper;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.TenantAuditEvent;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.repository.TenantAuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListTenantAuditEventsUseCaseTest {

    @Mock
    private TenantAuditEventRepository tenantAuditEventRepository;

    @Mock
    private AuditEventMapper auditEventMapper;

    private ListTenantAuditEventsUseCase useCase;

    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ListTenantAuditEventsUseCase(tenantAuditEventRepository, auditEventMapper);
    }

    @Test
    @DisplayName("Criterio 2: Debe listar eventos de auditoría de forma paginada y ordenada cronológicamente")
    void shouldReturnPaginatedAuditEventsWhenNoCriteria() {
        TenantAuditEvent event = new TenantAuditEvent(
                eventId, "user@tenant.com", UUID.randomUUID(), "user@tenant.com", "tenant-slug",
                "USER_CREATED", "USER", "usr-1", "{\"operation\":\"CREATE\"}", null,
                "Mozilla/5.0", "req-1", "corr-1", "APPLICATION", "SUCCESS", null, null, Instant.now()
        );
        AuditEventResponse response = new AuditEventResponse(
                eventId, "user@tenant.com", UUID.randomUUID(), "user@tenant.com", "tenant-slug",
                "USER_CREATED", "USER", "usr-1", "{\"operation\":\"CREATE\"}", "127.0.0.1",
                "Mozilla/5.0", "req-1", "corr-1", "APPLICATION", "SUCCESS", null, null, Instant.now()
        );

        Pageable pageable = PageRequest.of(0, 20);
        Page<TenantAuditEvent> eventPage = new PageImpl<>(List.of(event), pageable, 1);

        when(tenantAuditEventRepository.findAll(any(TenantAuditFilterCriteria.class), eq(pageable))).thenReturn(eventPage);
        when(auditEventMapper.toResponse(event)).thenReturn(response);

        Page<AuditEventResponse> result = useCase.execute(TenantAuditFilterCriteria.empty(), pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(eventId, result.getContent().get(0).id());
        verify(tenantAuditEventRepository).findAll(any(TenantAuditFilterCriteria.class), eq(pageable));
    }

    @Test
    @DisplayName("Criterio 3: Debe filtrar actividades por rango de fechas, usuario ejecutor, módulo, acción y recurso")
    void shouldFilterAuditEventsByCriteria() {
        Instant now = Instant.now();
        Instant from = now.minusSeconds(86400);
        TenantAuditFilterCriteria criteria = new TenantAuditFilterCriteria(
                from, now, "admin@tenant.com", "USER", "USER_ROLES_ASSIGNED", "USER", "role"
        );
        Pageable pageable = PageRequest.of(0, 10);

        when(tenantAuditEventRepository.findAll(eq(criteria), eq(pageable))).thenReturn(Page.empty());

        Page<AuditEventResponse> result = useCase.execute(criteria, pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tenantAuditEventRepository).findAll(eq(criteria), eq(pageable));
    }

    @Test
    @DisplayName("Debe consultar lista completa de eventos para compatibilidad")
    void shouldReturnAllEventsForLegacyExecute() {
        when(tenantAuditEventRepository.findAll()).thenReturn(List.of());

        List<AuditEventResponse> result = useCase.execute();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tenantAuditEventRepository).findAll();
    }

    @Test
    @DisplayName("Debe limitar correctamente la consulta de eventos recientes")
    void shouldReturnRecentEventsWithSafeLimit() {
        when(tenantAuditEventRepository.findRecent(50)).thenReturn(List.of());

        List<AuditEventResponse> result = useCase.execute(50);

        assertNotNull(result);
        verify(tenantAuditEventRepository).findRecent(50);
    }
}
