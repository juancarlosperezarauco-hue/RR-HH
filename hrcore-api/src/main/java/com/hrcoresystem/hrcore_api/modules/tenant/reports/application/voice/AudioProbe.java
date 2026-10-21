package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Component;

@Component
public final class AudioProbe {
    private static final int MAX_OUTPUT = 1024 * 1024;
    private final AiVoiceProperties properties;
    private final ObjectMapper mapper;

    public AudioProbe(AiVoiceProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    public String inspect(Path path, String declaredMime) {
        String extension = signature(path, declaredMime);
        List<String> command = List.of(properties.getFfprobePath(), "-v", "error",
                "-protocol_whitelist", "file", "-show_entries",
                "format=format_name,duration:stream=codec_type,codec_name:packet=pts_time,duration_time",
                "-show_format", "-show_streams", "-show_packets", "-of", "json",
                path.toAbsolutePath().toString());
        Process process;
        try { process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start(); }
        catch (IOException failure) { throw new AiVoiceException(AiVoiceException.Code.UNAVAILABLE); }
        try { process.getOutputStream().close(); } catch (IOException ignored) { /* no stdin needed */ }
        FutureTask<byte[]> output = new FutureTask<>(() -> {
            try (var stream = process.getInputStream()) { return stream.readNBytes(MAX_OUTPUT + 1); }
        });
        Thread.ofVirtual().start(output);
        try {
            byte[] bytes = output.get(properties.getProbeTimeoutSeconds(), TimeUnit.SECONDS);
            if (!process.waitFor(200, TimeUnit.MILLISECONDS) || process.exitValue() != 0 || bytes.length > MAX_OUTPUT)
                throw new AiVoiceException(AiVoiceException.Code.INVALID_AUDIO);
            return validateProbe(extension, bytes);
        } catch (TimeoutException timeout) {
            throw new AiVoiceException(AiVoiceException.Code.TIMEOUT);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AiVoiceException(AiVoiceException.Code.TIMEOUT);
        } catch (Exception failure) {
            if (failure instanceof AiVoiceException voice) throw voice;
            throw new AiVoiceException(AiVoiceException.Code.INVALID_AUDIO);
        } finally {
            process.destroyForcibly(); output.cancel(true);
            try { process.waitFor(500, TimeUnit.MILLISECONDS); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        }
    }

    String validateProbe(String extension, byte[] bytes) {
        try {
            JsonNode root = mapper.readTree(bytes);
            if (root == null || !root.isObject() || !containerMatches(extension,
                    root.path("format").path("format_name").asText("")))
                throw new AiVoiceException(AiVoiceException.Code.INVALID_AUDIO);
            JsonNode streams = root.path("streams");
            if (!streams.isArray() || streams.size() != 1 || !"audio".equals(streams.get(0).path("codec_type").asText())
                    || !codecMatches(extension, streams.get(0).path("codec_name").asText()))
                throw new AiVoiceException(AiVoiceException.Code.INVALID_AUDIO);
            JsonNode packets = root.path("packets");
            if (!packets.isArray() || packets.isEmpty()) throw new AiVoiceException(AiVoiceException.Code.INVALID_AUDIO);
            double first = Double.POSITIVE_INFINITY, last = Double.NEGATIVE_INFINITY;
            for (JsonNode packet : packets) {
                double pts = number(packet.path("pts_time"));
                double duration = number(packet.path("duration_time"));
                if (!Double.isFinite(pts) || !Double.isFinite(duration) || duration < 0)
                    throw new AiVoiceException(AiVoiceException.Code.INVALID_AUDIO);
                first = Math.min(first, pts); last = Math.max(last, pts + duration);
            }
            double formatDuration = number(root.path("format").path("duration"));
            double duration = Math.max(last - first, Double.isFinite(formatDuration) ? formatDuration : 0);
            if (!Double.isFinite(duration) || duration <= 0)
                throw new AiVoiceException(AiVoiceException.Code.INVALID_AUDIO);
            if (duration > properties.getMaxDurationSeconds())
                throw new AiVoiceException(AiVoiceException.Code.TOO_LONG);
            return extension;
        } catch (Exception failure) {
            if (failure instanceof AiVoiceException voice) throw voice;
            throw new AiVoiceException(AiVoiceException.Code.INVALID_AUDIO);
        }
    }

    private static double number(JsonNode node) {
        try { return Double.parseDouble(node.asText()); }
        catch (NumberFormatException invalid) { return Double.NaN; }
    }

    private static boolean containerMatches(String extension, String formats) {
        return switch (extension) {
            case "webm" -> formats.contains("webm");
            case "ogg" -> formats.equals("ogg");
            case "mp4" -> formats.contains("mp4") || formats.contains("m4a");
            default -> false;
        };
    }

    private static boolean codecMatches(String extension, String codec) {
        return switch (extension) {
            case "webm", "ogg" -> codec.equals("opus");
            case "mp4" -> codec.equals("aac");
            default -> false;
        };
    }

    private static String signature(Path path, String declaredMime) {
        try {
            byte[] head;
            try (var stream = Files.newInputStream(path)) { head = stream.readNBytes(16); }
            if (head.length < 12) throw new AiVoiceException(AiVoiceException.Code.INVALID_AUDIO);
            String mime = declaredMime == null ? "" : declaredMime.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            if (mime.equals("audio/webm") && (head[0] & 255) == 0x1a && (head[1] & 255) == 0x45
                    && (head[2] & 255) == 0xdf && (head[3] & 255) == 0xa3) return "webm";
            if (mime.equals("audio/ogg") && head[0] == 'O' && head[1] == 'g'
                    && head[2] == 'g' && head[3] == 'S') return "ogg";
            if (mime.equals("audio/mp4") && head[4] == 'f' && head[5] == 't'
                    && head[6] == 'y' && head[7] == 'p') return "mp4";
            throw new AiVoiceException(AiVoiceException.Code.INVALID_AUDIO);
        } catch (IOException failure) {
            throw new AiVoiceException(AiVoiceException.Code.INVALID_AUDIO);
        }
    }
}
