package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.voice;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class GroqTranscriptionProvider implements TranscriptionProvider {
    private static final URI ENDPOINT = URI.create("https://api.groq.com/openai/v1/audio/transcriptions");
    private static final int MAX_RESPONSE_BYTES = 8192;
    private final AiVoiceProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient client;
    private final URI endpoint;

    @Autowired
    public GroqTranscriptionProvider(AiVoiceProperties properties, ObjectMapper mapper) {
        this(properties, mapper, ENDPOINT, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }

    // Tests inject a mock client; production always uses the fixed Groq URI.
    GroqTranscriptionProvider(AiVoiceProperties properties, ObjectMapper mapper, URI endpoint, HttpClient client) {
        this.properties = properties;
        this.mapper = mapper.copy();
        this.mapper.getFactory().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        this.endpoint = endpoint;
        this.client = client;
    }

    @Override
    public String transcribe(Path audio, String extension) {
        if (!properties.available()) throw new AiVoiceException(AiVoiceException.Code.UNAVAILABLE);
        String boundary = "hrcore-" + UUID.randomUUID();
        String contentType = switch (extension) {
            case "webm" -> "audio/webm";
            case "ogg" -> "audio/ogg";
            case "mp4" -> "audio/mp4";
            default -> throw new AiVoiceException(AiVoiceException.Code.INVALID_AUDIO);
        };
        String header = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\n"
                + properties.getModel() + "\r\n--" + boundary
                + "\r\nContent-Disposition: form-data; name=\"language\"\r\n\r\nes\r\n--" + boundary
                + "\r\nContent-Disposition: form-data; name=\"response_format\"\r\n\r\njson\r\n--" + boundary
                + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"recording." + extension
                + "\"\r\nContent-Type: " + contentType + "\r\n\r\n";
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(properties.getProviderTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.concat(
                            HttpRequest.BodyPublishers.ofByteArray(header.getBytes(StandardCharsets.UTF_8)),
                            HttpRequest.BodyPublishers.ofFile(audio),
                            HttpRequest.BodyPublishers.ofByteArray(("\r\n--" + boundary + "--\r\n")
                                    .getBytes(StandardCharsets.UTF_8))))
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                int status = response.statusCode();
                if (status == 429) throw new AiVoiceException(AiVoiceException.Code.QUOTA);
                if (status == 401 || status == 403) throw new AiVoiceException(AiVoiceException.Code.UNAVAILABLE);
                if (status < 200 || status >= 300) throw new AiVoiceException(AiVoiceException.Code.PROVIDER);
                if (response.headers().firstValue("Content-Type").filter(value -> value.toLowerCase()
                        .startsWith("application/json")).isEmpty())
                    throw new AiVoiceException(AiVoiceException.Code.PROVIDER);
                byte[] bytes = body.readNBytes(MAX_RESPONSE_BYTES + 1);
                if (bytes.length > MAX_RESPONSE_BYTES) throw new AiVoiceException(AiVoiceException.Code.PROVIDER);
                try (JsonParser parser = mapper.getFactory().createParser(bytes)) {
                    JsonNode root = mapper.readTree(parser);
                    if (root == null || !root.isObject() || parser.nextToken() != null
                            || !root.path("text").isTextual())
                        throw new AiVoiceException(AiVoiceException.Code.PROVIDER);
                    String text = root.path("text").textValue().trim();
                    if (text.isBlank() || text.codePointCount(0, text.length()) > 2000 || text.indexOf('\0') >= 0)
                        throw new AiVoiceException(AiVoiceException.Code.PROVIDER);
                    return text;
                }
            }
        } catch (HttpTimeoutException timeout) {
            throw new AiVoiceException(AiVoiceException.Code.TIMEOUT);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AiVoiceException(AiVoiceException.Code.TIMEOUT);
        } catch (IOException failure) {
            throw new AiVoiceException(AiVoiceException.Code.PROVIDER);
        }
    }
}
