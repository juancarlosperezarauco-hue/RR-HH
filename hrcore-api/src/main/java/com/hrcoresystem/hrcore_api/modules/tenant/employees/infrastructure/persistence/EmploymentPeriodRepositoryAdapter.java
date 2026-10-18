package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmploymentPeriodRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Repository
public class EmploymentPeriodRepositoryAdapter implements EmploymentPeriodRepository {
    private final EmploymentPeriodJpaRepository jpa;
    public EmploymentPeriodRepositoryAdapter(EmploymentPeriodJpaRepository jpa) { this.jpa = jpa; }
    public Optional<EmploymentPeriod> findById(UUID id) { return jpa.findById(id).map(this::toDomain); }
    public Optional<EmploymentPeriod> findByIdAndEmployeeId(UUID id, UUID employeeId) { return jpa.findByIdAndEmployeeId(id, employeeId).map(this::toDomain); }
    public Optional<EmploymentPeriod> findLatestByEmployeeId(UUID employeeId) { return jpa.findFirstByEmployeeIdOrderByPeriodNumberDesc(employeeId).map(this::toDomain); }
    public Optional<EmploymentPeriod> findOpenByEmployeeId(UUID employeeId) { return jpa.findFirstByEmployeeIdAndEndDateIsNullOrderByPeriodNumberDesc(employeeId).map(this::toDomain); }
    public Optional<EmploymentPeriod> findByEmployeeIdAndDate(UUID employeeId, LocalDate date) {
        List<EmploymentPeriodEntity> matches = jpa.findByEmployeeIdAndDate(employeeId, date);
        if (matches.size() > 1) throw new IllegalStateException("Multiple employment periods match the requested date");
        return matches.stream().findFirst().map(this::toDomain);
    }
    public List<EmploymentPeriod> findAllByEmployeeId(UUID employeeId) { return jpa.findAllByEmployeeIdOrderByPeriodNumberDesc(employeeId).stream().map(this::toDomain).toList(); }
    public EmploymentPeriod createRehirePeriod(UUID employeeId, LocalDate startDate) {
        Instant now = Instant.now();
        EmploymentPeriodEntity e = new EmploymentPeriodEntity();
        e.setEmployeeId(employeeId);
        e.setStartDate(startDate);
        e.setStatus(EmploymentStatus.ACTIVE);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return toDomain(jpa.saveAndFlush(e));
    }
    private EmploymentPeriod toDomain(EmploymentPeriodEntity e) { return new EmploymentPeriod(e.getId(), e.getEmployeeId(), e.getPeriodNumber(), e.getStartDate(), e.getEndDate(), e.getStatus(), e.getCreatedAt(), e.getUpdatedAt()); }
}
