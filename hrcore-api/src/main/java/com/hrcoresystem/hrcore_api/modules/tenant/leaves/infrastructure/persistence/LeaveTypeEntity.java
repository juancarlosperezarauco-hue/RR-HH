package com.hrcoresystem.hrcore_api.modules.tenant.leaves.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.TypeStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "leave_types") @Getter @Setter
public class LeaveTypeEntity {
    @Id private UUID id;
    @Column(nullable = false, length = 50) private String code;
    @Column(nullable = false, length = 150) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false) private boolean paid;
    @Column(name = "requires_approval", nullable = false) private boolean requiresApproval;
    @Column(name = "requires_attachment", nullable = false) private boolean requiresAttachment;
    @Column(name = "deducts_balance", nullable = false) private boolean deductsBalance;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TypeStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void create() { if (createdAt == null) createdAt = Instant.now(); updatedAt = Instant.now(); }
    @PreUpdate void update() { updatedAt = Instant.now(); }
}
