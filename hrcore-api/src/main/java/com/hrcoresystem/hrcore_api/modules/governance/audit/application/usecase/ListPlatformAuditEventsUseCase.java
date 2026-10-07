package com.hrcoresystem.hrcore_api.modules.governance.audit.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto.AuditEventResponse;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.mapper.AuditEventMapper;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.repository.PlatformAuditEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListPlatformAuditEventsUseCase {

    private final PlatformAuditEventRepository platformAuditEventRepository;
    private final AuditEventMapper auditEventMapper;

    public ListPlatformAuditEventsUseCase(
            PlatformAuditEventRepository platformAuditEventRepository,
            AuditEventMapper auditEventMapper
    ) {
        this.platformAuditEventRepository = platformAuditEventRepository;
        this.auditEventMapper = auditEventMapper;
    }

    public List<AuditEventResponse> execute() {
        return platformAuditEventRepository.findAll()
                .stream()
                .map(auditEventMapper::toResponse)
                .toList();
    }

    public List<AuditEventResponse> execute(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);

        return platformAuditEventRepository.findRecent(safeLimit)
                .stream()
                .map(auditEventMapper::toResponse)
                .toList();
    }
}
