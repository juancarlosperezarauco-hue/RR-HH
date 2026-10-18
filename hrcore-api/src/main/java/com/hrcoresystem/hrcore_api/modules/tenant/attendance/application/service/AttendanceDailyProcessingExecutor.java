package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.EmploymentPeriodService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmployeeFilter;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentPeriod;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

/** Procesa jornadas cerradas de forma idempotente y en lotes acotados. */
@Service
public class AttendanceDailyProcessingExecutor {
    private static final Logger log = LoggerFactory.getLogger(AttendanceDailyProcessingExecutor.class);
    private static final int PAGE_SIZE = 100;
    private final EmployeeRepository employees;
    private final AttendanceCalculationService calculations;
    private final AttendancePolicyService policies;
    private final EmploymentPeriodService periods;

    @Autowired
    public AttendanceDailyProcessingExecutor(EmployeeRepository employees, AttendanceCalculationService calculations,
            AttendancePolicyService policies, EmploymentPeriodService periods) {
        this.employees = employees; this.calculations = calculations; this.policies = policies; this.periods = periods;
    }

    public AttendanceDailyProcessingExecutor(EmployeeRepository employees, AttendanceCalculationService calculations,
            AttendancePolicyService policies) {
        this.employees = employees; this.calculations = calculations; this.policies = policies; this.periods = null;
    }

    public ProcessingResult execute(LocalDate today, int recoveryDays) {
        if (!policies.enabled("attendance.daily_processing_enabled", true)) return new ProcessingResult(0, 0, 0);
        if (today == null) throw new IllegalArgumentException("Processing date is required");
        LocalDate last = today.minusDays(1);
        LocalDate first = last.minusDays(Math.max(0, recoveryDays - 1));
        int candidates = 0, processed = 0, failed = 0;
        for (int page = 0; ; page++) {
            var batch = employees.findAll(new EmployeeFilter(null, null, null, null, null), PageRequest.of(page, PAGE_SIZE)).getContent();
            if (batch.isEmpty()) break;
            for (var employee : batch) {
                List<EmploymentPeriod> employmentPeriods = periods == null ? List.of() : periods.list(employee.id());
                for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
                    if (periods != null && !belongsToEmploymentPeriod(employmentPeriods, date)) continue;
                    candidates++;
                    try { calculations.calculate(employee.id(), date); processed++; }
                    catch (RuntimeException ex) { failed++; log.warn("No se pudo procesar asistencia employee={} date={}: {}", employee.id(), date, ex.getMessage()); }
                }
            }
            if (batch.size() < PAGE_SIZE) break;
        }
        return new ProcessingResult(candidates, processed, failed);
    }

    private boolean belongsToEmploymentPeriod(List<EmploymentPeriod> employmentPeriods, LocalDate date) {
        for (EmploymentPeriod period : employmentPeriods) {
            boolean started = !date.isBefore(period.startDate());
            boolean notEnded = period.endDate() == null || !date.isAfter(period.endDate());
            if (started && notEnded) return true;
        }
        return false;
    }

    public record ProcessingResult(int candidates, int processed, int failed) { }
}
