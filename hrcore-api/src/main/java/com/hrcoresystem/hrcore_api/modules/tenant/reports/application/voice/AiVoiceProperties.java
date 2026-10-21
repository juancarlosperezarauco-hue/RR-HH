package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.voice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties("app.ai.voice")
@Validated
@Getter
@Setter
public class AiVoiceProperties {
    private boolean enabled = false;
    private String apiKey = "";
    private String model = "whisper-large-v3-turbo";
    @Min(1) @Max(120) private int maxDurationSeconds = 30;
    @Min(1024) @Max(25 * 1024 * 1024) private long maxBytes = 8 * 1024 * 1024;
    @Min(1) @Max(100) private int userRpm = 5;
    @Min(1) @Max(100) private int tenantRpm = 10;
    @Min(1) @Max(100) private int globalRpm = 15;
    @Min(1) @Max(8) private int userConcurrent = 1;
    @Min(1) @Max(8) private int tenantConcurrent = 2;
    @Min(1) @Max(8) private int maxConcurrent = 2;
    @Min(1) @Max(15) private int probeTimeoutSeconds = 3;
    @Min(3) @Max(120) private int providerTimeoutSeconds = 30;
    private String ffprobePath = "ffprobe";

    public boolean available() {
        return enabled && apiKey != null && !apiKey.isBlank()
                && apiKey.chars().allMatch(character -> character >= 33 && character <= 126)
                && model != null && model.matches("[A-Za-z0-9._/-]{1,100}");
    }
}
