package com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmployeeTermination;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
public interface EmployeeTerminationRepository {
    EmployeeTermination save(EmployeeTermination termination);
    Optional<EmployeeTermination> findByEmployeeId(UUID employeeId);
    List<EmployeeTermination> findAllByEmployeeId(UUID employeeId);
    Optional<EmployeeTermination> findByEmploymentPeriodId(UUID employmentPeriodId);
    boolean existsByEmploymentPeriodId(UUID employmentPeriodId);
}
