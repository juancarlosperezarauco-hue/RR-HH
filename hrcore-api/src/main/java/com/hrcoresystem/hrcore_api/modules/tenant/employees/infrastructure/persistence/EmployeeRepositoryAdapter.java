package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EmployeeRepositoryAdapter implements EmployeeRepository {
    private final EmployeeJpaRepository jpa;
    public EmployeeRepositoryAdapter(EmployeeJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public Employee save(Employee p) {
        EmployeeEntity e = p.id() == null ? new EmployeeEntity() : jpa.findById(p.id()).orElseThrow();
        e.setTenantUserId(p.tenantUserId()); e.setEmployeeNumber(p.employeeNumber());
        e.setFirstName(p.firstName()); e.setMiddleName(p.middleName()); e.setLastName(p.lastName());
        e.setSecondLastName(p.secondLastName()); e.setDocumentType(p.documentType());
        e.setDocumentNumber(p.documentNumber()); e.setBirthDate(p.birthDate()); e.setGender(p.gender());
        e.setNationality(p.nationality()); e.setPersonalEmail(p.personalEmail()); e.setWorkEmail(p.workEmail());
        e.setPhone(p.phone()); e.setMobilePhone(p.mobilePhone()); e.setAddress(p.address());
        e.setHireDate(p.hireDate()); e.setTerminationDate(p.terminationDate());
        e.setEmploymentStatus(p.employmentStatus()); e.setCreatedBy(p.createdBy()); e.setUpdatedBy(p.updatedBy());
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override public Optional<Employee> findById(UUID id) { return jpa.findById(id).map(this::toDomain); }
    @Override public Optional<Employee> findByIdForUpdate(UUID id) { return jpa.findByIdForUpdate(id).map(this::toDomain); }
    @Override public boolean existsNumberOtherThan(String number, UUID id) {
        return id == null ? jpa.existsByEmployeeNumber(number) : jpa.existsByEmployeeNumberAndIdNot(number, id);
    }
    @Override public boolean existsDocumentOtherThan(String type, String number, UUID id) {
        return id == null ? jpa.existsByDocumentTypeAndDocumentNumber(type, number)
                : jpa.existsByDocumentTypeAndDocumentNumberAndIdNot(type, number, id);
    }
    @Override public boolean existsTenantUserOtherThan(UUID tenantUserId, UUID id) {
        return id == null ? jpa.existsByTenantUserId(tenantUserId)
                : jpa.existsByTenantUserIdAndIdNot(tenantUserId, id);
    }

    @Override
    public Page<Employee> findAll(EmployeeFilter filter, Pageable pageable) {
        Specification<EmployeeEntity> spec = (root, query, cb) -> {
            var conditions = new ArrayList<Predicate>();
            if (filter.status() != null) conditions.add(cb.equal(root.get("employmentStatus"), filter.status()));
            if (filter.employeeNumber() != null && !filter.employeeNumber().isBlank())
                conditions.add(cb.equal(root.get("employeeNumber"), filter.employeeNumber().trim()));
            if (filter.name() != null && !filter.name().isBlank()) {
                String pattern = "%" + escapeLike(filter.name().trim().toLowerCase()) + "%";
                conditions.add(cb.or(cb.like(cb.lower(root.get("firstName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("lastName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("middleName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("secondLastName")), pattern, '\\')));
            }
            if (filter.departmentId() != null || filter.positionId() != null) {
                var sub = query.subquery(UUID.class);
                var assignment = sub.from(EmployeeAssignmentEntity.class);
                var joinConditions = new ArrayList<Predicate>();
                joinConditions.add(cb.equal(assignment.get("employeeId"), root.get("id")));
                LocalDate today = LocalDate.now();
                joinConditions.add(cb.lessThanOrEqualTo(assignment.get("startDate"), today));
                joinConditions.add(cb.or(cb.isNull(assignment.get("endDate")),
                        cb.greaterThanOrEqualTo(assignment.get("endDate"), today)));
                if (filter.departmentId() != null)
                    joinConditions.add(cb.equal(assignment.get("departmentId"), filter.departmentId()));
                if (filter.positionId() != null)
                    joinConditions.add(cb.equal(assignment.get("positionId"), filter.positionId()));
                sub.select(assignment.get("employeeId")).where(cb.and(joinConditions.toArray(Predicate[]::new)));
                conditions.add(cb.exists(sub));
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
        return jpa.findAll(spec, pageable).map(this::toDomain);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Employee toDomain(EmployeeEntity e) {
        return new Employee(e.getId(), e.getTenantUserId(), e.getEmployeeNumber(), e.getFirstName(),
                e.getMiddleName(), e.getLastName(), e.getSecondLastName(), e.getDocumentType(),
                e.getDocumentNumber(), e.getBirthDate(), e.getGender(), e.getNationality(),
                e.getPersonalEmail(), e.getWorkEmail(), e.getPhone(), e.getMobilePhone(), e.getAddress(),
                e.getHireDate(), e.getTerminationDate(), e.getEmploymentStatus(), e.getCreatedBy(),
                e.getUpdatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
