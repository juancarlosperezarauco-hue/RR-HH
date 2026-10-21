package com.hrcoresystem.hrcore_api.modules.identity.access.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRoleType;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TenantRoleRepositoryAdapter implements TenantRoleRepository {

    private final TenantRoleJpaRepository jpaRepository;
    private final JdbcTemplate jdbc;

    public TenantRoleRepositoryAdapter(TenantRoleJpaRepository jpaRepository, DataSource dataSource) {
        this.jpaRepository = jpaRepository;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public TenantRole save(TenantRole role) {
        if (role.id() == null) {
            if (role.roleType() != TenantRoleType.INTERNAL || role.systemRole()) {
                throw new IllegalArgumentException("Only internal roles can be created here");
            }
            return jdbc.queryForObject("""
                    INSERT INTO tenant_roles(name, description, role_type, system_role, active)
                    VALUES (?, ?, 'INTERNAL', FALSE, TRUE)
                    RETURNING id, code, name, description, role_type, system_role,
                              active, created_at, updated_at
                    """, (rs, row) -> fromRow(rs), role.name(), role.description());
        }

        TenantRoleEntity entity = jpaRepository.findById(role.id())
                .orElseThrow(() -> new IllegalArgumentException("Tenant role not found"));
        if (!entity.getCode().equals(role.code())
                || entity.getRoleType() != role.roleType()
                || entity.isSystemRole() != role.systemRole()) {
            throw new IllegalArgumentException("Role identifiers and type are immutable");
        }
        entity.setName(role.name());
        entity.setDescription(role.description());
        entity.setActive(role.active());
        return toDomain(jpaRepository.saveAndFlush(entity));
    }

    @Override public Optional<TenantRole> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }
    @Override public Optional<TenantRole> findByName(String name) {
        return jpaRepository.findByName(name).map(this::toDomain);
    }
    @Override public Optional<TenantRole> findByCode(String code) {
        return jpaRepository.findByCode(code).map(this::toDomain);
    }
    @Override public List<TenantRole> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }
    @Override public List<TenantRole> findAllByIds(List<UUID> ids) {
        return jpaRepository.findAllById(ids).stream().map(this::toDomain).toList();
    }
    @Override public boolean existsByName(String name) { return jpaRepository.existsByName(name); }
    @Override public boolean existsByCode(String code) { return jpaRepository.existsByCode(code); }
    @Override public long countActiveCustomRoles() {
        return jpaRepository.countByActiveTrueAndSystemRoleFalse();
    }

    private TenantRole fromRow(ResultSet rs) throws SQLException {
        return new TenantRole(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("system_role"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                TenantRoleType.valueOf(rs.getString("role_type")));
    }

    private TenantRole toDomain(TenantRoleEntity e) {
        return new TenantRole(
                e.getId(), e.getCode(), e.getName(), e.getDescription(),
                e.isSystemRole(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt(), e.getRoleType());
    }
}
