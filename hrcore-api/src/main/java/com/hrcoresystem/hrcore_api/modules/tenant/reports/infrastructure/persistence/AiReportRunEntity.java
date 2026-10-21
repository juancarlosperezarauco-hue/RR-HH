package com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai_report_runs")
@Getter
@Setter
public class AiReportRunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Column(name = "request_message_id", nullable = false, updatable = false)
    private UUID requestMessageId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "query_plan", nullable = false, columnDefinition = "jsonb", updatable = false)
    private JsonNode queryPlan;

    @Column(name = "plan_version", nullable = false, length = 20, updatable = false)
    private String planVersion;

    @Column(name = "catalog_version", nullable = false, length = 40, updatable = false)
    private String catalogVersion;

    @Column(nullable = false, length = 16)
    private String state;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_metadata", columnDefinition = "jsonb")
    private JsonNode resultMetadata;

    @PrePersist
    void setRequestedAt() {
        if (requestedAt == null) requestedAt = Instant.now();
    }
}
