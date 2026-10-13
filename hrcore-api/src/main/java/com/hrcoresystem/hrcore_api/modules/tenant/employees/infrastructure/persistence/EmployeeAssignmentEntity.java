package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.AssignmentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employee_assignments")
@Getter @Setter
public class EmployeeAssignmentEntity {
    @Id @GeneratedValue private UUID id;
    @Column(name = "employee_id", nullable = false, updatable = false) private UUID employeeId;
    @Column(name = "department_id", nullable = false) private UUID departmentId;
    @Column(name = "position_id", nullable = false) private UUID positionId;
    @Column(name = "supervisor_employee_id") private UUID supervisorEmployeeId;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 30) private AssignmentType assignmentType;
    @Column(length = 255) private String reason;
    @Column(name = "created_by", updatable = false) private UUID createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist
    void prePersist() { Instant now = Instant.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
