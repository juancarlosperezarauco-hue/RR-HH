package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentPeriod;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmploymentPeriodRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class EmploymentPeriodService {
    private final EmploymentPeriodRepository periods;
    public EmploymentPeriodService(EmploymentPeriodRepository periods) { this.periods = periods; }
    public List<EmploymentPeriod> list(UUID employeeId) { return periods.findAllByEmployeeId(employeeId); }
    public EmploymentPeriod require(UUID employeeId, UUID periodId) { return periods.findByIdAndEmployeeId(periodId, employeeId).orElseThrow(() -> new ResourceNotFoundException("Período laboral no encontrado")); }
    public EmploymentPeriod requireForDate(UUID employeeId, LocalDate date) {
        if (date == null) throw new BusinessException("La fecha laboral es obligatoria");
        return periods.findByEmployeeIdAndDate(employeeId, date).orElseThrow(() -> new BusinessException("La fecha indicada no pertenece a un período laboral del empleado"));
    }
    public EmploymentPeriod requireLatest(UUID employeeId) { return periods.findLatestByEmployeeId(employeeId).orElseThrow(() -> new BusinessException("El empleado no tiene períodos laborales")); }
    public EmploymentPeriod requireOpen(UUID employeeId) { return periods.findOpenByEmployeeId(employeeId).filter(p -> p.status() != EmploymentStatus.TERMINATED).orElseThrow(() -> new BusinessException("El empleado no tiene un período laboral abierto")); }
}
