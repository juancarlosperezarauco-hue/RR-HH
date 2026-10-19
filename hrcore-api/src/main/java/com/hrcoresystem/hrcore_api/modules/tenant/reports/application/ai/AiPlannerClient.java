package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Only this client sees the service credential. It never forwards user authentication. */
@Component
public final class AiPlannerClient {
    private static final int MAX_RESPONSE_BYTES = 128 * 1024;
    private static final Set<String> PLAN_FIELDS = Set.of("status", "plan");
    private static final Set<String> CLARIFICATION_FIELDS = Set.of("status", "question", "reasonCode");
    private static final Set<String> UNSUPPORTED_FIELDS = Set.of("status", "message", "reasonCode");
    private static final Set<String> UNSUPPORTED_REASONS = Set.of(
            "DATASET_NOT_AVAILABLE", "CAPABILITY_NOT_AVAILABLE", "UNRECOGNIZED_REQUEST");

    public record Request(String contractVersion, String question, String catalogVersion,
                          Map<String, AnalyticAuthorizationPolicy.DatasetCapabilities> capabilities) {}
    public record Reply(String status, JsonNode plan, String reasonCode) {}

    private final String url;
    private final String credential;
    private final RestClient rest;
    private final ObjectMapper mapper;

    public AiPlannerClient(@Value("${app.ai.planner.url:}") String url,
                           @Value("${app.ai.planner.service-token:}") String credential,
                           ObjectMapper mapper) {
        this.url = url == null ? "" : url.trim().replaceAll("/+$", "");
        this.credential = credential == null ? "" : credential;
        this.mapper = mapper.copy();
        this.mapper.getFactory().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.rest = RestClient.builder().requestFactory(factory).build();
    }

    public Reply plan(Request request) {
        if (!configured()) throw new QueryPlanException(QueryPlanException.Code.SERVICE_UNAVAILABLE);
        try {
            byte[] body = rest.post().uri(url + "/internal/v1/plan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Service-Token", credential)
                    .body(request)
                    .exchange((sent, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful())
                            throw new QueryPlanException(QueryPlanException.Code.SERVICE_UNAVAILABLE);
                        MediaType contentType = response.getHeaders().getContentType();
                        if (contentType == null || !MediaType.APPLICATION_JSON.isCompatibleWith(contentType))
                            throw invalid();
                        byte[] bytes = response.getBody().readNBytes(MAX_RESPONSE_BYTES + 1);
                        if (bytes.length > MAX_RESPONSE_BYTES) throw invalid();
                        return bytes;
                    });
            try (JsonParser parser = mapper.getFactory().createParser(body)) {
                JsonNode envelope = mapper.readTree(parser);
                if (envelope == null || parser.nextToken() != null || !envelope.isObject()) throw invalid();
                return reply(envelope);
            } catch (IOException invalidJson) {
                throw invalid();
            }
        } catch (RestClientException | IllegalArgumentException failure) {
            throw new QueryPlanException(QueryPlanException.Code.SERVICE_UNAVAILABLE);
        }
    }

    private boolean configured() {
        if (credential.length() < 32 || url.isBlank()) return false;
        try {
            URI uri = URI.create(url);
            return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null
                    && uri.getRawQuery() == null && uri.getRawFragment() == null
                    && (uri.getPath() == null || uri.getPath().isEmpty() || "/".equals(uri.getPath()));
        } catch (IllegalArgumentException invalidUrl) {
            return false;
        }
    }

    private static Reply reply(JsonNode root) {
        JsonNode statusNode = root.get("status");
        if (statusNode == null || !statusNode.isTextual()) throw invalid();
        String status = statusNode.textValue();
        return switch (status) {
            case "query_plan" -> {
                exactFields(root, PLAN_FIELDS);
                JsonNode plan = root.get("plan");
                if (plan == null || !plan.isObject()) throw invalid();
                yield new Reply(status, plan, null);
            }
            case "needs_clarification" -> {
                exactFields(root, CLARIFICATION_FIELDS);
                text(root, "question");
                if (!"MISSING_DATE_RANGE".equals(text(root, "reasonCode"))) throw invalid();
                yield new Reply(status, null, "MISSING_DATE_RANGE");
            }
            case "unsupported" -> {
                exactFields(root, UNSUPPORTED_FIELDS);
                text(root, "message");
                String reason = text(root, "reasonCode");
                if (!UNSUPPORTED_REASONS.contains(reason)) throw invalid();
                yield new Reply(status, null, reason);
            }
            default -> throw invalid();
        };
    }

    private static String text(JsonNode root, String key) {
        JsonNode node = root.get(key);
        if (node == null || !node.isTextual() || node.textValue().isBlank()
                || node.textValue().codePointCount(0, node.textValue().length()) > 300)
            throw invalid();
        return node.textValue();
    }

    private static void exactFields(JsonNode root, Set<String> expected) {
        if (root.size() != expected.size() || !root.properties().stream()
                .map(Map.Entry::getKey).allMatch(expected::contains)) throw invalid();
    }

    private static AiFlowException invalid() {
        return new AiFlowException(AiFlowException.Code.INVALID_PLANNER_RESPONSE);
    }
}
