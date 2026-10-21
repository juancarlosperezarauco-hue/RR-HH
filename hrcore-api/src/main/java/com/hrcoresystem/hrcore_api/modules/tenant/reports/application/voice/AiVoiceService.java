package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.voice;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AnalyticAuthorizationPolicy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public final class AiVoiceService {
    private static final Logger log = LoggerFactory.getLogger(AiVoiceService.class);
    private final AiVoiceProperties properties;
    private final AnalyticAuthorizationPolicy authorization;
    private final SecurityContextFacade security;
    private final AiVoiceRateLimiter limits;
    private final AudioProbe probe;
    private final TranscriptionProvider provider;

    public AiVoiceService(AiVoiceProperties properties, AnalyticAuthorizationPolicy authorization,
                          SecurityContextFacade security, AiVoiceRateLimiter limits, AudioProbe probe,
                          TranscriptionProvider provider) {
        this.properties = properties; this.authorization = authorization; this.security = security;
        this.limits = limits; this.probe = probe; this.provider = provider;
    }

    public record Config(boolean enabled, int maxDurationSeconds, long maxBytes) {}
    public record Result(String text) {}

    public Config config() {
        requireAccess();
        return new Config(properties.available(), properties.getMaxDurationSeconds(), properties.getMaxBytes());
    }

    public Result transcribe(MultipartFile file) {
        requireAccess();
        if (!properties.available()) throw new AiVoiceException(AiVoiceException.Code.UNAVAILABLE);
        if (file == null || file.isEmpty()) throw new AiVoiceException(AiVoiceException.Code.INVALID_AUDIO);
        if (file.getSize() > properties.getMaxBytes()) throw new AiVoiceException(AiVoiceException.Code.TOO_LARGE);
        try (AiVoiceRateLimiter.Lease ignored = limits.acquire(security.getCurrentTenantSlug(),
                security.requireCurrentUserId())) {
            Path staged = null;
            try {
                staged = Files.createTempFile("hrcore-voice-", ".audio");
                try (var source = file.getInputStream(); var target = Files.newOutputStream(staged)) {
                    byte[] buffer = new byte[8192];
                    long total = 0;
                    for (int count; (count = source.read(buffer)) != -1; ) {
                        total += count;
                        if (total > properties.getMaxBytes()) throw new AiVoiceException(AiVoiceException.Code.TOO_LARGE);
                        target.write(buffer, 0, count);
                    }
                    if (total == 0) throw new AiVoiceException(AiVoiceException.Code.INVALID_AUDIO);
                }
                String extension = probe.inspect(staged, file.getContentType());
                return new Result(provider.transcribe(staged, extension));
            } catch (IOException failure) {
                throw new AiVoiceException(AiVoiceException.Code.UNAVAILABLE);
            } finally {
                if (staged != null) {
                    try { Files.deleteIfExists(staged); }
                    catch (IOException firstFailure) {
                        try { Files.deleteIfExists(staged); }
                        catch (IOException secondFailure) { log.error("Temporary voice audio cleanup failed"); }
                    }
                }
            }
        }
    }

    private void requireAccess() {
        if (authorization.authorizedCatalog().isEmpty())
            throw new AccessDeniedException("Se requiere acceso a reportes analíticos");
    }
}
