package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class AttendanceActor {
    private final SecurityContextFacade security;
    public AttendanceActor(SecurityContextFacade security) { this.security = security; }
    public UUID id() {
        try { return UUID.fromString(security.getCurrentSubject()); }
        catch (RuntimeException ex) { throw new AccessDeniedException("A tenant user identity is required"); }
    }
    public boolean has(String permission) { return security.hasAuthority(permission); }
}
