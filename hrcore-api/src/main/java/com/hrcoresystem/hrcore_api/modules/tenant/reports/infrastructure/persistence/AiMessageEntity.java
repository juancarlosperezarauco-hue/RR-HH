package com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.persistence;

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

@Entity
@Table(name = "ai_messages")
@Getter
@Setter
public class AiMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Column(name = "client_message_id", updatable = false)
    private UUID clientMessageId;

    @Column(nullable = false, length = 16, updatable = false)
    private String role;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType;

    @Column(nullable = false, length = 16)
    private String state;

    @Column(name = "in_reply_to_message_id", updatable = false)
    private UUID inReplyToMessageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
