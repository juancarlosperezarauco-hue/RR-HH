package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmployeeEmergencyContact;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeEmergencyContactRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EmployeeEmergencyContactRepositoryAdapter implements EmployeeEmergencyContactRepository {
    private final EmployeeEmergencyContactJpaRepository jpa;
    public EmployeeEmergencyContactRepositoryAdapter(EmployeeEmergencyContactJpaRepository jpa) { this.jpa = jpa; }

    @Override public EmployeeEmergencyContact save(EmployeeEmergencyContact c) {
        EmployeeEmergencyContactEntity e = c.id() == null ? new EmployeeEmergencyContactEntity()
                : jpa.findByIdAndEmployeeId(c.id(), c.employeeId()).orElseThrow();
        e.setEmployeeId(c.employeeId()); e.setFullName(c.fullName()); e.setRelationship(c.relationship());
        e.setPhone(c.phone()); e.setAlternativePhone(c.alternativePhone()); e.setAddress(c.address());
        e.setPrimary(c.primary());
        return toDomain(jpa.saveAndFlush(e));
    }
    @Override public Optional<EmployeeEmergencyContact> findByIdAndEmployeeId(UUID id, UUID employeeId) {
        return jpa.findByIdAndEmployeeId(id, employeeId).map(this::toDomain);
    }
    @Override public Page<EmployeeEmergencyContact> findByEmployeeId(UUID employeeId, Pageable pageable) {
        return jpa.findByEmployeeId(employeeId, pageable).map(this::toDomain);
    }
    @Override public boolean existsOtherPrimary(UUID employeeId, UUID exceptId) {
        return exceptId == null ? jpa.existsByEmployeeIdAndPrimaryTrue(employeeId)
                : jpa.existsByEmployeeIdAndPrimaryTrueAndIdNot(employeeId, exceptId);
    }
    @Override public void delete(EmployeeEmergencyContact c) {
        EmployeeEmergencyContactEntity e = jpa.findByIdAndEmployeeId(c.id(), c.employeeId()).orElseThrow();
        jpa.delete(e); jpa.flush();
    }
    private EmployeeEmergencyContact toDomain(EmployeeEmergencyContactEntity e) {
        return new EmployeeEmergencyContact(e.getId(), e.getEmployeeId(), e.getFullName(), e.getRelationship(),
                e.getPhone(), e.getAlternativePhone(), e.getAddress(), e.isPrimary(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
