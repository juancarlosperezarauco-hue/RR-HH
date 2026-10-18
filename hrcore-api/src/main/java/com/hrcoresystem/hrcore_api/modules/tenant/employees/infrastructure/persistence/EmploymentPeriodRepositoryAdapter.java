package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmploymentPeriodRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Repository
public class EmploymentPeriodRepositoryAdapter implements EmploymentPeriodRepository {
    private final EmploymentPeriodJpaRepository jpa;
    @PersistenceContext
    private EntityManager entityManager;
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
    @Override
    public EmploymentPeriod createRehirePeriod(UUID employeeId, LocalDate startDate) {
        if (employeeId == null) throw new IllegalArgumentException("employeeId is required");
        if (startDate == null) throw new IllegalArgumentException("startDate is required");

        EmploymentPeriodEntity latest = jpa.findFirstByEmployeeIdOrderByPeriodNumberDesc(employeeId)
                .orElseThrow(() -> new BusinessConflictException("El empleado no tiene un período laboral previo"));
        if (latest.getStatus() != EmploymentStatus.TERMINATED || latest.getEndDate() == null)
            throw new BusinessConflictException("El último período laboral debe estar terminado");
        if (!startDate.isAfter(latest.getEndDate()))
            throw new BusinessConflictException("La fecha del reingreso debe ser posterior al último período laboral");
        if (latest.getPeriodNumber() == null)
            throw new IllegalStateException("El último período laboral no tiene número");

        int nextPeriodNumber = Math.addExact(latest.getPeriodNumber(), 1);
        Instant now = Instant.now();
        EmploymentPeriodEntity e = new EmploymentPeriodEntity();
        e.setEmployeeId(employeeId);
        e.setPeriodNumber(nextPeriodNumber);
        e.setStartDate(startDate);
        e.setEndDate(null);
        e.setStatus(EmploymentStatus.ACTIVE);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        EmploymentPeriodEntity saved = jpa.saveAndFlush(e);
        entityManager.refresh(saved);
        return toDomain(saved);
    }
    private EmploymentPeriod toDomain(EmploymentPeriodEntity e) { return new EmploymentPeriod(e.getId(), e.getEmployeeId(), e.getPeriodNumber(), e.getStartDate(), e.getEndDate(), e.getStatus(), e.getCreatedAt(), e.getUpdatedAt()); }
}
