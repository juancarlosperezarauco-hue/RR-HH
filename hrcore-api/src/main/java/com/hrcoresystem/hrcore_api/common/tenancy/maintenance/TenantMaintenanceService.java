package com.hrcoresystem.hrcore_api.common.tenancy.maintenance;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Optional;

@Service
public class TenantMaintenanceService {

    private final JdbcTemplate jdbcTemplate;

    public TenantMaintenanceService(@Qualifier("targetDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public boolean isTenantInMaintenance(String tenantSlug) {
        Boolean result = jdbcTemplate.queryForObject(
                """
                SELECT maintenance_mode
                FROM public.platform_tenants
                WHERE slug = ?
                """,
                Boolean.class,
                tenantSlug
        );
        return Boolean.TRUE.equals(result);
    }

    public Optional<String> getMaintenanceReason(String tenantSlug) {
        return jdbcTemplate.query(
                """
                SELECT maintenance_reason
                FROM public.platform_tenants
                WHERE slug = ?
                """,
                rs -> rs.next() ? Optional.ofNullable(rs.getString("maintenance_reason")) : Optional.empty(),
                tenantSlug
        );
    }

    public void enableMaintenance(String tenantSlug, String reason) {
        jdbcTemplate.update(
                """
                UPDATE public.platform_tenants
                SET maintenance_mode = true,
                    maintenance_reason = ?,
                    maintenance_started_at = NOW(),
                    updated_at = NOW()
                WHERE slug = ?
                """,
                reason,
                tenantSlug
        );
    }

    public void disableMaintenance(String tenantSlug) {
        jdbcTemplate.update(
                """
                UPDATE public.platform_tenants
                SET maintenance_mode = false,
                    maintenance_reason = NULL,
                    maintenance_started_at = NULL,
                    updated_at = NOW()
                WHERE slug = ?
                """,
                tenantSlug
        );
    }
}
