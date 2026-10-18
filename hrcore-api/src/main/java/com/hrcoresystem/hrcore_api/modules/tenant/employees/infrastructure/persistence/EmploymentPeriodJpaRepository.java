package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface EmploymentPeriodJpaRepository extends JpaRepository<EmploymentPeriodEntity, UUID> {
    Optional<EmploymentPeriodEntity> findByIdAndEmployeeId(UUID id, UUID employeeId);
    Optional<EmploymentPeriodEntity> findFirstByEmployeeIdOrderByPeriodNumberDesc(UUID employeeId);
    Optional<EmploymentPeriodEntity> findFirstByEmployeeIdAndEndDateIsNullOrderByPeriodNumberDesc(UUID employeeId);
    @Query("select p from EmploymentPeriodEntity p where p.employeeId = :employeeId and p.startDate <= :date and (p.endDate is null or p.endDate >= :date) order by p.periodNumber desc")
    List<EmploymentPeriodEntity> findByEmployeeIdAndDate(@Param("employeeId") UUID employeeId, @Param("date") LocalDate date);
    List<EmploymentPeriodEntity> findAllByEmployeeIdOrderByPeriodNumberDesc(UUID employeeId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from EmploymentPeriodEntity p where p.employeeId = :employeeId and p.endDate is null")
    Optional<EmploymentPeriodEntity> findOpenForUpdate(@Param("employeeId") UUID employeeId);
}
