package com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AttendanceRecordJpaRepository extends JpaRepository<AttendanceRecordEntity, UUID>,
        JpaSpecificationExecutor<AttendanceRecordEntity> {
    Optional<AttendanceRecordEntity> findByEmployeeIdAndWorkDate(UUID employeeId, LocalDate workDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AttendanceRecordEntity r where r.employeeId = :employeeId and r.workDate = :workDate")
    Optional<AttendanceRecordEntity> lockByEmployeeAndDate(@Param("employeeId") UUID employeeId,
            @Param("workDate") LocalDate workDate);
}
