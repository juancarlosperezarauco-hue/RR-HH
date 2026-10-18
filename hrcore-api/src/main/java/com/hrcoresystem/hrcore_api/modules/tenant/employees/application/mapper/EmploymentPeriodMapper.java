package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmploymentPeriodResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentPeriod;
import org.springframework.stereotype.Component;

@Component
public class EmploymentPeriodMapper {
    public EmploymentPeriodResponse toResponse(EmploymentPeriod period) {
        return new EmploymentPeriodResponse(period.id(), period.employeeId(), period.periodNumber(),
                period.startDate(), period.endDate(), period.status(), period.createdAt(), period.updatedAt());
    }
}
