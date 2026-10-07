package com.hrcoresystem.hrcore_api.modules.platform.tenants.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformPlanNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

@Service
public class PlatformPlanLookupService {

    private final JdbcTemplate jdbcTemplate;

    public PlatformPlanLookupService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public UUID findPlanIdByCodeOrDefault(String rawPlanCode) {
        String planCode = (rawPlanCode == null || rawPlanCode.isBlank())
                ? "BASIC"
                : rawPlanCode.trim().toUpperCase();

        List<UUID> ids = jdbcTemplate.query(
                """
                SELECT id
                FROM platform_plans
                WHERE code = ?
                  AND active = true
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                planCode
        );

        if (ids.isEmpty()) {
            throw new PlatformPlanNotFoundException("Platform plan not found for code: " + planCode);
        }

        return ids.getFirst();
    }
}