package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException;
import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmploymentPeriodMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmploymentPeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class RehireEmployeeUseCase {
    private final EmployeeRepository employees; private final EmploymentPeriodRepository periods;
    private final EmploymentPeriodMapper periodMapper; private final EmployeeMapper employeeMapper;
    private final EmployeePolicy policy; private final CreateEmployeeAssignmentUseCase createAssignment;
    private final EmployeeAudit audit; private final TenantTimezoneService timezone;
    public RehireEmployeeUseCase(EmployeeRepository employees, EmploymentPeriodRepository periods,
            EmploymentPeriodMapper periodMapper, EmployeeMapper employeeMapper, EmployeePolicy policy,
            CreateEmployeeAssignmentUseCase createAssignment, EmployeeAudit audit, TenantTimezoneService timezone) {
        this.employees = employees; this.periods = periods; this.periodMapper = periodMapper;
        this.employeeMapper = employeeMapper; this.policy = policy; this.createAssignment = createAssignment;
        this.audit = audit; this.timezone = timezone;
    }
    @Transactional
    public RehireEmployeeResponse execute(UUID employeeId, RehireEmployeeRequest request) {
        policy.assertOperational(); policy.requireForUpdate(employeeId);
        var latest = periods.findLatestByEmployeeId(employeeId).orElseThrow(() -> new BusinessException("El empleado no tiene períodos laborales"));
        if (latest.status() != EmploymentStatus.TERMINATED || latest.endDate() == null)
            throw new BusinessConflictException("El último período laboral no está terminado");
        if (request == null || request.startDate() == null)
            throw new BusinessException("La fecha de recontratación es obligatoria");
        if (!request.startDate().isAfter(latest.endDate()))
            throw new BusinessConflictException("La fecha de recontratación debe ser posterior al fin del período laboral anterior (" + latest.endDate() + ")");
        if (periods.findOpenByEmployeeId(employeeId).isPresent())
            throw new BusinessConflictException("El empleado ya tiene un período laboral abierto");
        if (!request.startDate().equals(timezone.today()))
            throw new BusinessException("La fecha de recontratación debe coincidir con la fecha empresarial actual");
        var created = periods.createRehirePeriod(employeeId, request.startDate());
        var refreshed = employees.findById(employeeId).orElseThrow(() -> new BusinessException("No se pudo actualizar el empleado recontratado"));
        EmployeeAssignmentResponse assignment = request.assignment() == null ? null : createAssignment.execute(employeeId, request.assignment());
        audit.record("EMPLOYEE", "EMPLOYEE_REHIRED", employeeId, List.of("employmentPeriodId"));
        return new RehireEmployeeResponse(employeeMapper.toResponse(refreshed), periodMapper.toResponse(created), assignment);
    }
}
