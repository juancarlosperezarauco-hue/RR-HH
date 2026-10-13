package com.hrcoresystem.hrcore_api.modules.tenant.positions.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.PositionRequirement;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.PositionRequirementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PositionRequirementRepositoryAdapter implements PositionRequirementRepository {
    private final PositionRequirementJpaRepository jpa;

    public PositionRequirementRepositoryAdapter(PositionRequirementJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public PositionRequirement save(PositionRequirement r) {
        PositionRequirementEntity e = r.id() == null ? new PositionRequirementEntity()
                : jpa.findByIdAndPositionId(r.id(), r.positionId()).orElseThrow();
        e.setPositionId(r.positionId()); e.setRequirementType(r.requirementType());
        e.setDescription(r.description()); e.setMandatory(r.mandatory());
        e.setMinimumYearsExperience(r.minimumYearsExperience());
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public Optional<PositionRequirement> findByIdAndPositionId(UUID id, UUID positionId) {
        return jpa.findByIdAndPositionId(id, positionId).map(this::toDomain);
    }

    @Override
    public Page<PositionRequirement> findByPositionId(UUID positionId, Pageable pageable) {
        return jpa.findByPositionId(positionId, pageable).map(this::toDomain);
    }

    @Override
    public void delete(PositionRequirement r) {
        PositionRequirementEntity e = jpa.findByIdAndPositionId(r.id(), r.positionId()).orElseThrow();
        jpa.delete(e);
        jpa.flush();
    }

    private PositionRequirement toDomain(PositionRequirementEntity e) {
        return new PositionRequirement(e.getId(), e.getPositionId(), e.getRequirementType(),
                e.getDescription(), e.isMandatory(), e.getMinimumYearsExperience(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
