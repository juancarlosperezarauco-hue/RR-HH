package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.export;

import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AiExportLimitException;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.QueryPlanException;
import java.io.*;
import java.time.Duration;

/** Bounds the raw data (including SXSSF scratch input), output and total generation time. */
final class ExportBudget {
    private final long maxBytes, started = System.nanoTime(), durationNanos;
    private long rawBytes;
    ExportBudget(long maxBytes, int seconds) {
        this.maxBytes = maxBytes; this.durationNanos = Duration.ofSeconds(seconds).toNanos();
    }
    void check() {
        if (System.nanoTime() - started > durationNanos)
            throw new QueryPlanException(QueryPlanException.Code.QUERY_TIMEOUT);
    }
    long remainingMillis() {
        check();
        return Math.max(1, java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(durationNanos - (System.nanoTime() - started)));
    }
    void input(long bytes) {
        check(); rawBytes += bytes;
        if (rawBytes > maxBytes) throw new AiExportLimitException();
    }
    OutputStream output(OutputStream delegate) {
        return new FilterOutputStream(delegate) {
            private long written;
            private void reserve(int bytes) {
                check(); written += bytes;
                if (written > maxBytes) throw new AiExportLimitException();
            }
            public void write(int value) throws IOException { reserve(1); out.write(value); }
            public void write(byte[] data, int offset, int length) throws IOException {
                reserve(length); out.write(data, offset, length);
            }
        };
    }
}
