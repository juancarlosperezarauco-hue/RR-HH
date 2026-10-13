package com.hrcoresystem.hrcore_api.modules.tenant.departments.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.DepartmentHistory;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public class DepartmentHistoryRepositoryAdapter implements DepartmentHistoryRepository {
    private final DepartmentHistoryJpaRepository jpa;

    public DepartmentHistoryRepositoryAdapter(DepartmentHistoryJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public DepartmentHistory save(DepartmentHistory h) {
        DepartmentHistoryEntity e = new DepartmentHistoryEntity();
        e.setId(h.id());
        e.setDepartmentId(h.departmentId());
        e.setAction(h.action());
        e.setPreviousData(h.previousData());
        e.setNewData(h.newData());
        e.setReason(h.reason());
        e.setPerformedBy(h.performedBy());
        e.setCreatedAt(h.createdAt());
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public Page<DepartmentHistory> findByDepartmentId(UUID id, Pageable pageable) {
        return jpa.findByDepartmentId(id, pageable).map(this::toDomain);
    }

    private DepartmentHistory toDomain(DepartmentHistoryEntity e) {
        return new DepartmentHistory(e.getId(), e.getDepartmentId(), e.getAction(), e.getPreviousData(),
                e.getNewData(), e.getReason(), e.getPerformedBy(), e.getCreatedAt());
    }
}
