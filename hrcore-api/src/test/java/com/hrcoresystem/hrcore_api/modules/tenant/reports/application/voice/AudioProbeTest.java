package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.voice;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AudioProbeTest {
    private final AiVoiceProperties properties = new AiVoiceProperties();
    private final AudioProbe probe = new AudioProbe(properties, new ObjectMapper());

    private byte[] result(String format, String codec, String lastTime) {
        return ("{\"format\":{\"format_name\":\"" + format + "\"},"
                + "\"streams\":[{\"codec_type\":\"audio\",\"codec_name\":\"" + codec + "\"}],"
                + "\"packets\":[{\"pts_time\":\"0\",\"duration_time\":\"0.02\"},"
                + "{\"pts_time\":\"" + lastTime + "\",\"duration_time\":\"0.02\"}]}")
                .getBytes(StandardCharsets.UTF_8);
    }

    @Test void acceptsSupportedContainersAndRejectsDurationAndCodec() {
        assertEquals("webm", probe.validateProbe("webm", result("matroska,webm", "opus", "29.98")));
        assertEquals("ogg", probe.validateProbe("ogg", result("ogg", "opus", "29.98")));
        assertEquals("mp4", probe.validateProbe("mp4", result("mov,mp4,m4a,3gp", "aac", "29.98")));
        assertEquals(AiVoiceException.Code.TOO_LONG, assertThrows(AiVoiceException.class,
                () -> probe.validateProbe("webm", result("matroska,webm", "opus", "30"))).code());
        assertEquals(AiVoiceException.Code.TOO_LONG, assertThrows(AiVoiceException.class,
                () -> probe.validateProbe("webm", result("matroska,webm", "opus", "31"))).code());
        assertEquals(AiVoiceException.Code.INVALID_AUDIO, assertThrows(AiVoiceException.class,
                () -> probe.validateProbe("webm", result("matroska,webm", "aac", "10"))).code());
        assertEquals(AiVoiceException.Code.INVALID_AUDIO, assertThrows(AiVoiceException.class,
                () -> probe.validateProbe("webm", "not json".getBytes(StandardCharsets.UTF_8))).code());
    }

    @Test void rejectsMismatchedOrMalformedSignatureBeforeStartingProbe() throws Exception {
        Path path = Files.createTempFile("voice-test-", ".bin");
        try {
            Files.write(path, "not an audio file".getBytes(StandardCharsets.UTF_8));
            assertEquals(AiVoiceException.Code.INVALID_AUDIO, assertThrows(AiVoiceException.class,
                    () -> probe.inspect(path, "audio/webm")).code());
            Files.write(path, new byte[] {0x1a, 0x45, (byte) 0xdf, (byte) 0xa3, 0, 0, 0, 0, 0, 0, 0, 0});
            assertEquals(AiVoiceException.Code.INVALID_AUDIO, assertThrows(AiVoiceException.class,
                    () -> probe.inspect(path, "audio/mp4")).code());
        } finally { Files.deleteIfExists(path); }
    }
}
