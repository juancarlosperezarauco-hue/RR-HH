package com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model;

import java.time.LocalDate;
import java.util.UUID;

public record ContractFilter(UUID employeeId, ContractType type, ContractStatus status,
                             LocalDate expiringBefore, String search) { }
