package com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiReportRunJpaRepository extends JpaRepository<AiReportRunEntity, UUID> {
    Optional<AiReportRunEntity> findFirstByConversationIdAndRequestMessageIdAndStateOrderByFinishedAtDescIdDesc(
            UUID conversationId, UUID requestMessageId, String state);

    Optional<AiReportRunEntity> findFirstByConversationIdAndIdAndState(
            UUID conversationId, UUID id, String state);
}
