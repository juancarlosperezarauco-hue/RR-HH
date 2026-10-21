package com.hrcoresystem.hrcore_api.modules.tenant.reports.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.voice.AiVoiceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/hr/ai/transcriptions")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyAuthority('hr.reports.employees.read','hr.reports.attendance.read')")
public class AiVoiceController {
    private final AiVoiceService voice;
    public AiVoiceController(AiVoiceService voice) { this.voice = voice; }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<AiVoiceService.Config>> config() {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(ApiResponse.success("Configuración de voz", voice.config()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AiVoiceService.Result>> transcribe(
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(ApiResponse.success("Audio transcrito", voice.transcribe(file)));
    }
}
