package com.hrcoresystem.hrcore_api.modules.platform.superadmin.application.mapper;

import com.hrcoresystem.hrcore_api.modules.platform.superadmin.application.dto.PlatformSuperadminResponse;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.model.PlatformSuperadmin;
import org.springframework.stereotype.Component;

@Component
public class PlatformSuperadminMapper {

    public PlatformSuperadminResponse toResponse(PlatformSuperadmin superadmin) {
        return new PlatformSuperadminResponse(
                superadmin.id(),
                superadmin.email(),
                superadmin.firstName(),
                superadmin.lastName(),
                superadmin.active(),
                superadmin.createdAt(),
                superadmin.updatedAt()
        );
    }
}