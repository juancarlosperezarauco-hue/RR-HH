package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeAssignmentResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeOnboardingRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeOnboardingResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.EmployeeFileRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto.FileResponse;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class CreateEmployeeWithAssignmentUseCase {
    private final CreateEmployeeUseCase employees;
    private final CreateEmployeeAssignmentUseCase assignments;
    private final EmployeeFileRepository files;

    @Autowired
    public CreateEmployeeWithAssignmentUseCase(CreateEmployeeUseCase employees,
                                                CreateEmployeeAssignmentUseCase assignments, EmployeeFileRepository files) {
        this.employees = employees;
        this.assignments = assignments;
        this.files = files;
    }

    @Transactional
    public EmployeeOnboardingResponse execute(EmployeeOnboardingRequest request) {
        EmployeeResponse employee = employees.execute(request.employee());
        EmployeeAssignmentResponse assignment = assignments.execute(employee.id(), request.assignment());
        var file = files.findByEmployeeId(employee.id()).orElseThrow();
        return new EmployeeOnboardingResponse(employee, assignment,
                new FileResponse(file.id(), file.employeeId(), file.fileNumber(), file.status(), file.notes(), file.createdAt(), file.updatedAt()));
    }
}
