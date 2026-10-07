package com.hrcoresystem.hrcore_api.common.tenancy.migration;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

@Service
public class TenantSchemaMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(TenantSchemaMigrationService.class);

    private final DataSource targetDataSource;
    private final JdbcTemplate jdbcTemplate;

    public TenantSchemaMigrationService(@Qualifier("targetDataSource") DataSource targetDataSource) {
        this.targetDataSource = targetDataSource;
        this.jdbcTemplate = new JdbcTemplate(targetDataSource);
    }

    public void migrateRegisteredTenantSchemas() {
        List<String> schemaNames = jdbcTemplate.queryForList(
                """
                SELECT schema_name
                FROM public.platform_tenants
                WHERE active = true
                ORDER BY created_at ASC
                """,
                String.class
        );

        if (schemaNames.isEmpty()) {
            logger.info("No registered tenant schemas found to migrate.");
            return;
        }

        for (String schemaName : schemaNames) {
            migrateSchema(schemaName);
        }
    }

    public void migrateSchema(String schemaName) {
        validateSchemaName(schemaName);

        logger.info("Running tenant Flyway migrations for schema '{}'.", schemaName);

        Flyway flyway = Flyway.configure()
                .dataSource(targetDataSource)
                .locations("classpath:db/migration/tenant")
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .baselineOnMigrate(true)
                .createSchemas(true)
                .load();

        flyway.migrate();

        logger.info("Tenant Flyway migrations completed for schema '{}'.", schemaName);
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException("Schema name must not be blank");
        }

        if (!schemaName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Schema name contains invalid characters: " + schemaName);
        }
    }
}