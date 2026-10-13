package com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.ContractStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ContractStatusRequest(@NotNull ContractStatus status,
                                    LocalDate effectiveDate,
                                    @Size(max = 2000) String reason) { }
