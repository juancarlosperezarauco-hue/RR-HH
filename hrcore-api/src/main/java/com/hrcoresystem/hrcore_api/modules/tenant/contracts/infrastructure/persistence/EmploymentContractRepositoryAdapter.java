package com.hrcoresystem.hrcore_api.modules.tenant.contracts.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.repository.EmploymentContractRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.*;

@Repository
public class EmploymentContractRepositoryAdapter implements EmploymentContractRepository {
    private final EmploymentContractJpaRepository jpa;
    public EmploymentContractRepositoryAdapter(EmploymentContractJpaRepository jpa) { this.jpa = jpa; }

    @Override public EmploymentContract save(EmploymentContract c) {
        EmploymentContractEntity e = c.id() == null ? new EmploymentContractEntity()
                : jpa.findById(c.id()).orElseThrow();
        e.setEmployeeId(c.employeeId()); e.setEmployeeAssignmentId(c.employeeAssignmentId());
        e.setContractNumber(c.contractNumber()); e.setContractType(c.contractType()); e.setStatus(c.status());
        e.setContractForm(c.contractForm()); e.setWorkSchedule(c.workSchedule()); e.setWorkModality(c.workModality());
        e.setStartDate(c.startDate()); e.setEndDate(c.endDate()); e.setSignedAt(c.signedAt());
        e.setBaseSalary(c.baseSalary()); e.setCurrency(c.currency()); e.setPaymentFrequency(c.paymentFrequency());
        e.setWeeklyHours(c.weeklyHours()); e.setProbationEndDate(c.probationEndDate());
        e.setTerminationDate(c.terminationDate()); e.setTerminationReason(c.terminationReason());
        e.setTerms(c.terms()); e.setRenewedFromContractId(c.renewedFromContractId());
        e.setCreatedBy(c.createdBy()); e.setUpdatedBy(c.updatedBy());
        return toDomain(jpa.saveAndFlush(e));
    }
    @Override public Optional<EmploymentContract> findById(UUID id) { return jpa.findById(id).map(this::toDomain); }
    @Override public Optional<EmploymentContract> findByIdForUpdate(UUID id) { return jpa.findByIdForUpdate(id).map(this::toDomain); }
    @Override public boolean existsContractNumberOtherThan(String n, UUID id) {
        return id == null ? jpa.existsByContractNumber(n) : jpa.existsByContractNumberAndIdNot(n, id);
    }
    @Override public boolean existsOverlappingActive(UUID employeeId, UUID exceptId, LocalDate start, LocalDate end) {
        return jpa.existsOverlappingActive(employeeId, exceptId, start, end);
    }
    @Override public boolean existsRenewal(UUID sourceContractId) { return jpa.existsByRenewedFromContractId(sourceContractId); }
    @Override public Page<EmploymentContract> findAll(ContractFilter filter, Pageable pageable) {
        Specification<EmploymentContractEntity> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (filter.employeeId() != null) p.add(cb.equal(root.get("employeeId"), filter.employeeId()));
            if (filter.type() != null) p.add(cb.equal(root.get("contractType"), filter.type()));
            if (filter.status() != null) p.add(cb.equal(root.get("status"), filter.status()));
            if (filter.expiringBefore() != null) {
                p.add(cb.isNotNull(root.get("endDate")));
                p.add(cb.lessThanOrEqualTo(root.get("endDate"), filter.expiringBefore()));
            }
            if (filter.search() != null && !filter.search().isBlank()) {
                String value = "%" + filter.search().trim().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
                p.add(cb.like(cb.lower(root.get("contractNumber")), value, '\\'));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return jpa.findAll(spec, pageable).map(this::toDomain);
    }
    private EmploymentContract toDomain(EmploymentContractEntity e) {
        return new EmploymentContract(e.getId(), e.getEmployeeId(), e.getEmployeeAssignmentId(),
                e.getContractNumber(), e.getContractType(), e.getStatus(), e.getContractForm(),
                e.getWorkSchedule(), e.getWorkModality(), e.getStartDate(), e.getEndDate(), e.getSignedAt(),
                e.getBaseSalary(), e.getCurrency(), e.getPaymentFrequency(), e.getWeeklyHours(),
                e.getProbationEndDate(), e.getTerminationDate(), e.getTerminationReason(), e.getTerms(),
                e.getRenewedFromContractId(), e.getCreatedBy(), e.getUpdatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
