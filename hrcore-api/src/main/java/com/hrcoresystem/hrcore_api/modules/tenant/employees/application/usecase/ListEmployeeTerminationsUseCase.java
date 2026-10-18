package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeTerminationHistoryResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeTerminationRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class ListEmployeeTerminationsUseCase {
    private final EmployeeTerminationRepository repository;
    public ListEmployeeTerminationsUseCase(EmployeeTerminationRepository repository) { this.repository = repository; }
    public List<EmployeeTerminationHistoryResponse> execute(UUID employeeId) {
        return repository.findAllByEmployeeId(employeeId).stream().map(t -> new EmployeeTerminationHistoryResponse(
                t.employeeId(), t.employmentPeriodId(), t.effectiveDate(), t.reason(), t.performedBy(), t.createdAt())).toList();
    }
    public EmployeeTerminationHistoryResponse executeForPeriod(UUID employeeId, UUID periodId) {
        var termination = repository.findByEmploymentPeriodId(periodId)
                .filter(t -> t.employeeId().equals(employeeId))
                .orElseThrow(() -> new com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException("Desvinculación no encontrada"));
        return new EmployeeTerminationHistoryResponse(termination.employeeId(), termination.employmentPeriodId(), termination.effectiveDate(), termination.reason(), termination.performedBy(), termination.createdAt());
    }
}
