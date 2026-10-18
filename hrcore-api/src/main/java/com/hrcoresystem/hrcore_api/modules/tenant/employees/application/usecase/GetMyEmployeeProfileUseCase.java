package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.MyEmployeeResponses.Profile;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
@Service public class GetMyEmployeeProfileUseCase {
    private final CurrentEmployeeResolver current; private final EmploymentPeriodService periods;
    public GetMyEmployeeProfileUseCase(CurrentEmployeeResolver c, EmploymentPeriodService p){current=c;periods=p;}
    public Profile execute(){Employee e=current.require(); var all=periods.list(e.id()); LocalDate latest=all.stream().map(p->p.startDate()).max(LocalDate::compareTo).orElse(e.hireDate()); String full=String.join(" ",java.util.stream.Stream.of(e.firstName(),e.middleName(),e.lastName(),e.secondLastName()).filter(v->v!=null&&!v.isBlank()).toList()); return new Profile(e.id(),e.employeeNumber(),e.firstName(),e.middleName(),e.lastName(),e.secondLastName(),full,e.documentType(),e.documentNumber(),e.birthDate(),e.personalEmail(),e.workEmail(),e.phone(),e.mobilePhone(),e.address(),e.employmentStatus(),latest);}
}
