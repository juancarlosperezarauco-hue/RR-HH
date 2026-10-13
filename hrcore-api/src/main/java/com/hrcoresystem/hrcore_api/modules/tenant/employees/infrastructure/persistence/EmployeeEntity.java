package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Getter @Setter
public class EmployeeEntity {
    @Id @GeneratedValue private UUID id;
    @Column(name = "tenant_user_id") private UUID tenantUserId;
    @Column(name = "employee_number", nullable = false, length = 50) private String employeeNumber;
    @Column(name = "first_name", nullable = false, length = 100) private String firstName;
    @Column(name = "middle_name", length = 100) private String middleName;
    @Column(name = "last_name", nullable = false, length = 100) private String lastName;
    @Column(name = "second_last_name", length = 100) private String secondLastName;
    @Column(name = "document_type", nullable = false, length = 30) private String documentType;
    @Column(name = "document_number", nullable = false, length = 50) private String documentNumber;
    @Column(name = "birth_date") private LocalDate birthDate;
    @Column(length = 30) private String gender;
    @Column(length = 80) private String nationality;
    @Column(name = "personal_email", length = 150) private String personalEmail;
    @Column(name = "work_email", length = 150) private String workEmail;
    @Column(length = 40) private String phone;
    @Column(name = "mobile_phone", length = 40) private String mobilePhone;
    @Column(columnDefinition = "TEXT") private String address;
    @Column(name = "hire_date", nullable = false) private LocalDate hireDate;
    @Column(name = "termination_date") private LocalDate terminationDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 30) private EmploymentStatus employmentStatus;
    @Column(name = "created_by", updatable = false) private UUID createdBy;
    @Column(name = "updated_by") private UUID updatedBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist
    void prePersist() { Instant now = Instant.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
