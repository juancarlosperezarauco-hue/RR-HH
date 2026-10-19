package com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiReportRunJpaRepository extends JpaRepository<AiReportRunEntity, UUID> {
    interface LatestRun {
        UUID getRequestMessageId();
        UUID getReportRunId();
    }

    @Query(value = """
            SELECT DISTINCT ON (request_message_id)
                   request_message_id AS "requestMessageId", id AS "reportRunId"
            FROM ai_report_runs
            WHERE conversation_id = :conversationId AND state = 'SUCCEEDED'
              AND request_message_id IN (:requestIds)
            ORDER BY request_message_id, finished_at DESC, id DESC
            """, nativeQuery = true)
    List<LatestRun> findLatestRuns(@Param("conversationId") UUID conversationId,
                                   @Param("requestIds") Collection<UUID> requestIds);
    Optional<AiReportRunEntity> findFirstByConversationIdAndStateOrderByFinishedAtDescIdDesc(
            UUID conversationId, String state);

    Optional<AiReportRunEntity> findFirstByConversationIdAndRequestMessageIdAndStateOrderByFinishedAtDescIdDesc(
            UUID conversationId, UUID requestMessageId, String state);

    Optional<AiReportRunEntity> findFirstByConversationIdAndIdAndState(
            UUID conversationId, UUID id, String state);
}
