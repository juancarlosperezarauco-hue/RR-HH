package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceResponses;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.*;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {
    public AttendanceResponses.Record toResponse(AttendanceRecord v) {
        return new AttendanceResponses.Record(v.id(), v.employeeId(), v.shiftAssignmentId(), v.workDate(),
                v.expectedStartAt(), v.expectedEndAt(), v.actualFirstInAt(), v.actualLastOutAt(),
                v.workedMinutes(), v.lateMinutes(), v.earlyDepartureMinutes(), v.status(),
                v.calculationDetails(), v.calculatedAt(), v.createdAt(), v.updatedAt());
    }
    public AttendanceResponses.Punch toResponse(AttendancePunch v) {
        return new AttendanceResponses.Punch(v.id(), v.employeeId(), v.attendanceRecordId(), v.punchType(),
                v.punchedAt(), v.source(), v.latitude(), v.longitude(), v.recordedBy(), v.notes(), v.createdAt());
    }
    public AttendanceResponses.Adjustment toResponse(AttendanceAdjustment v) {
        return new AttendanceResponses.Adjustment(v.id(), v.attendanceRecordId(), v.adjustmentType(), v.reason(),
                v.previousValues(), v.requestedValues(), v.status(), v.requestedBy(), v.reviewedBy(),
                v.reviewedAt(), v.reviewComment(), v.appliedAt(), v.createdAt(), v.updatedAt());
    }
    public AttendanceResponses.Overtime toResponse(AttendanceOvertime v) {
        return new AttendanceResponses.Overtime(v.id(), v.attendanceRecordId(), v.requestedMinutes(),
                v.approvedMinutes(), v.reason(), v.status(), v.approvedBy(), v.approvedAt(),
                v.createdAt(), v.updatedAt());
    }
}
