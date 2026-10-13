package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.CalendarExceptionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "work_calendar_exceptions")
@Getter @Setter
public class WorkCalendarExceptionEntity {
    @Id @GeneratedValue
    private UUID id;
    @Column(name = "employee_id")
    private UUID employeeId;
    @Column(name = "department_id")
    private UUID departmentId;
    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 30)
    private CalendarExceptionType exceptionType;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "work_required", nullable = false)
    private boolean workRequired;
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
