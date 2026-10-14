package com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service;

import java.time.LocalDate;
import java.util.UUID;

/** Sincroniza el estado de una ausencia aprobada o cancelada con HU17. */
public interface LeaveAttendanceSync {
    void synchronize(UUID employeeId, LocalDate startDate, LocalDate endDate);
}
