package com.hrcoresystem.hrcore_api.modules.tenant.contracts.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "employment_contract_history")
@Getter @Setter
public class ContractHistoryEntity {
    @Id @GeneratedValue private UUID id;
    @Column(name = "contract_id", nullable = false) private UUID contractId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private ContractHistoryAction action;
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", length = 30) private ContractStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name = "new_status", length = 30) private ContractStatus newStatus;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private Map<String, Object> details;
    @Column(columnDefinition = "TEXT") private String reason;
    @Column(name = "performed_by") private UUID performedBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
}
