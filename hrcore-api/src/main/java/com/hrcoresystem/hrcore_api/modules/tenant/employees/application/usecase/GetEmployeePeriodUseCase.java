package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmploymentPeriodResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmploymentPeriodMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.EmployeePolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.EmploymentPeriodService;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetEmployeePeriodUseCase {
    private final EmployeePolicy employees; private final EmploymentPeriodService periods; private final EmploymentPeriodMapper mapper;
    public GetEmployeePeriodUseCase(EmployeePolicy employees, EmploymentPeriodService periods, EmploymentPeriodMapper mapper) { this.employees = employees; this.periods = periods; this.mapper = mapper; }
    public EmploymentPeriodResponse execute(UUID employeeId, UUID periodId) { employees.assertOperational(); employees.require(employeeId); return mapper.toResponse(periods.require(employeeId, periodId)); }
}
