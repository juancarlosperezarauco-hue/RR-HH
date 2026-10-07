package com.hrcoresystem.hrcore_api.modules.identity.access.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRolePermissionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class TenantRolePermissionRepositoryAdapter implements TenantRolePermissionRepository {

    private final JdbcTemplate jdbcTemplate;

    public TenantRolePermissionRepositoryAdapter(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Set<String> findPermissionCodesByRoleId(UUID roleId) {
        List<String> codes = jdbcTemplate.queryForList(
                """
                SELECT permission_code
                FROM tenant_role_permissions
                WHERE role_id = ?
                ORDER BY permission_code ASC
                """,
                String.class,
                roleId
        );

        return new LinkedHashSet<>(codes);
    }

    @Override
    public void replacePermissions(UUID roleId, List<String> permissionCodes) {
        jdbcTemplate.update("DELETE FROM tenant_role_permissions WHERE role_id = ?", roleId);

        for (String permissionCode : permissionCodes) {
            jdbcTemplate.update(
                    """
                    INSERT INTO tenant_role_permissions (role_id, permission_code, assigned_at)
                    VALUES (?, ?, NOW())
                    """,
                    roleId,
                    permissionCode
            );
        }
    }
}