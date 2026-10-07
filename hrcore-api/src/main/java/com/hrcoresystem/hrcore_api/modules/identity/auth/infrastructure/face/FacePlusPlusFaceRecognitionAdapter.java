package com.hrcoresystem.hrcore_api.modules.identity.auth.infrastructure.face;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.config.FaceRecognitionProperties;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.exception.FaceRecognitionUnavailableException;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.port.FaceRecognitionPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class FacePlusPlusFaceRecognitionAdapter implements FaceRecognitionPort {

    private static final Semaphore FACEPP_GATE = new Semaphore(1);

    private final FaceRecognitionProperties properties;
    private final RestClient restClient;

    public FacePlusPlusFaceRecognitionAdapter(FaceRecognitionProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeoutMs());
        factory.setReadTimeout(properties.getTimeoutMs());

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public DetectedFace detectFace(byte[] imageBytes, String filename, String contentType) {
        ensureConfigured();
        Map<String, Object> response = postForm("/detect", buildDetectForm(imageBytes, filename, contentType));
        validateFacePlusPlusResponse(response);

        Object facesValue = response.get("faces");
        if (!(facesValue instanceof List<?> faces) || faces.isEmpty()) {
            throw new BusinessException("No se detectó un rostro válido en la imagen");
        }
        if (faces.size() != 1) {
            throw new BusinessException("La imagen debe contener un solo rostro");
        }

        Object firstFace = faces.get(0);
        if (!(firstFace instanceof Map<?, ?> faceMap)) {
            throw new BusinessException("Respuesta de Face++ inválida");
        }

        String faceToken = stringValue(faceMap.get("face_token"));
        String faceId = stringValue(faceMap.get("face_id"));

        if (!StringUtils.hasText(faceToken)) {
            throw new BusinessException("Face++ no devolvió un token de rostro válido");
        }

        return new DetectedFace(faceToken, faceId);
    }

    @Override
    public FaceComparisonResult compareFaceTokens(String faceToken1, String faceToken2) {
        ensureConfigured();
        Map<String, Object> response = postForm("/compare", buildCompareForm(faceToken1, faceToken2));
        validateFacePlusPlusResponse(response);

        Number confidence = numberValue(response.get("confidence"));
        if (confidence == null) {
            throw new BusinessException("Face++ no devolvió la confianza de comparación");
        }

        return new FaceComparisonResult(confidence.doubleValue());
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    private void ensureConfigured() {
        if (!properties.isConfigured()) {
            throw new FaceRecognitionUnavailableException("Face recognition is not configured", null);
        }
    }

    private Map<String, Object> postForm(String path, MultiValueMap<String, Object> form) {
        FaceRecognitionUnavailableException lastUnavailable = null;
        boolean acquired = false;

        try {
            acquired = FACEPP_GATE.tryAcquire(30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new FaceRecognitionUnavailableException(
                        "Face++ is busy, please retry",
                        null
                );
            }

            for (String baseUrl : List.of(properties.getBaseUrl(), properties.getFallbackBaseUrl())) {
                if (!StringUtils.hasText(baseUrl)) {
                    continue;
                }

                for (int attempt = 1; attempt <= 2; attempt++) {
                    try {
                        RestClient client = restClient.mutate().baseUrl(baseUrl).build();
                        ResponseEntity<Map> response = client.post()
                                .uri(path)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(form)
                                .retrieve()
                                .onStatus(
                                        status -> status.is4xxClientError() || status.is5xxServerError(),
                                        (request, response1) -> {
                                            throw new FaceRecognitionUnavailableException(
                                                    describeFacePlusPlusError(baseUrl, path, response1),
                                                    null
                                            );
                                        }
                                )
                                .toEntity(Map.class);

                        @SuppressWarnings("unchecked")
                        Map<String, Object> body = (Map<String, Object>) response.getBody();
                        if (body == null) {
                            throw new FaceRecognitionUnavailableException(
                                    "Face++ devolvió una respuesta vacía desde " + baseUrl + path,
                                    null
                            );
                        }

                        return body;
                    } catch (FaceRecognitionUnavailableException exception) {
                        lastUnavailable = exception;
                        if (!isConcurrencyLimit(exception) || attempt == 2) {
                            break;
                        }
                        sleepQuietly(500L);
                    } catch (Exception exception) {
                        lastUnavailable = new FaceRecognitionUnavailableException(
                                "No fue posible contactar Face++ en " + baseUrl + path + ": " + rootMessage(exception),
                                exception
                        );
                        break;
                    }
                }
            }

            if (lastUnavailable != null) {
                throw lastUnavailable;
            }
            throw new FaceRecognitionUnavailableException("Face++ is unavailable", null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new FaceRecognitionUnavailableException("Face++ request was interrupted", exception);
        } finally {
            if (acquired) {
                FACEPP_GATE.release();
            }
        }
    }

    private ByteArrayResource byteArrayResource(byte[] bytes, String filename) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return StringUtils.hasText(filename) ? filename : "face.jpg";
            }
        };
    }

    private MultiValueMap<String, Object> buildDetectForm(byte[] imageBytes, String filename, String contentType) {
        ByteArrayResource file = byteArrayResource(imageBytes, filename);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(parseMediaType(contentType, "image/jpeg"));

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("api_key", properties.getApiKey());
        form.add("api_secret", properties.getApiSecret());
        form.add("return_face_id", "1");
        form.add("return_landmark", "0");
        form.add("image_file", new HttpEntity<>(file, headers));
        return form;
    }

    private MultiValueMap<String, Object> buildCompareForm(String faceToken1, String faceToken2) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("api_key", properties.getApiKey());
        form.add("api_secret", properties.getApiSecret());
        form.add("face_token1", faceToken1);
        form.add("face_token2", faceToken2);
        return form;
    }

    private MediaType parseMediaType(String contentType, String fallback) {
        String value = StringUtils.hasText(contentType) ? contentType : fallback;

        try {
            MediaType parsed = MediaType.parseMediaType(value);
            return new MediaType(parsed.getType(), parsed.getSubtype());
        } catch (Exception exception) {
            return MediaType.parseMediaType(fallback);
        }
    }

    private void validateFacePlusPlusResponse(Map<String, Object> response) {
        if (response == null) {
            throw new BusinessException("Face++ devolvió una respuesta vacía");
        }

        Object errorMessage = response.get("error_message");
        if (errorMessage != null && StringUtils.hasText(String.valueOf(errorMessage))) {
            throw new BusinessException(String.valueOf(errorMessage));
        }
    }

    private String describeFacePlusPlusError(String baseUrl, String path, ClientHttpResponse response) {
        String status = "unknown";
        String body = "";

        try {
            status = String.valueOf(response.getStatusCode());
        } catch (IOException ignored) {
        }

        try {
            body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }

        if (!StringUtils.hasText(body)) {
            return "Face++ respondió con estado " + status + " en " + baseUrl + path;
        }

        return "Face++ respondió con estado " + status + " en " + baseUrl + path + ": " + body;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String rootMessage(Exception exception) {
        Throwable current = exception;
        String last = exception.getMessage();
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                last = current.getMessage();
            }
            current = current.getCause();
        }
        return last == null || last.isBlank() ? "unknown error" : last;
    }

    private Number numberValue(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private boolean isConcurrencyLimit(FaceRecognitionUnavailableException exception) {
        String message = exception.getMessage();
        return message != null && message.contains("CONCURRENCY_LIMIT_EXCEEDED");
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
