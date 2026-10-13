package com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.EmployeeShiftAssignment;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftAssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShiftAssignmentRepository {
    EmployeeShiftAssignment save(EmployeeShiftAssignment assignment);
    Optional<EmployeeShiftAssignment> findById(UUID id);
    Optional<UUID> findEmployeeId(UUID id);
    Page<EmployeeShiftAssignment> findAll(UUID employeeId, UUID shiftId,
            ShiftAssignmentStatus status, Pageable pageable);
    List<EmployeeShiftAssignment> findActiveOverlapping(UUID employeeId, UUID excludingId,
            LocalDate startDate, LocalDate endDate);
    List<EmployeeShiftAssignment> findEffectiveOn(UUID employeeId, LocalDate date);
    boolean existsAnyByShiftId(UUID shiftId);
    boolean existsActiveCurrentOrFutureByShiftId(UUID shiftId, LocalDate today);
}
