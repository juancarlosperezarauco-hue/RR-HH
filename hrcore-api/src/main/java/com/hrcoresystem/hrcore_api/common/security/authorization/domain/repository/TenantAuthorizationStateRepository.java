package com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository;

import com.hrcoresystem.hrcore_api.common.security.authorization.domain.model.TenantAuthorizationState;
import java.util.Optional;
import java.util.UUID;

public interface TenantAuthorizationStateRepository {
    Optional<TenantAuthorizationState> find();
    Optional<TenantAuthorizationState> findForUpdate();
    long incrementVersion();
    UUID ownerUserId();
}
