package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WorkShiftJpaRepository extends JpaRepository<WorkShiftEntity, UUID>,
        JpaSpecificationExecutor<WorkShiftEntity> {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from WorkShiftEntity s where s.id = :id")
    Optional<WorkShiftEntity> lockById(@Param("id") UUID id);
}
