package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import java.time.*;
import java.util.*;

public final class MyEmployeeResponses {
    private MyEmployeeResponses() {}
    public record Profile(UUID employeeId, String employeeNumber, String firstName, String middleName, String lastName,
            String secondLastName, String fullName, String documentType, String documentNumber, LocalDate birthDate,
            String personalEmail, String workEmail, String phone, String mobilePhone, String address,
            EmploymentStatus employmentStatus, LocalDate latestHireDate) {}
    public record Employment(UUID employeeId, String employeeNumber, EmploymentStatus employmentStatus,
            EmploymentPeriod currentEmploymentPeriod, List<EmploymentPeriod> employmentPeriods,
            UUID currentDepartmentId, UUID currentPositionId, UUID currentSupervisorId,
            LocalDate hireDate, LocalDate terminationDate) {}
}
