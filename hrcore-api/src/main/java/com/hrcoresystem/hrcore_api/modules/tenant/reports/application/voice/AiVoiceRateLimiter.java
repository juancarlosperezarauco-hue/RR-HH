package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.voice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Per-process limits. A shared gateway or store is needed before horizontal scaling. */
@Component
public final class AiVoiceRateLimiter {
    private static final long WINDOW = 60_000_000_000L;
    private final AiVoiceProperties properties;
    private final Map<String, Counter> users = new HashMap<>();
    private final Map<String, Counter> tenants = new HashMap<>();
    private final Counter global = new Counter();

    public AiVoiceRateLimiter(AiVoiceProperties properties) { this.properties = properties; }

    public synchronized Lease acquire(String tenant, UUID user) {
        long now = System.nanoTime();
        users.values().removeIf(counter -> counter.active == 0 && now - counter.start >= WINDOW);
        tenants.values().removeIf(counter -> counter.active == 0 && now - counter.start >= WINDOW);
        String userKey = tenant + ":" + user;
        Counter userCounter = users.computeIfAbsent(userKey, ignored -> new Counter());
        Counter tenantCounter = tenants.computeIfAbsent(tenant, ignored -> new Counter());
        refresh(userCounter, now); refresh(tenantCounter, now); refresh(global, now);
        if (userCounter.requests >= properties.getUserRpm()
                || tenantCounter.requests >= properties.getTenantRpm()
                || global.requests >= properties.getGlobalRpm()
                || userCounter.active >= properties.getUserConcurrent()
                || tenantCounter.active >= properties.getTenantConcurrent()
                || global.active >= properties.getMaxConcurrent())
            throw new AiVoiceException(AiVoiceException.Code.BUSY);
        userCounter.requests++; tenantCounter.requests++; global.requests++;
        userCounter.active++; tenantCounter.active++; global.active++;
        return () -> release(userCounter, tenantCounter);
    }

    private synchronized void release(Counter user, Counter tenant) {
        user.active--; tenant.active--; global.active--;
    }

    private static void refresh(Counter counter, long now) {
        if (counter.start == 0 || now - counter.start >= WINDOW) {
            counter.start = now;
            counter.requests = 0;
        }
    }

    public interface Lease extends AutoCloseable { @Override void close(); }
    private static final class Counter { long start; int requests; int active; }
}
