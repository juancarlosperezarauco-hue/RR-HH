package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.export;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties("app.ai.reports.export")
@Validated
@Getter
@Setter
public class AiExportProperties {
    @Min(1) @Max(100000) private int tabularMaxRows = 10000;
    @Min(1024) @Max(104857600) private long tabularMaxBytes = 25 * 1024 * 1024;
    @Min(1) @Max(2000) private int pdfMaxRows = 500;
    @Min(1) @Max(8) private int pdfMaxColumns = 8;
    @Min(1024) @Max(20971520) private long pdfMaxBytes = 10 * 1024 * 1024;
    @Min(1) @Max(120) private int timeoutSeconds = 30;
    @Min(1) @Max(8) private int maxConcurrent = 2;
}
