package com.hrcoresystem.hrcore_api.modules.identity.users.application.mapper;

import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.TenantUserResponse;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import org.springframework.stereotype.Component;

@Component
public class TenantUserMapper {

    public TenantUserResponse toResponse(TenantUser tenantUser) {
        return new TenantUserResponse(
                tenantUser.id(),
                tenantUser.email(),
                tenantUser.firstName(),
                tenantUser.lastName(),
                tenantUser.active(),
                tenantUser.status().name(),
                tenantUser.createdAt(),
                tenantUser.updatedAt()
        );
    }
}