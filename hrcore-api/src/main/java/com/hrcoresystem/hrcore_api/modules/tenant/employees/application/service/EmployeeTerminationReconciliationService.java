package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.repository.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeAssignmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftAssignmentRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service public class EmployeeTerminationReconciliationService {
    public record Result(int terminatedContracts,int cancelledContracts,int closedAssignments,int cancelledAssignments,int closedShiftAssignments,int cancelledShiftAssignments) {}
    private final EmploymentContractRepository contracts; private final ContractHistoryRepository contractHistory; private final EmployeeAssignmentRepository assignments; private final ShiftAssignmentRepository shifts; private final AuditTrailService audit;
    public EmployeeTerminationReconciliationService(EmploymentContractRepository contracts,ContractHistoryRepository contractHistory,EmployeeAssignmentRepository assignments,ShiftAssignmentRepository shifts,AuditTrailService audit){this.contracts=contracts;this.contractHistory=contractHistory;this.assignments=assignments;this.shifts=shifts;this.audit=audit;}
    public Result reconcile(Employee employee,LocalDate date,String reason,UUID actor){int terminated=0,cancelled=0,closed=0,cancelledAssignments=0,closedShifts=0,cancelledShifts=0;
        for(EmploymentContract c:contracts.findAllByEmployeeIdForUpdate(employee.id())){ContractStatus target=null;if(c.status()==ContractStatus.DRAFT||c.status()==ContractStatus.PENDING_SIGNATURE)target=ContractStatus.CANCELLED;else if(c.status()==ContractStatus.ACTIVE){if(c.endDate()!=null&&c.endDate().isBefore(date))target=ContractStatus.EXPIRED;else if(c.startDate().isAfter(date))target=ContractStatus.CANCELLED;else target=ContractStatus.TERMINATED;}if(target!=null){contracts.save(copy(c,target,target==ContractStatus.TERMINATED?date:c.terminationDate(),target==ContractStatus.TERMINATED?reason:c.terminationReason()));contractHistory.save(new ContractHistory(null,c.id(),action(target),c.status(),target,Map.of(),reason,actor,null));audit.recordTenantEvent("CONTRACT_STATUS_CHANGED","EMPLOYMENT_CONTRACT",c.id().toString(),Map.of("previousStatus",c.status(),"newStatus",target));if(target==ContractStatus.TERMINATED)terminated++;if(target==ContractStatus.CANCELLED)cancelled++;}}
        for(EmployeeAssignment a:assignments.findAllByEmployeeIdForUpdate(employee.id())){if(a.status()!=EmployeeAssignmentStatus.ACTIVE)continue;if(a.startDate().isAfter(date)){assignments.save(copy(a, a.endDate(),EmployeeAssignmentStatus.CANCELLED));cancelledAssignments++;}else if(a.endDate()==null||a.endDate().isAfter(date)){assignments.save(copy(a,date,EmployeeAssignmentStatus.ACTIVE));closed++;}}
        for(EmployeeShiftAssignment s:shifts.findAllByEmployeeIdForUpdate(employee.id())){if(s.status()!=ShiftAssignmentStatus.ACTIVE)continue;if(s.startDate().isAfter(date)){shifts.save(copy(s,s.endDate(),ShiftAssignmentStatus.CANCELLED));cancelledShifts++;}else if(s.endDate()==null||s.endDate().isAfter(date)){shifts.save(copy(s,date,ShiftAssignmentStatus.INACTIVE));closedShifts++;}}
        return new Result(terminated,cancelled,closed,cancelledAssignments,closedShifts,cancelledShifts);
    }
    private EmploymentContract copy(EmploymentContract c,ContractStatus status,LocalDate termination,String reason){return new EmploymentContract(c.id(),c.employeeId(),c.employeeAssignmentId(),c.contractNumber(),c.contractType(),status,c.contractForm(),c.workSchedule(),c.workModality(),c.startDate(),c.endDate(),c.signedAt(),c.baseSalary(),c.currency(),c.paymentFrequency(),c.weeklyHours(),c.probationEndDate(),termination,reason,c.terms(),c.renewedFromContractId(),c.createdBy(),c.updatedBy(),c.createdAt(),c.updatedAt(),c.employmentPeriodId());}
    private ContractHistoryAction action(ContractStatus s){return switch(s){case TERMINATED->ContractHistoryAction.TERMINATED;case CANCELLED->ContractHistoryAction.CANCELLED;case EXPIRED->ContractHistoryAction.EXPIRED;default->ContractHistoryAction.UPDATED;};}
    private EmployeeAssignment copy(EmployeeAssignment a,LocalDate end,EmployeeAssignmentStatus status){return new EmployeeAssignment(a.id(),a.employeeId(),a.departmentId(),a.positionId(),a.supervisorEmployeeId(),a.startDate(),end,a.assignmentType(),a.reason(),a.createdBy(),a.createdAt(),a.updatedAt(),status);}
    private EmployeeShiftAssignment copy(EmployeeShiftAssignment a,LocalDate end,ShiftAssignmentStatus status){return new EmployeeShiftAssignment(a.id(),a.employeeId(),a.shiftId(),a.employmentPeriodId(),a.startDate(),end,a.weekdays(),status,a.createdBy(),a.createdAt(),a.updatedAt());}
}
