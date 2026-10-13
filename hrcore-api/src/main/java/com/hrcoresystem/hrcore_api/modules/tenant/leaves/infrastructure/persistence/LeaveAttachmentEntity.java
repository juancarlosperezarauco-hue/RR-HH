package com.hrcoresystem.hrcore_api.modules.tenant.leaves.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "leave_attachments") @Getter @Setter
public class LeaveAttachmentEntity {
    @Id private UUID id;
    @Column(name = "leave_request_id", nullable = false, updatable = false) private UUID leaveRequestId;
    @Column(nullable = false, length = 255) private String filename;
    @Column(name = "storage_key", nullable = false, length = 500) private String storageKey;
    @Column(name = "mime_type", nullable = false, length = 150) private String mimeType;
    @Column(name = "file_size", nullable = false) private long fileSize;
    @Column(name = "uploaded_by") private UUID uploadedBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void create() { if (createdAt == null) createdAt = Instant.now(); }
}
