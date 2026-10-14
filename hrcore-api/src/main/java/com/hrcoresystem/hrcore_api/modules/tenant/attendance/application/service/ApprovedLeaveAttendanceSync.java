package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service.LeaveAttendanceSync;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ApprovedLeaveAttendanceSync implements LeaveAttendanceSync {
    private static final long MAX_SYNCHRONIZED_DAYS = 366;
    private final AttendanceCalculationService attendance;

    public ApprovedLeaveAttendanceSync(AttendanceCalculationService attendance) {
        this.attendance = attendance;
    }

    @Override
    public void synchronize(UUID employeeId, LocalDate startDate, LocalDate endDate) {
        if (employeeId == null || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new BusinessException("Invalid leave period for attendance synchronization");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > MAX_SYNCHRONIZED_DAYS) {
            throw new BusinessException("Leave period is too long to synchronize with attendance");
        }
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            attendance.calculate(employeeId, date);
        }
    }
}
