package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.voice;

import java.nio.file.Path;

public interface TranscriptionProvider {
    String transcribe(Path audio, String extension);
}
