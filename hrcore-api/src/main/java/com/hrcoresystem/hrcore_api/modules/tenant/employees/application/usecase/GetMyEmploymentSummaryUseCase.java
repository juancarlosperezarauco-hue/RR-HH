package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.MyEmployeeResponses.Employment;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeAssignmentRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
@Service @org.springframework.transaction.annotation.Transactional(readOnly = true) public class GetMyEmploymentSummaryUseCase {
 private final CurrentEmployeeResolver current; private final EmploymentPeriodService periods; private final EmployeeAssignmentRepository assignments; private final TenantTimezoneService timezone;
 public GetMyEmploymentSummaryUseCase(CurrentEmployeeResolver c,EmploymentPeriodService p,EmployeeAssignmentRepository a,TenantTimezoneService t){current=c;periods=p;assignments=a;timezone=t;}
 public Employment execute(){var e=current.require();var all=periods.list(e.id());var open=all.stream().filter(p->p.endDate()==null).findFirst().orElse(null);var assignment=open==null?null:assignments.findCurrentPrimary(e.id(),timezone.today()).orElse(null);return new Employment(e.id(),e.employeeNumber(),e.employmentStatus(),open,all,assignment==null?null:assignment.departmentId(),assignment==null?null:assignment.positionId(),assignment==null?null:assignment.supervisorEmployeeId(),e.hireDate(),e.terminationDate());}
}
