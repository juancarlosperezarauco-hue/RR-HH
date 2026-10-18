package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmployeeFilter;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Procesa jornadas cerradas de forma idempotente y en lotes acotados. */
@Service
public class AttendanceDailyProcessingExecutor {
    private static final Logger log = LoggerFactory.getLogger(AttendanceDailyProcessingExecutor.class);
    private final EmployeeRepository employees;
    private final AttendanceCalculationService calculations;
    private final AttendancePolicyService policies;

    public AttendanceDailyProcessingExecutor(EmployeeRepository employees, AttendanceCalculationService calculations,
            AttendancePolicyService policies) {
        this.employees = employees;
        this.calculations = calculations;
        this.policies = policies;
    }

    public ProcessingResult execute(LocalDate today, int recoveryDays) {
        if (!policies.enabled("attendance.daily_processing_enabled", true))
            return new ProcessingResult(0, 0, 0);
        LocalDate last = today.minusDays(1);
        LocalDate first = last.minusDays(Math.max(0, recoveryDays - 1));
        int candidates = 0, processed = 0, failed = 0;
        for (int page = 0; ; page++) {
            var batch = employees.findAll(new EmployeeFilter(null, null, null, null, null),
                    org.springframework.data.domain.PageRequest.of(page, 100)).getContent();
            if (batch.isEmpty()) break;
            for (var employee : batch) {
                for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
                    candidates++;
                    try { calculations.calculate(employee.id(), date); processed++; }
                    catch (RuntimeException ex) { failed++; log.warn("No se pudo procesar asistencia employee={} date={}: {}", employee.id(), date, ex.getMessage()); }
                }
            }
            if (batch.size() < 100) break;
        }
        return new ProcessingResult(candidates, processed, failed);
    }

    public record ProcessingResult(int candidates, int processed, int failed) { }
}
