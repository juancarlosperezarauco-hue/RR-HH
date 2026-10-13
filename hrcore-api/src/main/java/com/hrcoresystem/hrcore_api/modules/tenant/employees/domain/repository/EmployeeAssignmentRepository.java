package com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmployeeAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeAssignmentRepository {
    EmployeeAssignment save(EmployeeAssignment assignment);
    Optional<EmployeeAssignment> findByIdAndEmployeeId(UUID id, UUID employeeId);
    Page<EmployeeAssignment> findByEmployeeId(UUID employeeId, Pageable pageable);
    boolean hasOpenOrFutureAssignments(UUID employeeId);
    boolean hasOverlappingPrimary(UUID employeeId, UUID exceptId, LocalDate startDate, LocalDate endDate);
    Optional<LocalDate> earliestStartDate(UUID employeeId);
}
