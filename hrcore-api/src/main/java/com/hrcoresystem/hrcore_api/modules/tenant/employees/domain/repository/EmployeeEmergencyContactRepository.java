package com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmployeeEmergencyContact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeEmergencyContactRepository {
    EmployeeEmergencyContact save(EmployeeEmergencyContact contact);
    Optional<EmployeeEmergencyContact> findByIdAndEmployeeId(UUID id, UUID employeeId);
    Page<EmployeeEmergencyContact> findByEmployeeId(UUID employeeId, Pageable pageable);
    boolean existsOtherPrimary(UUID employeeId, UUID exceptId);
    void delete(EmployeeEmergencyContact contact);
}
