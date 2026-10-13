package com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ContractHistory(UUID id, UUID contractId, ContractHistoryAction action,
                              ContractStatus previousStatus, ContractStatus newStatus,
                              Map<String, Object> details, String reason,
                              UUID performedBy, Instant createdAt) { }
