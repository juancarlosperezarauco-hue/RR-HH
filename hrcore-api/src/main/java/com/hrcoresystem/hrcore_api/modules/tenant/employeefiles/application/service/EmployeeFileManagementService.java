package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.service;

import com.hrcoresystem.hrcore_api.common.exception.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.EmployeeFileRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeFileManagementService {
  private final EmployeeFileRepository files;
  private final EmployeeRepository employees;
  private final EmployeeFilePolicy policy;

  public EmployeeFileManagementService(
      EmployeeFileRepository files, EmployeeRepository employees, EmployeeFilePolicy policy) {
    this.files = files;
    this.employees = employees;
    this.policy = policy;
  }

  @Transactional
  public FileResponse create(CreateFileRequest r) {
    policy.operational();
    if (employees.findById(r.employeeId()).isEmpty())
      throw new ResourceNotFoundException("Employee not found");
    if (files.findByEmployeeId(r.employeeId()).isPresent())
      throw new BusinessException("Employee already has a file");
    String number = policy.required(r.fileNumber(), 80, "fileNumber");
    if (files.existsByFileNumberOtherThan(number, null))
      throw new BusinessException("File number already exists");
    var f =
        files.save(
            new EmployeeFile(
                UUID.randomUUID(),
                r.employeeId(),
                number,
                FileStatus.ACTIVE,
                r.notes(),
                policy.actor(),
                null,
                null));
    policy.audit(
        "EMPLOYEE_FILE_CREATED", "EMPLOYEE_FILE", f.id(), Map.of("employeeId", f.employeeId()));
    return map(f);
  }

  @Transactional(readOnly = true)
  public Page<FileResponse> list(EmployeeFileFilter filter, Pageable pageable) {
    policy.operational();
    return files.findAll(filter, pageable).map(this::map);
  }

  @Transactional(readOnly = true)
  public FileResponse get(UUID id) {
    policy.operational();
    return map(policy.file(id));
  }

  @Transactional(readOnly = true)
  public FileResponse byEmployee(UUID employeeId) {
    policy.operational();
    return map(
        files
            .findByEmployeeId(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee file not found")));
  }

  @Transactional
  public FileResponse update(UUID id, UpdateFileRequest r) {
    policy.operational();
    var before = policy.lockedFile(id);
    String number = policy.required(r.fileNumber(), 80, "fileNumber");
    if (files.existsByFileNumberOtherThan(number, id))
      throw new BusinessException("File number already exists");
    String notes = r.notes();
    if (before.status() == FileStatus.ARCHIVED
        && (!before.fileNumber().equals(number) || !Objects.equals(before.notes(), notes)))
      throw new BusinessException("Archived file can only be reactivated");
    var changed = new ArrayList<String>();
    if (!before.fileNumber().equals(number)) changed.add("fileNumber");
    if (!Objects.equals(before.notes(), notes)) changed.add("notes");
    if (before.status() != r.status()) changed.add("status");
    if (changed.isEmpty()) return map(before);
    var saved =
        files.save(
            new EmployeeFile(
                id,
                before.employeeId(),
                number,
                r.status(),
                notes,
                before.createdBy(),
                before.createdAt(),
                before.updatedAt()));
    policy.audit("EMPLOYEE_FILE_UPDATED", "EMPLOYEE_FILE", id, Map.of("changedFields", changed));
    return map(saved);
  }

  private FileResponse map(EmployeeFile f) {
    return new FileResponse(
        f.id(),
        f.employeeId(),
        f.fileNumber(),
        f.status(),
        f.notes(),
        f.createdAt(),
        f.updatedAt());
  }
}
