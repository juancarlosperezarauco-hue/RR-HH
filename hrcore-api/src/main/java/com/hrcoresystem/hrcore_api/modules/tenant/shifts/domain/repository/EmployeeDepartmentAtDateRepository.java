package com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EmployeeDepartmentAtDateRepository {
    List<Placement> findAtDate(UUID employeeId, LocalDate date);

    record Placement(UUID departmentId, boolean primary) { }
}
