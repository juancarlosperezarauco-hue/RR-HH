package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingsDependencyRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TenantSettingsDependencyRepositoryAdapter implements TenantSettingsDependencyRepository {
    private final JdbcTemplate jdbc;
    public TenantSettingsDependencyRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public boolean hasOperationalDateDependencies() {
        Boolean result = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM work_shifts) OR EXISTS (SELECT 1 FROM employee_shift_assignments) OR EXISTS (SELECT 1 FROM attendance_records) OR EXISTS (SELECT 1 FROM attendance_punches) OR EXISTS (SELECT 1 FROM leave_requests)", Boolean.class);
        return Boolean.TRUE.equals(result);
    }
    @Override public boolean hasContracts() {
        Boolean result = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM employment_contracts)", Boolean.class);
        return Boolean.TRUE.equals(result);
    }
}
