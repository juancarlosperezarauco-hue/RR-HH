package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException;
import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
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
    private final TenantUserRepository users;
    private final AssignmentPolicy assignmentPolicy;
    @org.springframework.beans.factory.annotation.Autowired
    public RehireEmployeeUseCase(EmployeeRepository employees, EmploymentPeriodRepository periods,
            EmploymentPeriodMapper periodMapper, EmployeeMapper employeeMapper, EmployeePolicy policy,
            CreateEmployeeAssignmentUseCase createAssignment, EmployeeAudit audit, TenantTimezoneService timezone,
            TenantUserRepository users, AssignmentPolicy assignmentPolicy) {
        this.employees = employees; this.periods = periods; this.periodMapper = periodMapper;
        this.employeeMapper = employeeMapper; this.policy = policy; this.createAssignment = createAssignment;
        this.audit = audit; this.timezone = timezone;
        this.users = users;
        this.assignmentPolicy = assignmentPolicy;
    }
    public RehireEmployeeUseCase(EmployeeRepository employees, EmploymentPeriodRepository periods,
            EmploymentPeriodMapper periodMapper, EmployeeMapper employeeMapper, EmployeePolicy policy,
            CreateEmployeeAssignmentUseCase createAssignment, EmployeeAudit audit, TenantTimezoneService timezone,
            TenantUserRepository users) {
        this(employees, periods, periodMapper, employeeMapper, policy, createAssignment, audit, timezone, users, null);
    }
    @Transactional
    public RehireEmployeeResponse execute(UUID employeeId, RehireEmployeeRequest request) {
        policy.assertOperational();
        if (assignmentPolicy != null) assignmentPolicy.lockForWrite();
        policy.requireForUpdate(employeeId);
        var latest = periods.findLatestByEmployeeId(employeeId).orElseThrow(
                () -> new BusinessException("El empleado no tiene períodos laborales"));
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
        var refreshed = employees.refresh(employeeId);
        if (refreshed.employmentStatus() != EmploymentStatus.ACTIVE)
            throw new IllegalStateException("La recontratación no sincronizó el estado laboral del empleado");
        if (!request.startDate().equals(refreshed.hireDate()))
            throw new IllegalStateException("La fecha de ingreso no coincide con el nuevo período laboral");
        if (refreshed.terminationDate() != null)
            throw new IllegalStateException("El empleado recontratado conserva una fecha de desvinculación");
        EmployeeAssignmentResponse assignment = request.assignment() == null ? null : createAssignment.execute(employeeId, request.assignment());
        boolean userActivationRequired = requiresUserActivation(refreshed.tenantUserId());
        audit.record("EMPLOYEE", "EMPLOYEE_REHIRED", employeeId, List.of("employmentPeriodId"));
        return new RehireEmployeeResponse(employeeMapper.toResponse(refreshed), periodMapper.toResponse(created), assignment, userActivationRequired);
    }

    private boolean requiresUserActivation(UUID tenantUserId) {
        if (tenantUserId == null) return false;
        var user = users.findById(tenantUserId).orElseThrow(() -> new BusinessException("La cuenta vinculada al empleado no existe"));
        return !user.active() || user.status() != TenantUserStatus.ACTIVE;
    }
}
