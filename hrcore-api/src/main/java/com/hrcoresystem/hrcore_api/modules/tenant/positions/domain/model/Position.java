package com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Position(UUID id, String code, String name, String description, String responsibilities,
                       OrganizationalLevel organizationalLevel, BigDecimal salaryMin, BigDecimal salaryMax,
                       String currency, PositionStatus status, UUID createdBy, UUID updatedBy,
                       Instant createdAt, Instant updatedAt) { }
