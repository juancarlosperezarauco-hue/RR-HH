package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employee_emergency_contacts")
@Getter @Setter
public class EmployeeEmergencyContactEntity {
    @Id @GeneratedValue private UUID id;
    @Column(name = "employee_id", nullable = false, updatable = false) private UUID employeeId;
    @Column(name = "full_name", nullable = false, length = 200) private String fullName;
    @Column(length = 80) private String relationship;
    @Column(nullable = false, length = 40) private String phone;
    @Column(name = "alternative_phone", length = 40) private String alternativePhone;
    @Column(columnDefinition = "TEXT") private String address;
    @Column(name = "is_primary", nullable = false) private boolean primary;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist
    void prePersist() { Instant now = Instant.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
