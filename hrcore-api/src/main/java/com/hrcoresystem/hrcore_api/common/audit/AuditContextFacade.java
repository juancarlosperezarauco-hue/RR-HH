package com.hrcoresystem.hrcore_api.common.audit;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

@Component
public class AuditContextFacade {

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";

    private final SecurityContextFacade securityContextFacade;

    public AuditContextFacade(SecurityContextFacade securityContextFacade) {
        this.securityContextFacade = securityContextFacade;
    }

    public AuditContext resolve() {
        String actorSubject = resolveActorSubject();
        String actorEmail = resolveActorEmail();
        String tenantSlug = securityContextFacade.getCurrentTenantSlug();
        String requestId = resolveRequestId();
        String correlationId = resolveCorrelationId(requestId);

        return new AuditContext(
                resolveActorId(actorSubject),
                actorSubject,
                actorEmail,
                tenantSlug,
                resolveIpAddress(),
                resolveUserAgent(),
                requestId,
                correlationId,
                "APPLICATION",
                "SUCCESS"
        );
    }

    private UUID resolveActorId(String actorSubject) {
        if (actorSubject == null || actorSubject.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(actorSubject.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String resolveActorSubject() {
        String subject = securityContextFacade.getCurrentSubject();
        return (subject == null || subject.isBlank()) ? "SYSTEM" : subject.trim();
    }

    private String resolveActorEmail() {
        String email = securityContextFacade.getCurrentEmail();
        return (email == null || email.isBlank()) ? null : email.trim();
    }

    private String resolveRequestId() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }

        return normalize(request.getHeader(HEADER_REQUEST_ID), request.getHeader(HEADER_CORRELATION_ID));
    }

    private String resolveCorrelationId(String requestId) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return requestId;
        }

        return normalize(request.getHeader(HEADER_CORRELATION_ID), requestId);
    }

    private InetAddress resolveIpAddress() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }

        String forwardedFor = normalize(request.getHeader(HEADER_FORWARDED_FOR), null);
        if (forwardedFor != null) {
            int separatorIndex = forwardedFor.indexOf(',');
            return toInetAddress(separatorIndex >= 0 ? forwardedFor.substring(0, separatorIndex).trim() : forwardedFor);
        }

        String remoteAddress = normalize(request.getRemoteAddr(), null);
        return toInetAddress(remoteAddress);
    }

    private String resolveUserAgent() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }

        return normalize(request.getHeader("User-Agent"), null);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }

        return servletRequestAttributes.getRequest();
    }

    private String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private InetAddress toInetAddress(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return InetAddress.getByName(value.trim());
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    public record AuditContext(
            UUID actorId,
            String actorSubject,
            String actorEmail,
            String tenantSlug,
            InetAddress ipAddress,
            String userAgent,
            String requestId,
            String correlationId,
            String source,
            String outcome
    ) {
    }
}
