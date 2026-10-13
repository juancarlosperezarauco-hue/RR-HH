package com.hrcoresystem.hrcore_api.modules.tenant.departments.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.DepartmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "departments")
@Getter @Setter
public class DepartmentEntity {
    @Id @GeneratedValue
    private UUID id;
    @Column(nullable = false, length = 50)
    private String code;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "parent_department_id")
    private UUID parentDepartmentId;
    @Column(name = "manager_employee_id")
    private UUID managerEmployeeId;
    @Column(name = "cost_center_code", length = 50)
    private String costCenterCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepartmentStatus status;
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;
    @Column(name = "updated_by")
    private UUID updatedBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
