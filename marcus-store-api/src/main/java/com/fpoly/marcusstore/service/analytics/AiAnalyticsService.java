package com.fpoly.marcusstore.service.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpoly.marcusstore.dto.analytics.AiAnalyticsReportResponse;
import com.fpoly.marcusstore.dto.analytics.AiAnalyticsReportResponse.Action;
import com.fpoly.marcusstore.dto.analytics.AiAnalyticsReportResponse.ProductOutlook;
import com.fpoly.marcusstore.dto.analytics.AiAnalyticsReportResponse.Signal;
import com.fpoly.marcusstore.dto.analytics.AnalyticsOverviewResponse;
import com.fpoly.marcusstore.dto.analytics.AnalyticsPeriod;
import com.fpoly.marcusstore.dto.analytics.ProductTrendResponse;
import com.fpoly.marcusstore.entity.analytics.AiAnalyticsReport;
import com.fpoly.marcusstore.repository.analytics.AiAnalyticsReportRepository;
import com.fpoly.marcusstore.service.ai.AiUsageEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiAnalyticsService {

    private static final Duration DOUBLE_SUBMIT_GUARD = Duration.ofSeconds(20);
    private static final String METRIC_SCHEMA_VERSION = "recognized-revenue-v2";
    private static final int PRODUCT_LIMIT = 10;
    private static final Set<String> OUTLOOKS = Set.of("GROWTH", "STEADY", "DECLINE", "UNCERTAIN");
    private static final Set<String> SEVERITIES = Set.of("POSITIVE", "INFO", "WARNING", "CRITICAL");
    private static final Set<String> PRIORITIES = Set.of("HIGH", "MEDIUM", "LOW");
    private static final Set<String> DIRECTIONS = Set.of("UP", "STEADY", "DOWN", "UNCERTAIN");

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;
    private final AiAnalyticsReportRepository reportRepository;
    private final AiUsageEventService aiUsageEventService;
    private final Map<CacheKey, LocalDateTime> recentGenerations = new ConcurrentHashMap<>();

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.5-flash-lite}")
    private String model;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    /**
     * Marcus thêm: Gemini chỉ nhận DTO tổng hợp từ AnalyticsService. Service
     * không có Repository nghiệp vụ nên AI không thể tự truy vấn hoặc thay đổi
     * đơn hàng, khách hàng hay sản phẩm. Repository duy nhất chỉ lưu bản tin AI.
     */
    public AiAnalyticsReportResponse findLatestReport(LocalDate fromDate, LocalDate toDate) {
        AnalyticsPeriod period = analyticsService.resolvePeriod(fromDate, toDate);
        return reportRepository
                .findFirstByFromDateAndToDateAndModelNameOrderByGeneratedAtDesc(
                        period.fromDate(),
                        period.toDate(),
                        cacheModelName())
                .map(this::readStoredReport)
                .orElse(null);
    }

    public AiAnalyticsReportResponse generateReport(LocalDate fromDate, LocalDate toDate) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI phân tích chưa được cấu hình GEMINI_API_KEY.");
        }

        AnalyticsOverviewResponse overview = analyticsService.getOverview(fromDate, toDate);
        CacheKey cacheKey = new CacheKey(
                overview.period().fromDate(),
                overview.period().toDate());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recentGeneration = recentGenerations.get(cacheKey);
        if (recentGeneration != null && now.isBefore(recentGeneration.plus(DOUBLE_SUBMIT_GUARD))) {
            AiAnalyticsReportResponse stored = findStoredReport(cacheKey);
            if (stored != null) {
                return stored;
            }
        }

        List<ProductTrendResponse> products = analyticsService.getProductTrends(
                overview.period().fromDate(),
                overview.period().toDate(),
                PRODUCT_LIMIT);
        String context = buildSafeContext(overview, products);
        JsonNode providerResponse = callGemini(context);
        AiAnalyticsReportResponse report = parseReport(
                providerResponse,
                products,
                now,
                now);

        saveReport(cacheKey, report);
        recentGenerations.put(cacheKey, now);
        return report;
    }

    private AiAnalyticsReportResponse findStoredReport(CacheKey key) {
        return reportRepository
                .findFirstByFromDateAndToDateAndModelNameOrderByGeneratedAtDesc(
                        key.fromDate(), key.toDate(), cacheModelName())
                .map(this::readStoredReport)
                .orElse(null);
    }

    private void saveReport(CacheKey key, AiAnalyticsReportResponse report) {
        try {
            AiAnalyticsReport entity = new AiAnalyticsReport();
            entity.setFromDate(key.fromDate());
            entity.setToDate(key.toDate());
            entity.setReportJson(objectMapper.writeValueAsString(report));
            entity.setModelName(cacheModelName());
            entity.setGeneratedAt(report.generatedAt());
            reportRepository.save(entity);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "AI đã phân tích nhưng không thể lưu báo cáo. Vui lòng kiểm tra database.");
        }
    }

    private AiAnalyticsReportResponse readStoredReport(AiAnalyticsReport entity) {
        try {
            return objectMapper
                    .readValue(entity.getReportJson(), AiAnalyticsReportResponse.class)
                    .asCached();
        } catch (Exception exception) {
            throw new IllegalStateException("Báo cáo AI đã lưu không còn đúng định dạng.");
        }
    }

    private String buildSafeContext(
            AnalyticsOverviewResponse overview,
            List<ProductTrendResponse> products) {
        Map<String, Object> context = new LinkedHashMap<>();
        // Marcus thêm: gửi định nghĩa chỉ số để AI không diễn giải final_amount
        // của đơn chưa thu được tiền thành doanh thu.
        context.put("metricPolicy", Map.of(
                "completedSales",
                "SUCCESS COD/VNPAY transactions of COMPLETED orders, grouped by transaction date",
                "successfulRefundAmount",
                "SUCCESS REFUND transactions, grouped by transaction date",
                "profitAvailable",
                false));
        context.put("period", overview.period());
        context.put("completedSales", overview.completedSales());
        context.put("completedOrders", overview.completedOrders());
        context.put("unitsSold", overview.unitsSold());
        context.put("averageOrderValue", overview.averageOrderValue());
        context.put("completionRate", overview.completionRate());
        context.put("cancellationRate", overview.cancellationRate());
        context.put("successfulRefundAmount", overview.successfulRefundAmount());
        context.put("orderingCustomers", overview.orderingCustomers());
        context.put("productTrends", products);
        try {
            // Marcus thêm: AI chỉ nhận thống kê hành vi đã tổng hợp, không nhận
            // sessionId hay nội dung câu hỏi của từng khách.
            context.put(
                    "aiAdvisorUsage",
                    aiUsageEventService.summarize(
                            overview.period().fromDate(),
                            overview.period().toDate()));
        } catch (RuntimeException ignored) {
            // Chưa chạy migration telemetry vẫn cho phép phân tích số liệu bán hàng.
        }
        try {
            return objectMapper.writeValueAsString(context);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể chuẩn bị dữ liệu tổng hợp cho AI.");
        }
    }

    private JsonNode callGemini(String context) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(8));
        // Marcus sửa: báo cáo dài cần đủ thời gian hoàn thiện JSON, tránh cắt
        // phản hồi rồi báo sai định dạng.
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        try {
            return RestClient.builder()
                    .baseUrl(baseUrl)
                    .requestFactory(requestFactory)
                    .defaultHeader("x-goog-api-key", apiKey)
                    .build()
                    .post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "systemInstruction", Map.of(
                                    "parts", List.of(Map.of("text", systemInstructions()))),
                            "contents", List.of(Map.of(
                                    "role", "user",
                                    "parts", List.of(Map.of(
                                            "text",
                                            "DỮ LIỆU TỔNG HỢP ĐÃ KIỂM DUYỆT:\n" + context)))),
                            "generationConfig", Map.of(
                                    "maxOutputTokens", 2_400,
                                    "responseMimeType", "application/json",
                                    "responseJsonSchema", analyticsResponseSchema())))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpStatusCodeException exception) {
            throw new IllegalStateException(providerErrorMessage(exception));
        } catch (RestClientException exception) {
            throw new IllegalStateException("Không thể kết nối Gemini để tạo bản tin phân tích.");
        }
    }

    private String systemInstructions() {
        return """
                Bạn là Marcus AI Business Analyst dành cho chủ cửa hàng thương mại điện tử.
                Chỉ phân tích JSON tổng hợp được cung cấp. Không yêu cầu hoặc suy đoán dữ liệu khách hàng.
                Không gọi doanh thu là lợi nhuận vì hệ thống không có giá nhập.
                completedSales chỉ là tiền đã thu SUCCESS của đơn COMPLETED, không phải tổng final_amount.
                successfulRefundAmount là tiền REFUND đã SUCCESS; PENDING/FAILED không được coi là đã hoàn.
                Không bịa tin thị trường, tồn kho, nguyên nhân hoặc con số tương lai.
                aiAdvisorUsage chỉ là số liệu tổng hợp ẩn danh; dùng để đánh giá mức khách tương tác với tư vấn AI.
                Mọi kết luận phải gắn với evidence có số liệu trong JSON.
                Có thể dự đoán HƯỚNG tăng/đi ngang/giảm của sản phẩm, nhưng phải dùng UNCERTAIN khi dữ liệu yếu.
                Nếu changePercent là null, hiểu là kỳ trước bằng 0; không tự biến thành phần trăm tăng.
                Viết tiếng Việt rõ, ngắn, phù hợp người quản lý cửa hàng.
                Chỉ nhắc productId có trong productTrends.
                Tối đa 4 signals, 3 actions và 5 productOutlooks.
                Trả JSON thuần, không Markdown, đúng cấu trúc:
                {
                  "headline":"một kết luận nổi bật",
                  "executiveSummary":"tóm tắt 2-3 câu có dẫn chứng",
                  "outlook":"GROWTH|STEADY|DECLINE|UNCERTAIN",
                  "confidence":"HIGH|MEDIUM|LOW",
                  "signals":[
                    {"title":"...","evidence":"...","interpretation":"...","severity":"POSITIVE|INFO|WARNING|CRITICAL"}
                  ],
                  "actions":[
                    {"title":"...","reason":"...","priority":"HIGH|MEDIUM|LOW"}
                  ],
                  "productOutlooks":[
                    {"productId":1,"direction":"UP|STEADY|DOWN|UNCERTAIN","reason":"..."}
                  ]
                }
                """;
    }

    private String cacheModelName() {
        return METRIC_SCHEMA_VERSION + "|" + model;
    }

    // Marcus thêm: structured output khóa hình dạng báo cáo ngay từ Gemini,
    // thay vì chỉ hy vọng model làm đúng phần mô tả trong prompt.
    private Map<String, Object> analyticsResponseSchema() {
        Map<String, Object> signal = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "evidence", Map.of("type", "string"),
                        "interpretation", Map.of("type", "string"),
                        "severity", Map.of("type", "string", "enum", List.copyOf(SEVERITIES))),
                "required", List.of("title", "evidence", "interpretation", "severity"));
        Map<String, Object> action = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "reason", Map.of("type", "string"),
                        "priority", Map.of("type", "string", "enum", List.copyOf(PRIORITIES))),
                "required", List.of("title", "reason", "priority"));
        Map<String, Object> productOutlook = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "productId", Map.of("type", "integer"),
                        "direction", Map.of("type", "string", "enum", List.copyOf(DIRECTIONS)),
                        "reason", Map.of("type", "string")),
                "required", List.of("productId", "direction", "reason"));

        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "headline", Map.of("type", "string"),
                        "executiveSummary", Map.of("type", "string"),
                        "outlook", Map.of("type", "string", "enum", List.copyOf(OUTLOOKS)),
                        "confidence", Map.of("type", "string", "enum", List.copyOf(PRIORITIES)),
                        "signals", Map.of("type", "array", "maxItems", 4, "items", signal),
                        "actions", Map.of("type", "array", "maxItems", 3, "items", action),
                        "productOutlooks", Map.of(
                                "type", "array",
                                "maxItems", 5,
                                "items", productOutlook)),
                "required", List.of(
                        "headline",
                        "executiveSummary",
                        "outlook",
                        "confidence",
                        "signals",
                        "actions",
                        "productOutlooks"));
    }

    private AiAnalyticsReportResponse parseReport(
            JsonNode response,
            List<ProductTrendResponse> products,
            LocalDateTime generatedAt,
            LocalDateTime cachedUntil) {
        String output = extractOutputText(response);
        try {
            JsonNode root = objectMapper.readTree(output);
            Map<Integer, ProductTrendResponse> allowedProducts = products.stream()
                    .collect(Collectors.toMap(ProductTrendResponse::productId, Function.identity()));

            List<Signal> signals = new ArrayList<>();
            root.path("signals").forEach(node -> {
                if (signals.size() < 4) {
                    signals.add(new Signal(
                            safeText(node, "title", 100),
                            safeText(node, "evidence", 180),
                            safeText(node, "interpretation", 240),
                            allowedValue(node.path("severity").asText(), SEVERITIES, "INFO")));
                }
            });

            List<Action> actions = new ArrayList<>();
            root.path("actions").forEach(node -> {
                if (actions.size() < 3) {
                    actions.add(new Action(
                            safeText(node, "title", 120),
                            safeText(node, "reason", 240),
                            allowedValue(node.path("priority").asText(), PRIORITIES, "MEDIUM")));
                }
            });

            List<ProductOutlook> productOutlooks = new ArrayList<>();
            root.path("productOutlooks").forEach(node -> {
                if (productOutlooks.size() >= 5) {
                    return;
                }
                int productId = node.path("productId").asInt(-1);
                ProductTrendResponse product = allowedProducts.get(productId);
                if (product != null) {
                    productOutlooks.add(new ProductOutlook(
                            productId,
                            product.productName(),
                            allowedValue(node.path("direction").asText(), DIRECTIONS, "UNCERTAIN"),
                            safeText(node, "reason", 240)));
                }
            });

            return new AiAnalyticsReportResponse(
                    generatedAt,
                    cachedUntil,
                    false,
                    requiredText(root, "headline", 180),
                    requiredText(root, "executiveSummary", 600),
                    allowedValue(root.path("outlook").asText(), OUTLOOKS, "UNCERTAIN"),
                    allowedValue(root.path("confidence").asText(), PRIORITIES, "LOW"),
                    List.copyOf(signals),
                    List.copyOf(actions),
                    List.copyOf(productOutlooks),
                    "Nhận định AI dựa trên dữ liệu tổng hợp và không thay thế quyết định của chủ cửa hàng.");
        } catch (Exception exception) {
            throw new IllegalStateException("Gemini trả về bản phân tích chưa đúng định dạng.");
        }
    }

    private String extractOutputText(JsonNode response) {
        String text = response == null
                ? ""
                : response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        if (text.isBlank()) {
            throw new IllegalStateException("Gemini không trả về nội dung phân tích.");
        }
        String normalized = text
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
        // Marcus sửa: chấp nhận trường hợp provider bọc JSON bằng một câu dẫn.
        int firstBrace = normalized.indexOf('{');
        int lastBrace = normalized.lastIndexOf('}');
        return firstBrace >= 0 && lastBrace > firstBrace
                ? normalized.substring(firstBrace, lastBrace + 1)
                : normalized;
    }

    private String requiredText(JsonNode node, String field, int maxLength) {
        String value = safeText(node, field, maxLength);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Thiếu trường " + field);
        }
        return value;
    }

    private String safeText(JsonNode node, String field, int maxLength) {
        String value = node.path(field).asText("").replaceAll("\\s+", " ").trim();
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String allowedValue(String value, Set<String> allowed, String fallback) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        return allowed.contains(normalized) ? normalized : fallback;
    }

    private String providerErrorMessage(HttpStatusCodeException exception) {
        return switch (exception.getStatusCode().value()) {
            case 400 -> "Gemini từ chối dữ liệu phân tích hoặc cấu hình model chưa hợp lệ.";
            case 401, 403 -> "Gemini API key không hợp lệ hoặc chưa được cấp quyền.";
            case 404 -> "Model Gemini phân tích không còn khả dụng.";
            case 429 -> "Đã chạm quota Gemini miễn phí. Hãy dùng lại báo cáo cache hoặc thử sau.";
            default -> "Dịch vụ Gemini đang gián đoạn. Vui lòng thử lại sau.";
        };
    }

    private record CacheKey(LocalDate fromDate, LocalDate toDate) {
    }

}
