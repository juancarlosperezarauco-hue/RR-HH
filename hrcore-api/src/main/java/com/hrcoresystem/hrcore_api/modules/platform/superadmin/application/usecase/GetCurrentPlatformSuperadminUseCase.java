package com.hrcoresystem.hrcore_api.modules.platform.superadmin.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.application.dto.PlatformSuperadminResponse;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.application.mapper.PlatformSuperadminMapper;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.exception.PlatformSuperadminNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.repository.PlatformSuperadminRepository;
import org.springframework.stereotype.Service;

@Service
public class GetCurrentPlatformSuperadminUseCase {

    private final SecurityContextFacade securityContextFacade;
    private final PlatformSuperadminRepository platformSuperadminRepository;
    private final PlatformSuperadminMapper platformSuperadminMapper;

    public GetCurrentPlatformSuperadminUseCase(
            SecurityContextFacade securityContextFacade,
            PlatformSuperadminRepository platformSuperadminRepository,
            PlatformSuperadminMapper platformSuperadminMapper
    ) {
        this.securityContextFacade = securityContextFacade;
        this.platformSuperadminRepository = platformSuperadminRepository;
        this.platformSuperadminMapper = platformSuperadminMapper;
    }

    public PlatformSuperadminResponse execute() {
        String currentSubject = securityContextFacade.getCurrentSubject();

        return platformSuperadminRepository.findByEmail(currentSubject)
                .map(platformSuperadminMapper::toResponse)
                .orElseThrow(() -> new PlatformSuperadminNotFoundException(
                        "Platform superadmin not found for current subject: " + currentSubject
                ));
    }
}