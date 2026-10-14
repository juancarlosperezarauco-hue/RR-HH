package com.hrcoresystem.hrcore_api.modules.identity.access.application.service;

import com.hrcoresystem.hrcore_api.modules.identity.access.application.catalog.PermissionCatalogRegistry;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.PermissionDefinition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Service
public class PermissionCatalogSynchronizer {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final TransactionTemplate tx;

    public PermissionCatalogSynchronizer(@Qualifier("targetDataSource") DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.namedJdbc = new NamedParameterJdbcTemplate(dataSource);
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    public void synchronize() {
        tx.executeWithoutResult(status -> {
            int changes = 0;
            for (PermissionDefinition permission : PermissionCatalogRegistry.definitions()) {
                changes += jdbc.update("""
                        INSERT INTO public.system_permissions(code, module, description, scope, custom_role_assignable, active)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT(code) DO UPDATE SET
                            module = EXCLUDED.module,
                            description = EXCLUDED.description,
                            scope = EXCLUDED.scope,
                            custom_role_assignable = EXCLUDED.custom_role_assignable,
                            active = EXCLUDED.active
                        WHERE public.system_permissions.module IS DISTINCT FROM EXCLUDED.module
                           OR public.system_permissions.description IS DISTINCT FROM EXCLUDED.description
                           OR public.system_permissions.scope IS DISTINCT FROM EXCLUDED.scope
                           OR public.system_permissions.custom_role_assignable IS DISTINCT FROM EXCLUDED.custom_role_assignable
                           OR public.system_permissions.active IS DISTINCT FROM EXCLUDED.active
                        """, permission.code(), permission.module(), permission.description(), permission.scope().name(), permission.customRoleAssignable(), permission.active());
            }
            var codes = PermissionCatalogRegistry.definitions().stream().map(PermissionDefinition::code).toList();
            if (!codes.isEmpty()) {
                changes += namedJdbc.update("UPDATE public.system_permissions SET active = false WHERE active = true AND code NOT IN (:registeredCodes)",
                        new MapSqlParameterSource("registeredCodes", codes));
            }
            if (changes > 0 && jdbc.update("UPDATE public.authorization_catalog_state SET catalog_version = catalog_version + 1, updated_at = NOW() WHERE id = 1") != 1) {
                throw new IllegalStateException("Authorization catalog state is missing");
            }
        });
    }
}
