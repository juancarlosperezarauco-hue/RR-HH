package com.hrcoresystem.hrcore_api.modules.tenant.departments.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.DepartmentHistoryAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "department_history")
@Getter @Setter
public class DepartmentHistoryEntity {
    @Id @GeneratedValue
    private UUID id;
    @Column(name = "department_id", nullable = false)
    private UUID departmentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DepartmentHistoryAction action;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_data", columnDefinition = "JSONB")
    private JsonNode previousData;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_data", columnDefinition = "JSONB")
    private JsonNode newData;
    @Column(columnDefinition = "TEXT")
    private String reason;
    @Column(name = "performed_by")
    private UUID performedBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
}
