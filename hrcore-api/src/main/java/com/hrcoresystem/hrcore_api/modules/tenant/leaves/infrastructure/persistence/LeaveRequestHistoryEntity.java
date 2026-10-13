package com.hrcoresystem.hrcore_api.modules.tenant.leaves.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.RequestStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "leave_request_history") @Getter @Setter
public class LeaveRequestHistoryEntity {
    @Id private UUID id;
    @Column(name = "leave_request_id", nullable = false, updatable = false) private UUID leaveRequestId;
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", length = 20) private RequestStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false, length = 20) private RequestStatus newStatus;
    @Column(columnDefinition = "TEXT") private String comment;
    @Column(name = "performed_by") private UUID performedBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void create() { if (createdAt == null) createdAt = Instant.now(); }
}
