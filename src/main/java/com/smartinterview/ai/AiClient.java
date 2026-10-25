package com.smartinterview.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Minimal blocking client for Anthropic and OpenAI-compatible chat APIs.
 * Call from a background thread, never the JavaFX thread.
 */
public final class AiClient {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private static final AiClient INSTANCE = new AiClient();

    private AiClient() {}

    public static AiClient getInstance() {
        return INSTANCE;
    }

    public boolean isConfigured() {
        return AiConfig.isConfigured();
    }

    /** Sends one system + user prompt and returns the model's text reply. Tries each provider in turn. */
    public String complete(String system, String user, int maxTokens) throws AiException {
        return tryEach(system, user, maxTokens, reply -> reply);
    }

    /** Like {@link #complete} but extracts and parses the JSON object or array in the reply. */
    public JsonNode completeJson(String system, String user, int maxTokens) throws AiException {
        return tryEach(system + "\n\nRespond with valid JSON only: no prose, no markdown fences.",
                user, maxTokens, AiClient::parseJson);
    }

    private interface Parser<T> {
        T parse(String reply) throws AiException;
    }

    /** A provider that errors, or returns something unparseable, falls through to the next one. */
    private <T> T tryEach(String system, String user, int maxTokens, Parser<T> parser) throws AiException {
        List<AiConfig.Endpoint> endpoints = AiConfig.endpoints();
        if (endpoints.isEmpty()) {
            throw new AiException(
                    "No AI API key configured. Set GROQ_API_KEY, GEMINI_API_KEY or similar (see config.properties.example).");
        }
        AiException last = null;
        for (AiConfig.Endpoint ep : endpoints) {
            try {
                String reply = ep.anthropic() ? callAnthropic(ep, system, user, maxTokens)
                        : callOpenAi(ep, system, user, maxTokens);
                return parser.parse(reply);
            } catch (AiException e) {
                last = new AiException(ep.name() + ": " + e.getMessage(), e);
                System.err.println("AI provider failed, trying next: " + last.getMessage());
            }
        }
        throw last;
    }

    static JsonNode parseJson(String reply) throws AiException {
        String text = reply.trim();
        // Models sometimes wrap JSON in ```json fences or add a sentence around it.
        int objStart = text.indexOf('{');
        int arrStart = text.indexOf('[');
        int start;
        char close;
        if (objStart >= 0 && (arrStart < 0 || objStart < arrStart)) {
            start = objStart;
            close = '}';
        } else if (arrStart >= 0) {
            start = arrStart;
            close = ']';
        } else {
            throw new AiException("The AI reply did not contain JSON.");
        }
        int end = text.lastIndexOf(close);
        if (end <= start) {
            throw new AiException("The AI reply contained incomplete JSON.");
        }
        try {
            return JSON.readTree(text.substring(start, end + 1));
        } catch (IOException e) {
            throw new AiException("The AI reply was not valid JSON.", e);
        }
    }

    // ---------------------------------------------------------------- providers

    private String callAnthropic(AiConfig.Endpoint ep, String system, String user, int maxTokens) throws AiException {
        ObjectNode body = JSON.createObjectNode();
        body.put("model", ep.model());
        body.put("max_tokens", maxTokens);
        body.put("system", system);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "user").put("content", user);

        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        ep.baseUrl() + "/messages"))
                .timeout(Duration.ofSeconds(90))
                .header("content-type", "application/json")
                .header("x-api-key", ep.apiKey())
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        JsonNode root = send(request);
        StringBuilder text = new StringBuilder();
        for (JsonNode block : root.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        if (text.isEmpty()) {
            throw new AiException("The AI returned an empty reply.");
        }
        return text.toString();
    }

    private String callOpenAi(AiConfig.Endpoint ep, String system, String user, int maxTokens) throws AiException {
        ObjectNode body = JSON.createObjectNode();
        body.put("model", ep.model());
        // Reasoning models (gpt-oss, Gemini) spend part of this budget thinking, so leave room.
        body.put("max_tokens", maxTokens * 2);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", system);
        messages.addObject().put("role", "user").put("content", user);

        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        ep.baseUrl() + "/chat/completions"))
                .timeout(Duration.ofSeconds(90))
                .header("content-type", "application/json")
                .header("authorization", "Bearer " + ep.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        JsonNode root = send(request);
        String text = root.path("choices").path(0).path("message").path("content").asText("");
        if (text.isEmpty()) {
            throw new AiException("The AI returned an empty reply.");
        }
        return text;
    }

    private JsonNode send(HttpRequest request) throws AiException {
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new AiException(describeHttpError(response.statusCode(), response.body()));
            }
            return JSON.readTree(response.body());
        } catch (IOException e) {
            throw new AiException("Could not reach the AI service: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("The AI request was interrupted.", e);
        }
    }

    private static String describeHttpError(int status, String body) {
        String detail = "";
        try {
            JsonNode err = JSON.readTree(body).path("error");
            detail = err.isTextual() ? err.asText() : err.path("message").asText("");
        } catch (IOException ignored) {
            // fall through to the status-only message
        }
        String reason = switch (status) {
            case 401, 403 -> "the API key was rejected";
            case 429 -> "rate limit or quota reached";
            case 529, 503 -> "the service is overloaded";
            default -> "request failed";
        };
        return "AI service error (" + status + ", " + reason + ")" + (detail.isEmpty() ? "" : ": " + detail);
    }
}
