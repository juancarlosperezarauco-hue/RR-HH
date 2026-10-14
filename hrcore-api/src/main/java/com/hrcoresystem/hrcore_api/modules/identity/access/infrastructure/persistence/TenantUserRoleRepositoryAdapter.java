package com.hrcoresystem.hrcore_api.modules.identity.access.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantUserRoleRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

@Repository
public class TenantUserRoleRepositoryAdapter implements TenantUserRoleRepository {

    private final JdbcTemplate jdbcTemplate;

    public TenantUserRoleRepositoryAdapter(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public List<UUID> findRoleIdsByUserId(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT role_id
                FROM tenant_user_roles
                WHERE user_id = ?
                ORDER BY assigned_at ASC
                """,
                (rs, rowNum) -> rs.getObject("role_id", UUID.class),
                userId
        );
    }

    @Override
    public List<String> findRoleNamesByUserId(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT r.name
                FROM tenant_user_roles ur
                JOIN tenant_roles r ON r.id = ur.role_id
                WHERE ur.user_id = ?
                  AND r.active = true
                ORDER BY r.name ASC
                """,
                (rs, rowNum) -> rs.getString("name"),
                userId
        );
    }

    @Override
    public List<String> findRoleCodesByUserId(UUID userId) {
        return jdbcTemplate.query("""
                SELECT DISTINCT r.code
                FROM tenant_user_roles ur JOIN tenant_roles r ON r.id = ur.role_id
                WHERE ur.user_id = ? AND r.active = TRUE ORDER BY r.code ASC
                """, (rs, rowNum) -> rs.getString("code"), userId);
    }

    @Override
    public List<String> findPermissionCodesByUserId(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT DISTINCT rp.permission_code
                FROM tenant_user_roles ur
                JOIN tenant_roles r ON r.id = ur.role_id
                JOIN tenant_role_permissions rp ON rp.role_id = r.id
                JOIN public.system_permissions sp ON sp.code = rp.permission_code
                WHERE ur.user_id = ?
                  AND r.active = true
                  AND sp.active = true
                  AND sp.scope = 'TENANT'
                  AND rp.permission_code IS NOT NULL
                  AND rp.permission_code <> ''
                ORDER BY rp.permission_code ASC
                """,
                (rs, rowNum) -> rs.getString("permission_code"),
                userId
        );
    }

    @Override
    public boolean replaceUserRoles(UUID userId, List<UUID> roleIds) {
        List<UUID> current=findRoleIdsByUserId(userId); java.util.Set<UUID> oldSet=new java.util.HashSet<>(current), newSet=new java.util.HashSet<>(roleIds); boolean changed=!oldSet.equals(newSet);
        for(UUID id:oldSet) if(!newSet.contains(id)) jdbcTemplate.update("DELETE FROM tenant_user_roles WHERE user_id=? AND role_id=?",userId,id);
        for(UUID id:newSet) if(!oldSet.contains(id)) jdbcTemplate.update("INSERT INTO tenant_user_roles(user_id,role_id,assigned_at) VALUES(?,?,NOW())",userId,id);
        return changed;
    }
}
