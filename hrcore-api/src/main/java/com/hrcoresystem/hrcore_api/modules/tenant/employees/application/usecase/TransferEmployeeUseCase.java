package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;
import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.UUID;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
@Service
public class TransferEmployeeUseCase {
    private final EmployeeAssignmentRepository assignments; private final EmployeePolicy employees;
    private final AssignmentPolicy policy; private final EmployeeAudit audit; private final EmployeeMapper mapper;
    private final TenantTimezoneService timezone;
    public TransferEmployeeUseCase(EmployeeAssignmentRepository assignments, EmployeePolicy employees,
            AssignmentPolicy policy, EmployeeAudit audit, EmployeeMapper mapper, TenantTimezoneService timezone) {
        this.assignments = assignments; this.employees = employees; this.policy = policy;
        this.audit = audit; this.mapper = mapper; this.timezone = timezone;
    }
    @Transactional
    public EmployeeTransferResponse execute(UUID employeeId, TransferEmployeeRequest request) {
        employees.assertOperational(); Employee employee = employees.requireForUpdate(employeeId);
        employees.assertNotTerminated(employee);
        LocalDate today = timezone.today();
        if (request.effectiveDate() == null || !request.effectiveDate().isAfter(today))
            throw new BusinessException("effectiveDate must be after today");
        if (request.reason() == null || request.reason().trim().isBlank())
            throw new BusinessException("reason is required");
        EmployeeAssignment previous = assignments.findCurrentPrimaryForUpdate(employeeId, today)
                .orElseThrow(() -> new BusinessException("No current primary assignment exists"));
        EmployeeAssignment closed = assignments.save(policy.transferred(previous, request.effectiveDate().minusDays(1)));
        EmployeeAssignmentRequest next = new EmployeeAssignmentRequest(request.departmentId(), request.positionId(),
                request.supervisorEmployeeId(), request.effectiveDate(), null, AssignmentType.PRIMARY, request.reason());
        EmployeeAssignment created = assignments.save(policy.normalized(employee, next, null, employees.actorId()));
        audit.record("EMPLOYEE_ASSIGNMENT", "TRANSFERRED", created.id(), java.util.List.of("previousAssignmentId", "effectiveDate", "reason"));
        return new EmployeeTransferResponse(employeeId, mapper.toResponse(closed), mapper.toResponse(created),
                request.effectiveDate(), request.reason().trim());
    }
}
