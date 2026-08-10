package com.fpoly.marcusstore.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    // Marcus thêm: một client dùng chung cho AI tư vấn và AI Analytics; timeout,
    // model và endpoint không còn bị khai báo rải rác ở từng service.
    public GeminiClient(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.5-flash-lite}") String model,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${gemini.connect-timeout-seconds:8}") int connectTimeoutSeconds,
            @Value("${gemini.read-timeout-seconds:60}") int readTimeoutSeconds) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)));
        factory.setReadTimeout(Duration.ofSeconds(Math.max(5, readTimeoutSeconds)));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public String modelName() {
        return model;
    }

    public JsonNode generate(String systemInstruction, String userInput,
                             Map<String, Object> responseSchema, int maxOutputTokens) {
        if (!isConfigured()) throw new IllegalStateException("Gemini chưa được cấu hình.");
        try {
            return restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                            "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userInput)))),
                            "generationConfig", Map.of(
                                    "maxOutputTokens", maxOutputTokens,
                                    "responseMimeType", "application/json",
                                    "responseJsonSchema", responseSchema)))
                    .retrieve().body(JsonNode.class);
        } catch (HttpStatusCodeException exception) {
            throw new GeminiClientException(exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            throw new GeminiClientException(0, exception);
        }
    }

    public static class GeminiClientException extends RuntimeException {
        private final int statusCode;
        public GeminiClientException(int statusCode, Throwable cause) {
            super(cause);
            this.statusCode = statusCode;
        }
        public int statusCode() { return statusCode; }
    }
}
