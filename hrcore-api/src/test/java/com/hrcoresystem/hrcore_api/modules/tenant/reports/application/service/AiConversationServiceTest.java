package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.security.principal.AuthenticatedUserPrincipal;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AnalyticAuthorizationPolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.SemanticCatalogV1;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiConversationEntity;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiConversationJpaRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiMessageJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiConversationServiceTest {
    private static final String TENANT = "tenant-a";
    private final SecurityContextFacade security = new SecurityContextFacade();
    private final AnalyticAuthorizationPolicy authorization =
            new AnalyticAuthorizationPolicy(security, new SemanticCatalogV1());
    @Mock private AiConversationJpaRepository repository;
    @Mock private AiMessageJpaRepository messages;
    @Mock private com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiReportRunJpaRepository reportRuns;
    private AiConversationService service;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        service = new AiConversationService(repository, messages, reportRuns, security, authorization);
        authenticate("hr.reports.employees.read");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void createsConversationWithCurrentOwnerAndNeutralTitle() {
        when(repository.save(any(AiConversationEntity.class))).thenAnswer(invocation -> {
            AiConversationEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        var result = service.create();

        assertEquals(ownerId, captureSaved().getOwnerUserId());
        assertEquals("Nuevo informe", result.title());
        assertEquals("ACTIVE", result.status());
    }

    @Test
    void listsOnlyOwnerConversationsAndBoundsPageSize() {
        when(repository.findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(eq(ownerId), any(Pageable.class)))
                .thenAnswer(invocation -> Page.empty(invocation.getArgument(1)));

        service.list(Pageable.ofSize(500));

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(eq(ownerId), page.capture());
        assertEquals(100, page.getValue().getPageSize());
        assertEquals("updatedAt: DESC,id: DESC", page.getValue().getSort().toString());
    }

    @Test
    void deniesUsersWithoutEffectiveReportReadPermissionBeforePersistence() {
        authenticate("employees.team.read", "attendance.team.read");

        assertThrows(AccessDeniedException.class, service::create);
        verifyNoInteractions(repository);
    }

    @Test
    void hidesForeignConversationsAndReturnsPersistedHistoryForOwnedConversation() {
        UUID conversationId = UUID.randomUUID();
        when(repository.findByIdAndOwnerUserId(conversationId, ownerId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.get(conversationId));
        assertThrows(ResourceNotFoundException.class, () -> service.messages(conversationId, Pageable.ofSize(10)));

        AiConversationEntity entity = new AiConversationEntity();
        entity.setId(conversationId);
        entity.setOwnerUserId(ownerId);
        when(repository.findByIdAndOwnerUserId(conversationId, ownerId)).thenReturn(Optional.of(entity));
        when(messages.findAllByConversationId(eq(conversationId), any(Pageable.class)))
                .thenAnswer(invocation -> Page.empty(invocation.getArgument(1)));
        assertTrue(service.messages(conversationId, Pageable.ofSize(10)).isEmpty());
    }

    @Test
    void ownerCanDeleteConversationAfterLosingAnalyticReportPermission() {
        authenticate("employees.team.read");
        UUID conversationId = UUID.randomUUID();
        AiConversationEntity entity = new AiConversationEntity();
        entity.setId(conversationId);
        entity.setOwnerUserId(ownerId);
        when(repository.findByIdAndOwnerUserId(conversationId, ownerId)).thenReturn(Optional.of(entity));

        service.delete(conversationId);

        verify(repository).delete(entity);
    }

    @Test
    void historyResolvesReportIdsInOneConversationScopedProjectionWithoutLoadingPlans() {
        UUID conversationId = UUID.randomUUID(), requestId = UUID.randomUUID(), runId = UUID.randomUUID();
        var conversation = new AiConversationEntity();
        when(repository.findByIdAndOwnerUserId(conversationId, ownerId)).thenReturn(Optional.of(conversation));
        var user = new com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiMessageEntity();
        user.setId(requestId); user.setRole("USER"); user.setMessageType("TEXT");
        var report = new com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiMessageEntity();
        report.setRole("ASSISTANT"); report.setMessageType("REPORT"); report.setInReplyToMessageId(requestId);
        when(messages.findAllByConversationId(eq(conversationId), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(user, report)));
        var projection = mock(com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiReportRunJpaRepository.LatestRun.class);
        when(projection.getRequestMessageId()).thenReturn(requestId);
        when(projection.getReportRunId()).thenReturn(runId);
        when(reportRuns.findLatestRuns(conversationId, List.of(requestId))).thenReturn(List.of(projection));
        var history = service.messages(conversationId, Pageable.ofSize(50));
        assertNull(history.getContent().getFirst().reportRunId());
        assertEquals(runId, history.getContent().get(1).reportRunId());
        verify(reportRuns).findLatestRuns(conversationId, List.of(requestId));
        verifyNoMoreInteractions(reportRuns);
    }

    private AiConversationEntity captureSaved() {
        ArgumentCaptor<AiConversationEntity> entity = ArgumentCaptor.forClass(AiConversationEntity.class);
        verify(repository).save(entity.capture());
        return entity.getValue();
    }

    private void authenticate(String... permissions) {
        TenantContextHolder.set(new TenantContext(TENANT, "tenant_a", false));
        var principal = new AuthenticatedUserPrincipal(ownerId, UUID.randomUUID(),
                "fictional@example.invalid", "Fictional", TENANT, "TENANT",
                List.of(), List.of(permissions), 1L, 1L, 1L);
        var authorities = java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }
}
