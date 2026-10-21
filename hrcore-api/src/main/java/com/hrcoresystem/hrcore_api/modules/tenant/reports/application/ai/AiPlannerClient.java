package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Only this client sees the service credential. It never forwards user authentication. */
@Component
public final class AiPlannerClient {
    private static final Logger log = LoggerFactory.getLogger(AiPlannerClient.class);
    private static final int MAX_RESPONSE_BYTES = 128 * 1024;
    private static final Set<String> PLAN_FIELDS = Set.of("status", "plan");
    private static final Set<String> CLARIFICATION_FIELDS = Set.of("status", "question", "reasonCode");
    private static final Set<String> UNSUPPORTED_FIELDS = Set.of("status", "message", "reasonCode");
    private static final Set<String> UNSUPPORTED_REASONS = Set.of(
            "DATASET_NOT_AVAILABLE", "CAPABILITY_NOT_AVAILABLE", "UNRECOGNIZED_REQUEST",
            "PERSONAL_IDENTIFIER_NOT_ALLOWED");
    private static final Set<String> CLARIFICATION_REASONS = Set.of(
            "MISSING_DATE_RANGE", "AMBIGUOUS_REQUEST", "MISSING_INFORMATION");

    public record Request(String contractVersion, String question, String catalogVersion,
                          Map<String, AnalyticAuthorizationPolicy.DatasetCapabilities> capabilities,
                          Map<String, Object> semanticContext,
                          ConversationContext conversationContext) {
        public Request(String contractVersion, String question, String catalogVersion,
                       Map<String, AnalyticAuthorizationPolicy.DatasetCapabilities> capabilities,
                       Map<String, Object> semanticContext) {
            this(contractVersion, question, catalogVersion, capabilities, semanticContext, null);
        }

        public Request(String contractVersion, String question, String catalogVersion,
                       Map<String, AnalyticAuthorizationPolicy.DatasetCapabilities> capabilities) {
            this(contractVersion, question, catalogVersion, capabilities, Map.of(), null);
        }

        public Request withConversationContext(ConversationContext context) {
            return new Request(contractVersion, question, catalogVersion, capabilities,
                    semanticContext, context);
        }
    }

    public record MessageMetadata(String role, String messageType, String state) {}

    public record ConversationContext(boolean hasUsablePlan, JsonNode lastSuccessfulPlan,
                                      String planVersion, String catalogVersion,
                                      List<MessageMetadata> recentMessages) {
        public ConversationContext {
            lastSuccessfulPlan = lastSuccessfulPlan == null ? null : lastSuccessfulPlan.deepCopy();
            recentMessages = recentMessages == null ? List.of() : List.copyOf(recentMessages);
        }

        public ConversationContext withoutPlan() {
            return new ConversationContext(false, null, null, null, recentMessages);
        }
    }
    public record Reply(String status, JsonNode plan, String reasonCode, String question) {
        public Reply(String status, JsonNode plan, String reasonCode) {
            this(status, plan, reasonCode, null);
        }
    }

    private final String url;
    private final String credential;
    private final RestClient rest;
    private final ObjectMapper mapper;
    private final int maxConversationContextChars;

    @Autowired
    public AiPlannerClient(@Value("${app.ai.planner.url:}") String url,
                           @Value("${app.ai.planner.service-token:}") String credential,
                           @Value("${app.ai.planner.mode:mock}") String mode,
                           ObjectMapper mapper,
                           @Value("${app.ai.conversations.context-max-chars:4000}") int maxConversationContextChars,
                           @Value("${app.ai.planner.read-timeout-seconds:65}") int readTimeoutSeconds) {
        if (maxConversationContextChars < 1 || maxConversationContextChars > 32_000)
            throw new IllegalArgumentException("AI conversation context size must be between 1 and 32000");
        if (readTimeoutSeconds < 3 || readTimeoutSeconds > 180)
            throw new IllegalArgumentException("AI planner read timeout must be between 3 and 180 seconds");
        this.url = url == null ? "" : url.trim().replaceAll("/+$", "");
        this.credential = credential == null ? "" : credential;
        this.maxConversationContextChars = maxConversationContextChars;
        this.mapper = mapper.copy();
        this.mapper.getFactory().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds("gemini".equals(mode) ? readTimeoutSeconds : 3));
        this.rest = RestClient.builder().requestFactory(factory).build();
    }

    public AiPlannerClient(String url, String credential, String mode, ObjectMapper mapper,
                           int maxConversationContextChars) {
        this(url, credential, mode, mapper, maxConversationContextChars,
                "gemini".equals(mode) ? 65 : 3);
    }

    public AiPlannerClient(String url, String credential, String mode, ObjectMapper mapper) {
        this(url, credential, mode, mapper, 4000);
    }

    public AiPlannerClient(String url, String credential, ObjectMapper mapper) {
        this(url, credential, "mock", mapper, 4000);
    }

    public Reply plan(Request request) {
        if (!configured()) throw new QueryPlanException(QueryPlanException.Code.SERVICE_UNAVAILABLE);
        try {
            Request boundedRequest = boundedContext(request);
            log.info("AI planner request sent question={} payload={}",
                    truncate(boundedRequest.question(), 500), truncateJson(boundedRequest));
            byte[] body = rest.post().uri(url + "/internal/v1/plan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Service-Token", credential)
                    .body(boundedRequest)
                    .exchange((sent, response) -> {
                        if (response.getStatusCode().value() == 504)
                            throw new AiFlowException(AiFlowException.Code.PLANNER_TIMEOUT);
                        if (response.getStatusCode().value() == 502)
                            throw invalid();
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
                Reply parsed = reply(envelope);
                log.info("AI planner response received status={} reasonCode={} payload={}",
                        parsed.status(), parsed.reasonCode(), truncate(envelope.toString(), 8000));
                return parsed;
            } catch (IOException invalidJson) {
                throw invalid();
            }
        } catch (RestClientException | IllegalArgumentException failure) {
            log.warn("AI planner request failed type={} message={}",
                    failure.getClass().getSimpleName(), truncate(failure.getMessage(), 300));
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof HttpTimeoutException || cause instanceof java.net.SocketTimeoutException)
                    throw new AiFlowException(AiFlowException.Code.PLANNER_TIMEOUT);
            }
            throw new QueryPlanException(QueryPlanException.Code.SERVICE_UNAVAILABLE);
        }
    }

    private String truncateJson(Object value) {
        try {
            return truncate(mapper.writeValueAsString(value), 12_000);
        } catch (IOException serializationFailure) {
            return "<unserializable>";
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "…";
    }

    private Request boundedContext(Request request) {
        ConversationContext context = request.conversationContext();
        if (context == null || contextLength(context) <= maxConversationContextChars) return request;

        ConversationContext withoutPlan = context.withoutPlan();
        if (contextLength(withoutPlan) <= maxConversationContextChars)
            return request.withConversationContext(withoutPlan);
        return request.withConversationContext(null);
    }

    private int contextLength(ConversationContext context) {
        try {
            String json = mapper.writeValueAsString(context);
            return json.codePointCount(0, json.length());
        } catch (IOException serializationFailure) {
            return Integer.MAX_VALUE;
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
                String reason = text(root, "reasonCode");
                if (!CLARIFICATION_REASONS.contains(reason)) throw invalid();
                yield new Reply(status, null, reason, text(root, "question"));
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
