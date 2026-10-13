package com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.repository.AttendanceStore;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AttendanceStoreAdapter implements AttendanceStore {
    private final AttendanceRecordJpaRepository records;
    private final AttendancePunchJpaRepository punches;
    private final AttendanceAdjustmentJpaRepository adjustments;
    private final AttendanceOvertimeJpaRepository overtime;
    private final JdbcTemplate jdbc;

    public AttendanceStoreAdapter(AttendanceRecordJpaRepository records, AttendancePunchJpaRepository punches,
            AttendanceAdjustmentJpaRepository adjustments, AttendanceOvertimeJpaRepository overtime,
            DataSource dataSource) {
        this.records = records;
        this.punches = punches;
        this.adjustments = adjustments;
        this.overtime = overtime;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override public AttendanceRecord save(AttendanceRecord value) {
        AttendanceRecordEntity e = value.id() == null ? new AttendanceRecordEntity() : records.findById(value.id()).orElseThrow();
        e.setEmployeeId(value.employeeId()); e.setShiftAssignmentId(value.shiftAssignmentId()); e.setWorkDate(value.workDate());
        e.setExpectedStartAt(value.expectedStartAt()); e.setExpectedEndAt(value.expectedEndAt());
        e.setActualFirstInAt(value.actualFirstInAt()); e.setActualLastOutAt(value.actualLastOutAt());
        e.setWorkedMinutes(value.workedMinutes()); e.setLateMinutes(value.lateMinutes());
        e.setEarlyDepartureMinutes(value.earlyDepartureMinutes()); e.setStatus(value.status());
        e.setCalculationDetails(value.calculationDetails()); e.setCalculatedAt(value.calculatedAt());
        return model(records.saveAndFlush(e));
    }

    @Override public AttendanceRecord lockOrCreate(UUID employeeId, LocalDate workDate) {
        return records.lockByEmployeeAndDate(employeeId, workDate).map(this::model).orElseGet(() -> {
            AttendanceRecordEntity e = new AttendanceRecordEntity();
            e.setEmployeeId(employeeId); e.setWorkDate(workDate); e.setStatus(AttendanceStatus.PENDING);
            return model(records.saveAndFlush(e));
        });
    }

    @Override public Optional<AttendanceRecord> record(UUID id) { return records.findById(id).map(this::model); }
    @Override public Optional<AttendanceRecord> record(UUID employeeId, LocalDate date) {
        return records.findByEmployeeIdAndWorkDate(employeeId, date).map(this::model);
    }

    @Override public Page<AttendanceRecord> records(UUID employeeId, LocalDate from, LocalDate to,
            AttendanceStatus status, Pageable page) {
        Specification<AttendanceRecordEntity> s = (r, q, cb) -> cb.conjunction();
        if (employeeId != null) s = s.and((r, q, cb) -> cb.equal(r.get("employeeId"), employeeId));
        if (from != null) s = s.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("workDate"), from));
        if (to != null) s = s.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("workDate"), to));
        if (status != null) s = s.and((r, q, cb) -> cb.equal(r.get("status"), status));
        return records.findAll(s, page).map(this::model);
    }

    @Override public AttendancePunch save(AttendancePunch value) {
        if (value.attendanceRecordId() != null) {
            AttendanceRecordEntity record = records.findById(value.attendanceRecordId())
                    .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));
            if (!record.getEmployeeId().equals(value.employeeId()))
                throw new BusinessException("Punch and attendance record must belong to the same employee");
        }
        AttendancePunchEntity e = new AttendancePunchEntity();
        e.setEmployeeId(value.employeeId()); e.setAttendanceRecordId(value.attendanceRecordId());
        e.setPunchType(value.punchType()); e.setPunchedAt(value.punchedAt()); e.setSource(value.source());
        e.setDeviceIdentifier(value.deviceIdentifier()); e.setLatitude(value.latitude()); e.setLongitude(value.longitude());
        e.setRecordedBy(value.recordedBy()); e.setNotes(value.notes());
        return model(punches.saveAndFlush(e));
    }

    @Override public Optional<AttendancePunch> punch(UUID id) { return punches.findById(id).map(this::model); }
    @Override public List<AttendancePunch> punches(UUID employeeId, Instant from, Instant to) {
        return punches.findInWindow(employeeId, from, to).stream().map(this::model).toList();
    }
    @Override public List<AttendancePunch> punches(UUID recordId) {
        return punches.findByAttendanceRecordIdOrderByPunchedAtAscIdAsc(recordId).stream().map(this::model).toList();
    }
    @Override public Page<AttendancePunch> punches(UUID employeeId, UUID recordId, Instant from, Instant to,
            PunchType type, PunchSource source, Pageable page) {
        Specification<AttendancePunchEntity> s = (r, q, cb) -> cb.conjunction();
        if (employeeId != null) s = s.and((r, q, cb) -> cb.equal(r.get("employeeId"), employeeId));
        if (recordId != null) s = s.and((r, q, cb) -> cb.equal(r.get("attendanceRecordId"), recordId));
        if (from != null) s = s.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("punchedAt"), from));
        if (to != null) s = s.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("punchedAt"), to));
        if (type != null) s = s.and((r, q, cb) -> cb.equal(r.get("punchType"), type));
        if (source != null) s = s.and((r, q, cb) -> cb.equal(r.get("source"), source));
        return punches.findAll(s, page).map(this::model);
    }
    @Override public void associatePunch(UUID punchId, UUID recordId) {
        AttendancePunchEntity e = punches.findById(punchId)
                .orElseThrow(() -> new ResourceNotFoundException("Punch not found"));
        AttendanceRecordEntity record = records.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));
        if (e.getAttendanceRecordId() != null || !e.getEmployeeId().equals(record.getEmployeeId()))
            throw new BusinessException("Punch cannot be associated with this attendance record");
        e.setAttendanceRecordId(recordId);
        punches.saveAndFlush(e);
    }

    @Override public AttendanceAdjustment save(AttendanceAdjustment value) {
        AttendanceAdjustmentEntity e = value.id() == null ? new AttendanceAdjustmentEntity()
                : adjustments.findById(value.id()).orElseThrow();
        e.setAttendanceRecordId(value.attendanceRecordId()); e.setAdjustmentType(value.adjustmentType());
        e.setReason(value.reason()); e.setPreviousValues(value.previousValues());
        e.setRequestedValues(value.requestedValues()); e.setStatus(value.status());
        e.setRequestedBy(value.requestedBy()); e.setReviewedBy(value.reviewedBy());
        e.setReviewedAt(value.reviewedAt()); e.setReviewComment(value.reviewComment()); e.setAppliedAt(value.appliedAt());
        return model(adjustments.saveAndFlush(e));
    }
    @Override public Optional<AttendanceAdjustment> adjustment(UUID id) { return adjustments.findById(id).map(this::model); }
    @Override public AttendanceAdjustment lockAdjustment(UUID id) {
        return adjustments.lockById(id).map(this::model)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance adjustment not found: " + id));
    }
    @Override public List<AttendanceAdjustment> adjustments(UUID recordId) {
        return adjustments.findByAttendanceRecordIdOrderByCreatedAtAscIdAsc(recordId).stream().map(this::model).toList();
    }
    @Override public Page<AttendanceAdjustment> adjustments(UUID recordId, UUID requestedBy,
            AdjustmentType type, ReviewStatus status, Pageable page) {
        Specification<AttendanceAdjustmentEntity> s = (r, q, cb) -> cb.conjunction();
        if (recordId != null) s = s.and((r, q, cb) -> cb.equal(r.get("attendanceRecordId"), recordId));
        if (requestedBy != null) s = s.and((r, q, cb) -> cb.equal(r.get("requestedBy"), requestedBy));
        if (type != null) s = s.and((r, q, cb) -> cb.equal(r.get("adjustmentType"), type));
        if (status != null) s = s.and((r, q, cb) -> cb.equal(r.get("status"), status));
        return adjustments.findAll(s, page).map(this::model);
    }

    @Override public AttendanceOvertime save(AttendanceOvertime value) {
        AttendanceOvertimeEntity e = value.id() == null ? new AttendanceOvertimeEntity()
                : overtime.findById(value.id()).orElseThrow();
        e.setAttendanceRecordId(value.attendanceRecordId()); e.setRequestedMinutes(value.requestedMinutes());
        e.setApprovedMinutes(value.approvedMinutes()); e.setReason(value.reason()); e.setStatus(value.status());
        e.setApprovedBy(value.approvedBy()); e.setApprovedAt(value.approvedAt());
        return model(overtime.saveAndFlush(e));
    }
    @Override public Optional<AttendanceOvertime> overtime(UUID id) { return overtime.findById(id).map(this::model); }
    @Override public AttendanceOvertime lockOvertime(UUID id) {
        return overtime.lockById(id).map(this::model)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance overtime not found: " + id));
    }
    @Override public List<AttendanceOvertime> overtimeForRecord(UUID recordId) {
        return overtime.findByAttendanceRecordIdOrderByCreatedAtAscIdAsc(recordId).stream().map(this::model).toList();
    }
    @Override public Page<AttendanceOvertime> overtime(UUID recordId, ReviewStatus status,
            LocalDate from, LocalDate to, Pageable page) {
        Specification<AttendanceOvertimeEntity> s = (r, q, cb) -> cb.conjunction();
        if (recordId != null) s = s.and((r, q, cb) -> cb.equal(r.get("attendanceRecordId"), recordId));
        if (status != null) s = s.and((r, q, cb) -> cb.equal(r.get("status"), status));
        if (from != null || to != null) s = s.and((r, q, cb) -> {
            var sub = q.subquery(UUID.class);
            var record = sub.from(AttendanceRecordEntity.class);
            var p = cb.conjunction();
            if (from != null) p = cb.and(p, cb.greaterThanOrEqualTo(record.get("workDate"), from));
            if (to != null) p = cb.and(p, cb.lessThanOrEqualTo(record.get("workDate"), to));
            sub.select(record.get("id")).where(p);
            return r.get("attendanceRecordId").in(sub);
        });
        return overtime.findAll(s, page).map(this::model);
    }

    @Override public Optional<UUID> employeeForUser(UUID tenantUserId) {
        List<UUID> ids = jdbc.query("select id from employees where tenant_user_id = ?",
                (rs, row) -> rs.getObject(1, UUID.class), tenantUserId);
        return ids.stream().findFirst();
    }
    @Override public List<UUID> overtimeRequestActors(UUID overtimeId) {
        return jdbc.query("select actor_id from tenant_audit_events where event_type = ? " +
                "and resource_type = ? and resource_id = ? and outcome = 'SUCCESS'",
                (rs, row) -> rs.getObject(1, UUID.class), "ATTENDANCE_OVERTIME_REQUESTED",
                "ATTENDANCE_OVERTIME", overtimeId.toString());
    }

    private AttendanceRecord model(AttendanceRecordEntity e) {
        return new AttendanceRecord(e.getId(), e.getEmployeeId(), e.getShiftAssignmentId(), e.getWorkDate(),
                e.getExpectedStartAt(), e.getExpectedEndAt(), e.getActualFirstInAt(), e.getActualLastOutAt(),
                e.getWorkedMinutes(), e.getLateMinutes(), e.getEarlyDepartureMinutes(), e.getStatus(),
                e.getCalculationDetails(), e.getCalculatedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
    private AttendancePunch model(AttendancePunchEntity e) {
        return new AttendancePunch(e.getId(), e.getEmployeeId(), e.getAttendanceRecordId(), e.getPunchType(),
                e.getPunchedAt(), e.getSource(), e.getDeviceIdentifier(), e.getLatitude(), e.getLongitude(),
                e.getRecordedBy(), e.getNotes(), e.getCreatedAt());
    }
    private AttendanceAdjustment model(AttendanceAdjustmentEntity e) {
        return new AttendanceAdjustment(e.getId(), e.getAttendanceRecordId(), e.getAdjustmentType(), e.getReason(),
                e.getPreviousValues(), e.getRequestedValues(), e.getStatus(), e.getRequestedBy(), e.getReviewedBy(),
                e.getReviewedAt(), e.getReviewComment(), e.getAppliedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
    private AttendanceOvertime model(AttendanceOvertimeEntity e) {
        return new AttendanceOvertime(e.getId(), e.getAttendanceRecordId(), e.getRequestedMinutes(),
                e.getApprovedMinutes(), e.getReason(), e.getStatus(), e.getApprovedBy(), e.getApprovedAt(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
