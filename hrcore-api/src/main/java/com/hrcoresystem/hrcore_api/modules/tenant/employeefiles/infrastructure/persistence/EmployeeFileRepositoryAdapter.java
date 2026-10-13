package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.EmployeeFileRepository;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeFileRepositoryAdapter implements EmployeeFileRepository {
  private final EmployeeFileJpaRepository jpa;

  public EmployeeFileRepositoryAdapter(EmployeeFileJpaRepository jpa) {
    this.jpa = jpa;
  }

  public EmployeeFile save(EmployeeFile f) {
    var e = jpa.findById(f.id()).orElseGet(EmployeeFileEntity::new);
    e.setId(f.id());
    e.setEmployeeId(f.employeeId());
    e.setFileNumber(f.fileNumber());
    e.setStatus(f.status());
    e.setNotes(f.notes());
    e.setCreatedBy(f.createdBy());
    e.setCreatedAt(f.createdAt());
    return map(jpa.saveAndFlush(e));
  }

  public Optional<EmployeeFile> findById(UUID id) {
    return jpa.findById(id).map(this::map);
  }

  public Optional<EmployeeFile> findByIdForUpdate(UUID id) {
    return jpa.findByIdForUpdate(id).map(this::map);
  }

  public Optional<EmployeeFile> findByEmployeeId(UUID id) {
    return jpa.findByEmployeeId(id).map(this::map);
  }

  public boolean existsByFileNumberOtherThan(String number, UUID id) {
    return id == null ? jpa.existsByFileNumber(number) : jpa.existsByFileNumberAndIdNot(number, id);
  }

  public Page<EmployeeFile> findAll(EmployeeFileFilter filter, Pageable pageable) {
    Specification<EmployeeFileEntity> spec =
        (root, q, cb) -> {
          var p = new ArrayList<jakarta.persistence.criteria.Predicate>();
          if (filter.employeeId() != null)
            p.add(cb.equal(root.get("employeeId"), filter.employeeId()));
          if (filter.status() != null) p.add(cb.equal(root.get("status"), filter.status()));
          if (filter.fileNumber() != null && !filter.fileNumber().isBlank())
            p.add(
                cb.like(
                    cb.lower(root.get("fileNumber")),
                    "%" + escape(filter.fileNumber().trim().toLowerCase()) + "%",
                    '\\'));
          return cb.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    return jpa.findAll(spec, pageable).map(this::map);
  }

  private String escape(String s) {
    return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private EmployeeFile map(EmployeeFileEntity e) {
    return new EmployeeFile(
        e.getId(),
        e.getEmployeeId(),
        e.getFileNumber(),
        e.getStatus(),
        e.getNotes(),
        e.getCreatedBy(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }
}
