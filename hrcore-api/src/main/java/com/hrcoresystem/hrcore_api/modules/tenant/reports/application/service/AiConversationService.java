package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AnalyticAuthorizationPolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.AiConversationMessageResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.AiConversationResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiConversationEntity;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiConversationJpaRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiMessageEntity;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiMessageJpaRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence.AiReportRunJpaRepository;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;

@Service
public class AiConversationService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_TITLE = "Nuevo informe";

    private final AiConversationJpaRepository conversations;
    private final AiMessageJpaRepository messages;
    private final AiReportRunJpaRepository reportRuns;
    private final SecurityContextFacade security;
    private final AnalyticAuthorizationPolicy authorization;

    public AiConversationService(AiConversationJpaRepository conversations,
                                 AiMessageJpaRepository messages,
                                 AiReportRunJpaRepository reportRuns,
                                 SecurityContextFacade security,
                                 AnalyticAuthorizationPolicy authorization) {
        this.conversations = conversations;
        this.messages = messages;
        this.reportRuns = reportRuns;
        this.security = security;
        this.authorization = authorization;
    }

    @Transactional
    public AiConversationResponse create() {
        UUID ownerId = requireAuthorizedOwner();
        AiConversationEntity conversation = new AiConversationEntity();
        conversation.setOwnerUserId(ownerId);
        conversation.setTitle(DEFAULT_TITLE);
        conversation.setStatus("ACTIVE");
        return toResponse(conversations.save(conversation));
    }

    @Transactional(readOnly = true)
    public Page<AiConversationResponse> list(Pageable pageable) {
        UUID ownerId = requireAuthorizedOwner();
        return conversations.findAllByOwnerUserIdOrderByUpdatedAtDescIdDesc(ownerId, bounded(pageable))
                .map(AiConversationService::toResponse);
    }

    @Transactional(readOnly = true)
    public AiConversationResponse get(UUID conversationId) {
        UUID ownerId = requireAuthorizedOwner();
        return toResponse(requireOwned(conversationId, ownerId));
    }

    @Transactional(readOnly = true)
    public Page<AiConversationMessageResponse> messages(UUID conversationId, Pageable pageable) {
        UUID ownerId = requireAuthorizedOwner();
        requireOwned(conversationId, ownerId);
        Page<AiMessageEntity> page = messages.findAllByConversationId(conversationId, boundedMessages(pageable));
        List<UUID> requestIds = page.stream()
                .filter(message -> "ASSISTANT".equals(message.getRole()) && "REPORT".equals(message.getMessageType()))
                .map(AiMessageEntity::getInReplyToMessageId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<UUID, UUID> runs = requestIds.isEmpty() ? Map.of() : reportRuns.findLatestRuns(conversationId, requestIds)
                .stream().collect(Collectors.toMap(AiReportRunJpaRepository.LatestRun::getRequestMessageId,
                        AiReportRunJpaRepository.LatestRun::getReportRunId));
        return page.map(message -> toMessageResponse(message,
                "REPORT".equals(message.getMessageType()) && message.getInReplyToMessageId() != null
                        ? runs.get(message.getInReplyToMessageId()) : null));
    }

    @Transactional
    public void delete(UUID conversationId) {
        UUID ownerId = requireAuthenticatedTenantOwner();
        conversations.delete(requireOwned(conversationId, ownerId));
    }

    private UUID requireAuthorizedOwner() {
        if (authorization.authorizedCatalog().isEmpty())
            throw new AccessDeniedException("Se requiere acceso a reportes analíticos");
        return security.requireCurrentUserId();
    }

    private UUID requireAuthenticatedTenantOwner() {
        TenantContext context = TenantContextHolder.get();
        if (!security.isTenantAuthenticated() || context == null || context.publicRequest()
                || !context.tenantSlug().equals(security.getCurrentTenantSlug()))
            throw new AccessDeniedException("Se requiere una sesión tenant válida");
        return security.requireCurrentUserId();
    }

    private AiConversationEntity requireOwned(UUID conversationId, UUID ownerId) {
        return conversations.findByIdAndOwnerUserId(conversationId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));
    }

    private static AiConversationResponse toResponse(AiConversationEntity entity) {
        return new AiConversationResponse(entity.getId(), entity.getTitle(), entity.getStatus(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getLastMessageAt(), entity.getVersion());
    }

    private static AiConversationMessageResponse toMessageResponse(AiMessageEntity entity, UUID reportRunId) {
        return new AiConversationMessageResponse(entity.getId(), entity.getRole(), entity.getContent(),
                entity.getMessageType(), entity.getState(), entity.getCreatedAt(), entity.getCompletedAt(), reportRunId);
    }

    private static Pageable bounded(Pageable pageable) {
        int page = pageable == null ? 0 : Math.max(0, pageable.getPageNumber());
        int requestedSize = pageable == null || pageable.getPageSize() < 1 ? 20 : pageable.getPageSize();
        return PageRequest.of(page, Math.min(requestedSize, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.DESC, "id")));
    }

    private static Pageable boundedMessages(Pageable pageable) {
        int page = pageable == null ? 0 : Math.max(0, pageable.getPageNumber());
        int requestedSize = pageable == null || pageable.getPageSize() < 1 ? 50 : pageable.getPageSize();
        return PageRequest.of(page, Math.min(requestedSize, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id")));
    }
}
