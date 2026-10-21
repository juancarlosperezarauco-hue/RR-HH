package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.voice;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.AnalyticAuthorizationPolicy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiVoiceServiceTest {
    private final AiVoiceProperties properties = new AiVoiceProperties();
    private final AnalyticAuthorizationPolicy authorization = mock(AnalyticAuthorizationPolicy.class);
    private final SecurityContextFacade security = mock(SecurityContextFacade.class);
    private final AudioProbe probe = mock(AudioProbe.class);
    private final TranscriptionProvider provider = mock(TranscriptionProvider.class);
    private AiVoiceService service;

    @BeforeEach void setup() {
        service = new AiVoiceService(properties, authorization, security,
                new AiVoiceRateLimiter(properties), probe, provider);
        when(authorization.authorizedCatalog()).thenReturn(Map.of("employees",
                new AnalyticAuthorizationPolicy.DatasetCapabilities(true, false, Set.of(), Set.of())));
        when(security.getCurrentTenantSlug()).thenReturn("tenant-a");
        when(security.requireCurrentUserId()).thenReturn(UUID.randomUUID());
    }

    @Test void disabledWithoutFlagOrKeyAndRequiresAuthorizedCatalog() {
        assertFalse(service.config().enabled());
        properties.setEnabled(true);
        assertFalse(service.config().enabled());
        var audio = new MockMultipartFile("file", "recording.webm", "audio/webm", new byte[] {1});
        assertEquals(AiVoiceException.Code.UNAVAILABLE,
                assertThrows(AiVoiceException.class, () -> service.transcribe(audio)).code());
        when(authorization.authorizedCatalog()).thenReturn(Map.of());
        assertThrows(AccessDeniedException.class, service::config);
        verifyNoInteractions(provider);
    }

    @Test void validatesSizeAndEmptyBeforeProvider() {
        properties.setEnabled(true); properties.setApiKey("synthetic-key"); properties.setMaxBytes(3);
        assertEquals(AiVoiceException.Code.INVALID_AUDIO, assertThrows(AiVoiceException.class,
                () -> service.transcribe(new MockMultipartFile("file", new byte[0]))).code());
        assertEquals(AiVoiceException.Code.TOO_LARGE, assertThrows(AiVoiceException.class,
                () -> service.transcribe(new MockMultipartFile("file", new byte[4]))).code());
        verifyNoInteractions(probe, provider);
    }

    @Test void streamsToTemporaryFileAndDeletesItAfterSuccessAndFailure() {
        properties.setEnabled(true); properties.setApiKey("synthetic-key");
        var audio = new MockMultipartFile("file", "recording.webm", "audio/webm", new byte[] {1, 2, 3});
        AtomicReference<Path> staged = new AtomicReference<>();
        when(probe.inspect(any(Path.class), eq("audio/webm"))).thenAnswer(call -> {
            Path path = call.getArgument(0); staged.set(path);
            assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(path));
            return "webm";
        });
        when(provider.transcribe(any(Path.class), eq("webm"))).thenReturn("Hola");
        assertEquals("Hola", service.transcribe(audio).text());
        assertFalse(Files.exists(staged.get()));
        when(probe.inspect(any(Path.class), eq("audio/webm")))
                .thenThrow(new AiVoiceException(AiVoiceException.Code.TOO_LONG));
        assertEquals(AiVoiceException.Code.TOO_LONG,
                assertThrows(AiVoiceException.class, () -> service.transcribe(audio)).code());
        assertFalse(Files.exists(staged.get()));
        verify(provider, times(1)).transcribe(any(Path.class), eq("webm"));
    }

    @Test void limitsAreSeparatedByUserAndTenantAndReleaseConcurrency() {
        properties.setUserRpm(1); properties.setTenantRpm(2); properties.setGlobalRpm(3);
        AiVoiceRateLimiter limiter = new AiVoiceRateLimiter(properties);
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        try (var lease = limiter.acquire("a", first)) {
            assertEquals(AiVoiceException.Code.BUSY,
                    assertThrows(AiVoiceException.class, () -> limiter.acquire("a", first)).code());
            try (var other = limiter.acquire("a", second)) { assertNotNull(other); }
            assertEquals(AiVoiceException.Code.BUSY,
                    assertThrows(AiVoiceException.class, () -> limiter.acquire("a", UUID.randomUUID())).code());
            try (var separate = limiter.acquire("b", UUID.randomUUID())) { assertNotNull(separate); }
        }
    }
}
