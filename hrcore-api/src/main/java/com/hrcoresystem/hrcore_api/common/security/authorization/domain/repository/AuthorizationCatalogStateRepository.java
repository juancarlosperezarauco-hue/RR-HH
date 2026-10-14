package com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository;

import com.hrcoresystem.hrcore_api.common.security.authorization.domain.model.AuthorizationCatalogState;
import java.util.Optional;

public interface AuthorizationCatalogStateRepository {
    Optional<AuthorizationCatalogState> find();
    long incrementVersion();
}
