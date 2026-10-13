package com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkCalendarException;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkCalendarExceptionFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkCalendarExceptionRepository {
    WorkCalendarException save(WorkCalendarException exception);
    Optional<WorkCalendarException> findById(UUID id);
    Optional<WorkCalendarException> findByIdFresh(UUID id);
    Page<WorkCalendarException> findAll(WorkCalendarExceptionFilter filter, Pageable pageable);
    List<WorkCalendarException> findByScopeAndDate(UUID employeeId, UUID departmentId, LocalDate date);
    void delete(UUID id);
    void lockScopeDate(UUID employeeId, UUID departmentId, LocalDate date);
}
