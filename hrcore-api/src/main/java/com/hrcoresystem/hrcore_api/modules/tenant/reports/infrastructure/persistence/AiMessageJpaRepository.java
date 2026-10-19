package com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMessageJpaRepository extends JpaRepository<AiMessageEntity, UUID> {
    Optional<AiMessageEntity> findByIdAndConversationId(UUID id, UUID conversationId);

    Optional<AiMessageEntity> findByConversationIdAndClientMessageId(UUID conversationId,
                                                                      UUID clientMessageId);

    Optional<AiMessageEntity> findByConversationIdAndInReplyToMessageId(UUID conversationId,
                                                                        UUID requestMessageId);

    List<AiMessageEntity> findAllByConversationIdAndRoleAndStateAndCreatedAtBefore(
            UUID conversationId, String role, String state, Instant createdBefore);

    boolean existsByConversationIdAndRoleAndState(UUID conversationId, String role, String state);

    Page<AiMessageEntity> findAllByConversationId(UUID conversationId, Pageable pageable);
}
