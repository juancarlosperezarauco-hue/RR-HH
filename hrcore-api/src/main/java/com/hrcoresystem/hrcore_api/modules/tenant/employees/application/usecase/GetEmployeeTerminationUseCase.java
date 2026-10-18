package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeTerminationHistoryResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeTerminationRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;
@Service
public class GetEmployeeTerminationUseCase {
    private final EmployeeTerminationRepository repository;
    public GetEmployeeTerminationUseCase(EmployeeTerminationRepository repository) { this.repository = repository; }
    public EmployeeTerminationHistoryResponse execute(UUID employeeId) {
        return repository.findByEmployeeId(employeeId).map(t -> new EmployeeTerminationHistoryResponse(
                t.employeeId(), t.employmentPeriodId(), t.effectiveDate(), t.reason(), t.performedBy(), t.createdAt()))
                .orElseThrow(() -> new ResourceNotFoundException("Employee termination not found: " + employeeId));
    }
}
