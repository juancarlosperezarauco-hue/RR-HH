package com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AttendanceStore {
    AttendanceRecord save(AttendanceRecord value);
    AttendanceRecord lockOrCreate(UUID employeeId, LocalDate workDate);
    Optional<AttendanceRecord> record(UUID id);
    Optional<AttendanceRecord> record(UUID employeeId, LocalDate date);
    Page<AttendanceRecord> records(UUID employeeId, LocalDate from, LocalDate to, AttendanceStatus status, Pageable page);

    AttendancePunch save(AttendancePunch value);
    Optional<AttendancePunch> punch(UUID id);
    List<AttendancePunch> punches(UUID employeeId, Instant from, Instant to);
    List<AttendancePunch> punches(UUID recordId);
    Page<AttendancePunch> punches(UUID employeeId, UUID recordId, Instant from, Instant to,
            PunchType type, PunchSource source, Pageable page);
    void associatePunch(UUID punchId, UUID recordId);

    AttendanceAdjustment save(AttendanceAdjustment value);
    Optional<AttendanceAdjustment> adjustment(UUID id);
    AttendanceAdjustment lockAdjustment(UUID id);
    List<AttendanceAdjustment> adjustments(UUID recordId);
    Page<AttendanceAdjustment> adjustments(UUID recordId, UUID requestedBy, AdjustmentType type,
            ReviewStatus status, Pageable page);

    AttendanceOvertime save(AttendanceOvertime value);
    Optional<AttendanceOvertime> overtime(UUID id);
    AttendanceOvertime lockOvertime(UUID id);
    List<AttendanceOvertime> overtimeForRecord(UUID recordId);
    Page<AttendanceOvertime> overtime(UUID recordId, ReviewStatus status, LocalDate from,
            LocalDate to, Pageable page);

    Optional<UUID> employeeForUser(UUID tenantUserId);
    List<UUID> overtimeRequestActors(UUID overtimeId);
}
