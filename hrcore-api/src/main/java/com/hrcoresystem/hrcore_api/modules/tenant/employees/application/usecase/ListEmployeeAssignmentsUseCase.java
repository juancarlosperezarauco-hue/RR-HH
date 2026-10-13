package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeAssignmentResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.EmployeePolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeAssignmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class ListEmployeeAssignmentsUseCase {
    private final EmployeeAssignmentRepository assignments; private final EmployeePolicy employees; private final EmployeeMapper mapper;
    public ListEmployeeAssignmentsUseCase(EmployeeAssignmentRepository assignments, EmployeePolicy employees, EmployeeMapper mapper) {
        this.assignments = assignments; this.employees = employees; this.mapper = mapper;
    }
    public Page<EmployeeAssignmentResponse> execute(UUID employeeId, Pageable pageable) {
        employees.assertOperational(); employees.require(employeeId);
        return assignments.findByEmployeeId(employeeId, pageable).map(mapper::toResponse);
    }
}
