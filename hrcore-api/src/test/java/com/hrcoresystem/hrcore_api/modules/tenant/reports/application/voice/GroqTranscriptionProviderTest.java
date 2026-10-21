package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.voice;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GroqTranscriptionProviderTest {
    private Path audio;
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> body = new AtomicReference<>("{\"text\":\"Hola equipo\"}");
    private final AtomicReference<HttpRequest> request = new AtomicReference<>();
    private GroqTranscriptionProvider provider;
    private HttpClient client;

    @BeforeEach void setup() throws Exception {
        audio = Files.createTempFile("synthetic-voice-", ".webm");
        Files.write(audio, "synthetic audio".getBytes(StandardCharsets.UTF_8));
        AiVoiceProperties properties = new AiVoiceProperties();
        properties.setEnabled(true); properties.setApiKey("synthetic-key");
        client = mock(HttpClient.class);
        when(client.send(any(HttpRequest.class), any())).thenAnswer(call -> {
            request.set(call.getArgument(0));
            @SuppressWarnings("unchecked") HttpResponse<java.io.InputStream> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(status.get());
            when(response.body()).thenReturn(new ByteArrayInputStream(body.get().getBytes(StandardCharsets.UTF_8)));
            when(response.headers()).thenReturn(HttpHeaders.of(
                    Map.of("content-type", List.of("application/json")), (name, value) -> true));
            return response;
        });
        provider = new GroqTranscriptionProvider(properties, new ObjectMapper(),
                URI.create("https://api.groq.com/openai/v1/audio/transcriptions"), client);
    }

    @AfterEach void cleanup() throws Exception { Files.deleteIfExists(audio); }

    @Test void sendsExpectedMultipartAndReturnsOnlyText() throws Exception {
        assertEquals("Hola equipo", provider.transcribe(audio, "webm"));
        HttpRequest sent = request.get();
        assertEquals("https://api.groq.com/openai/v1/audio/transcriptions", sent.uri().toString());
        assertEquals("Bearer synthetic-key", sent.headers().firstValue("Authorization").orElseThrow());
        String payload = publisherText(sent.bodyPublisher().orElseThrow());
        assertTrue(payload.contains("name=\"model\"\r\n\r\nwhisper-large-v3-turbo"));
        assertTrue(payload.contains("name=\"language\"\r\n\r\nes"));
        assertTrue(payload.contains("name=\"response_format\"\r\n\r\njson"));
        assertTrue(payload.contains("name=\"file\"; filename=\"recording.webm\""));
        assertTrue(payload.contains("synthetic audio"));
    }

    @Test void mapsProviderStatusesAndRejectsMalformedResponses() {
        for (int code : List.of(401, 403, 429, 500)) {
            status.set(code);
            AiVoiceException.Code expected = code == 429 ? AiVoiceException.Code.QUOTA
                    : code == 401 || code == 403 ? AiVoiceException.Code.UNAVAILABLE : AiVoiceException.Code.PROVIDER;
            assertEquals(expected, assertThrows(AiVoiceException.class,
                    () -> provider.transcribe(audio, "webm")).code());
        }
        status.set(200);
        for (String invalid : List.of("not json", "{}", "{\"text\":\"\"}", "{\"text\":3}")) {
            body.set(invalid);
            assertEquals(AiVoiceException.Code.PROVIDER, assertThrows(AiVoiceException.class,
                    () -> provider.transcribe(audio, "webm")).code());
        }
    }

    @Test void timeoutDoesNotRetry() throws Exception {
        reset(client);
        when(client.send(any(HttpRequest.class), any())).thenThrow(new HttpTimeoutException("synthetic timeout"));
        assertEquals(AiVoiceException.Code.TIMEOUT,
                assertThrows(AiVoiceException.class, () -> provider.transcribe(audio, "webm")).code());
        verify(client, times(1)).send(any(HttpRequest.class), any());
    }

    private static String publisherText(HttpRequest.BodyPublisher publisher) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        publisher.subscribe(new Flow.Subscriber<>() {
            @Override public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            @Override public void onNext(ByteBuffer buffer) {
                byte[] chunk = new byte[buffer.remaining()]; buffer.get(chunk); output.writeBytes(chunk);
            }
            @Override public void onError(Throwable error) { failure.set(error); done.countDown(); }
            @Override public void onComplete() { done.countDown(); }
        });
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertNull(failure.get());
        return output.toString(StandardCharsets.UTF_8);
    }
}
