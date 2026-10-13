package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface WorkCalendarExceptionJpaRepository extends JpaRepository<WorkCalendarExceptionEntity, UUID>,
        JpaSpecificationExecutor<WorkCalendarExceptionEntity> {
    List<WorkCalendarExceptionEntity> findByEmployeeIdAndExceptionDate(UUID employeeId, LocalDate date);
    List<WorkCalendarExceptionEntity> findByDepartmentIdAndExceptionDate(UUID departmentId, LocalDate date);
    List<WorkCalendarExceptionEntity> findByEmployeeIdIsNullAndDepartmentIdIsNullAndExceptionDate(LocalDate date);
}
