package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class EmployeeSupervisionScopeService {
    private static final String ACTIVE_EMPLOYEE_BY_USER_SQL = """
            SELECT e.id
            FROM employees e
            JOIN tenant_users u ON u.id = e.tenant_user_id
            WHERE u.id = ?
              AND e.employment_status = 'ACTIVE'
              AND u.active = true
              AND u.status = 'ACTIVE'
            """;
    private final JdbcTemplate jdbc;
    private final SecurityContextFacade security;
    private final OrganizationalReportingService reporting;

    public EmployeeSupervisionScopeService(DataSource dataSource, SecurityContextFacade security,
            OrganizationalReportingService reporting) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.security = security;
        this.reporting = reporting;
    }

    public UUID resolveEffectiveSupervisorEmployeeId(UUID employeeId, LocalDate date) {
        return reporting.resolveEffectiveSupervisor(employeeId, date);
    }

    public boolean canSupervise(UUID authenticatedUserId, UUID targetEmployeeId, LocalDate date) {
        UUID currentEmployeeId = findActiveEmployeeIdByUser(authenticatedUserId);
        if (currentEmployeeId == null) return false;
        UUID supervisorId = resolveEffectiveSupervisorEmployeeId(targetEmployeeId, date);
        return currentEmployeeId.equals(supervisorId);
    }

    public UUID requireCurrentEmployeeId() {
        UUID currentUserId = security.getCurrentUserId();
        UUID employeeId = findActiveEmployeeIdByUser(currentUserId);
        if (employeeId == null) throw new BusinessException("La cuenta no tiene un empleado activo asociado");
        return employeeId;
    }

    private UUID findActiveEmployeeIdByUser(UUID userId) {
        return jdbc.query(ACTIVE_EMPLOYEE_BY_USER_SQL, rs -> rs.next() ? (UUID) rs.getObject(1) : null, userId);
    }
}
