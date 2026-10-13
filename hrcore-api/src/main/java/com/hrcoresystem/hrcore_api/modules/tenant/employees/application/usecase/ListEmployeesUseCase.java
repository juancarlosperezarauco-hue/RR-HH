package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeSummaryResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.EmployeePolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmployeeFilter;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ListEmployeesUseCase {
    private final EmployeeRepository employees; private final EmployeePolicy policy; private final EmployeeMapper mapper;
    public ListEmployeesUseCase(EmployeeRepository employees, EmployeePolicy policy, EmployeeMapper mapper) {
        this.employees = employees; this.policy = policy; this.mapper = mapper;
    }
    public Page<EmployeeSummaryResponse> execute(EmployeeFilter filter, Pageable pageable) {
        policy.assertOperational();
        return employees.findAll(filter, pageable).map(mapper::toSummary);
    }
}
