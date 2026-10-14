package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeAssignmentResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeOnboardingRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeOnboardingResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateEmployeeWithAssignmentUseCase {
    private final CreateEmployeeUseCase employees;
    private final CreateEmployeeAssignmentUseCase assignments;

    public CreateEmployeeWithAssignmentUseCase(CreateEmployeeUseCase employees,
                                                CreateEmployeeAssignmentUseCase assignments) {
        this.employees = employees;
        this.assignments = assignments;
    }

    @Transactional
    public EmployeeOnboardingResponse execute(EmployeeOnboardingRequest request) {
        EmployeeResponse employee = employees.execute(request.employee());
        EmployeeAssignmentResponse assignment = assignments.execute(employee.id(), request.assignment());
        return new EmployeeOnboardingResponse(employee, assignment);
    }
}
