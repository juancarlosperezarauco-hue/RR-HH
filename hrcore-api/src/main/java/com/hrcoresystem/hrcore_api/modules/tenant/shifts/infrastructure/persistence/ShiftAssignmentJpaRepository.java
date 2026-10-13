package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ShiftAssignmentJpaRepository extends JpaRepository<ShiftAssignmentEntity, UUID>,
        JpaSpecificationExecutor<ShiftAssignmentEntity> {
    boolean existsByShiftId(UUID shiftId);

    @Query("SELECT a.employeeId FROM ShiftAssignmentEntity a WHERE a.id = :id")
    java.util.Optional<UUID> findEmployeeId(@Param("id") UUID id);

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
              FROM ShiftAssignmentEntity a
             WHERE a.shiftId = :shiftId AND a.status = :status
               AND (a.endDate IS NULL OR a.endDate >= :today)
            """)
    boolean existsActiveCurrentOrFutureByShiftId(@Param("shiftId") UUID shiftId,
            @Param("today") LocalDate today, @Param("status") ShiftAssignmentStatus status);

    @Query(value = """
            SELECT * FROM employee_shift_assignments
             WHERE employee_id = :employeeId
               AND status <> 'CANCELLED'
               AND start_date <= :date
               AND (end_date IS NULL OR end_date >= :date)
               AND CAST(:weekday AS SMALLINT) = ANY(weekdays)
             ORDER BY start_date, id
            """, nativeQuery = true)
    List<ShiftAssignmentEntity> findEffectiveOn(@Param("employeeId") UUID employeeId,
            @Param("date") LocalDate date, @Param("weekday") Short weekday);
}
