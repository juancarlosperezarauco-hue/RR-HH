package com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentPeriod;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmploymentPeriodRepository {
    Optional<EmploymentPeriod> findById(UUID id);
    Optional<EmploymentPeriod> findByIdAndEmployeeId(UUID id, UUID employeeId);
    Optional<EmploymentPeriod> findLatestByEmployeeId(UUID employeeId);
    Optional<EmploymentPeriod> findOpenByEmployeeId(UUID employeeId);
    Optional<EmploymentPeriod> findByEmployeeIdAndDate(UUID employeeId, LocalDate date);
    List<EmploymentPeriod> findAllByEmployeeId(UUID employeeId);
    EmploymentPeriod createRehirePeriod(UUID employeeId, LocalDate startDate);
}
