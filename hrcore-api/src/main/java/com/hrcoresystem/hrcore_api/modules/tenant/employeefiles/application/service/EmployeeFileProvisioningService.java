package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.BusinessSequenceService;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto.FileResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.EmployeeFile;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.FileStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.EmployeeFileRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EmployeeFileProvisioningService {
  private final EmployeeFileRepository files;
  private final EmployeeFilePolicy policy;
  private final BusinessSequenceService sequences;

  public EmployeeFileProvisioningService(EmployeeFileRepository files, EmployeeFilePolicy policy,
                                         BusinessSequenceService sequences) {
    this.files = files;
    this.policy = policy;
    this.sequences = sequences;
  }

  public FileResponse provision(Employee employee) {
    policy.operational();
    if (files.findByEmployeeId(employee.id()).isPresent())
      throw new BusinessException("Employee already has a file");
    EmployeeFile file = files.save(new EmployeeFile(UUID.randomUUID(), employee.id(),
        sequences.nextCode("employee_file"), FileStatus.ACTIVE, null, policy.actor(), null, null));
    policy.audit("EMPLOYEE_FILE_CREATED", "EMPLOYEE_FILE", file.id(), Map.of("employeeId", file.employeeId()));
    return new FileResponse(file.id(), file.employeeId(), file.fileNumber(), file.status(), file.notes(), file.createdAt(), file.updatedAt());
  }
}
