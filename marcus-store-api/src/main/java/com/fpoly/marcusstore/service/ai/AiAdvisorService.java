package com.fpoly.marcusstore.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpoly.marcusstore.dto.ai.AiAdvisorRequest;
import com.fpoly.marcusstore.dto.ai.AiAdvisorResponse;
import com.fpoly.marcusstore.repository.core.HomeProductRepository;
import com.fpoly.marcusstore.repository.core.HomeProductRepository.AiProductProjection;
import com.fpoly.marcusstore.repository.core.HomeProductRepository.AiProductSpecProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import java.text.NumberFormat;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiAdvisorService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern MILLION_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(?:triệu|trieu)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final List<String> PHONE_BRANDS = List.of(
            "iphone", "apple", "samsung", "xiaomi", "oppo", "vivo", "realme", "nokia", "honor");

    private final HomeProductRepository homeProductRepository;

    @Value("${gemini.api-key:}")
    private String apiKey;

    // dùng Flash Lite để phù hợp quota miễn phí
    @Value("${gemini.model:gemini-3.5-flash-lite}")
    private String model;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    public AiAdvisorResponse advise(AiAdvisorRequest request) {
        AiAdvisorResponse knownAnswer = answerKnownStoreQuestion(request.getMessage());
        if (knownAnswer != null) {
            return knownAnswer;
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "AI chưa được cấu hình. Admin cần thiết lập GEMINI_API_KEY.");
        }

        ProductSearchCriteria criteria = analyzeProductRequest(request.getMessage());
        List<AiProductProjection> products = findProducts(criteria);
        // Marcus sửa: nếu khách gọi đúng dòng máy nhưng dữ liệu không khớp, chỉ nới
        // từ khóa trong cùng danh mục; tuyệt đối không fallback sang phụ kiện.
        if (products.isEmpty() && !criteria.keyword().isBlank()) {
            products = homeProductRepository.findProductsForAiAdvisor(
                    "", criteria.categoryKeyword(), criteria.minPrice(), criteria.maxPrice(), criteria.targetPrice());
        }
        Map<Integer, String> productSpecs = loadProductSpecs(products);
        String input = buildInput(request, products, productSpecs);

        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofSeconds(5));
            requestFactory.setReadTimeout(Duration.ofSeconds(25));

            JsonNode response = RestClient.builder()
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
                                    "parts", List.of(Map.of("text", input)))),
                            "generationConfig", Map.of(
                                    "maxOutputTokens", 900,
                                    "responseMimeType", "application/json")))
                    .retrieve()
                    .body(JsonNode.class);

            return buildAdvisorResponse(response, products);
        } catch (HttpStatusCodeException exception) {
            // trả hướng xử lý an toàn nhưng không làm lộ key hay response nội bộ.
            throw new IllegalStateException(providerErrorMessage(exception));
        } catch (RestClientException exception) {
            throw new IllegalStateException("Không thể kết nối dịch vụ AI. Vui lòng thử lại sau.");
        }
    }

    private String providerErrorMessage(HttpStatusCodeException exception) {
        return switch (exception.getStatusCode().value()) {
            case 400 -> "Gemini từ chối yêu cầu. Hãy kiểm tra API key và model trong application.properties.";
            case 401, 403 -> "Gemini API key không hợp lệ hoặc chưa được cấp quyền sử dụng.";
            case 404 -> "Model Gemini đang cấu hình không còn khả dụng. Hãy kiểm tra GEMINI_MODEL.";
            case 429 -> "Marcus AI đã chạm giới hạn miễn phí. Vui lòng chờ một lúc rồi thử lại.";
            default -> exception.getStatusCode().is4xxClientError()
                    ? "Cấu hình hoặc yêu cầu gửi tới Gemini chưa hợp lệ."
                    : "Dịch vụ Gemini đang gián đoạn. Vui lòng thử lại sau.";
        };
    }

    // thông tin cố định của cửa hàng trả ngay, không tốn token và không thể bị AI
    // bịa.
    private AiAdvisorResponse answerKnownStoreQuestion(String rawMessage) {
        String message = rawMessage.toLowerCase(Locale.ROOT);
        if (message.contains("địa chỉ")
                || (message.contains("ở đâu")
                        && (message.contains("marcus") || message.contains("cửa hàng")))) {
            return fixedAnswer("Dạ, Marcus Store ở địa chỉ 118 Cát Bi, Hải An, Hải Phòng ạ.");
        }
        if (message.contains("nhận hàng tại") || message.contains("nhận tại cửa hàng")
                || message.contains("đến cửa hàng nhận")) {
            return fixedAnswer(
                    "Dạ được ạ. Bạn có thể chọn “Nhận hàng tại cửa hàng” khi thanh toán và đến 118 Cát Bi, Hải An, Hải Phòng để nhận sản phẩm.");
        }
        return null;
    }

    private AiAdvisorResponse fixedAnswer(String answer) {
        return AiAdvisorResponse.builder()
                .answer(answer)
                .products(List.of())
                .build();
    }

    private String systemInstructions() {
        return """
                Bạn là Marcus AI, trợ lý tư vấn bán hàng 24/7 của Marcus Store.
                Luôn trả lời bằng tiếng Việt, thân thiện, rõ ràng, tối đa khoảng 120 từ.
                Viết văn bản thuần, không dùng Markdown như **, # hoặc bảng vì khung chat không render Markdown.
                Marcus Store chỉ có một địa chỉ: 118 Cát Bi, Hải An, Hải Phòng.
                Chỉ dùng dữ liệu sản phẩm được cung cấp trong NGỮ CẢNH SẢN PHẨM.
                Không bịa giá, tồn kho, khuyến mãi, cấu hình, bảo hành hoặc chính sách.
                Khi ngữ cảnh không đủ, nói rõ cần nhân viên kiểm tra và gợi ý khách dùng Live Chat.
                Không yêu cầu mật khẩu, OTP, số thẻ hay thông tin thanh toán nhạy cảm.
                Hãy phân tích nhu cầu, ngân sách và điểm khác nhau giữa các lựa chọn; không dùng lời quảng cáo chung chung.
                Chỉ được đề xuất sản phẩm có trong NGỮ CẢNH SẢN PHẨM và không tự suy đoán thông số chưa được cung cấp.
                Chọn tối đa 3 sản phẩm phù hợp nhất. recommendedProductIds phải chứa đúng ID của các sản phẩm được nhắc đến.
                Trả JSON hợp lệ theo mẫu: {"answer":"nội dung tư vấn","recommendedProductIds":[1,2]}.
                Bạn là AI, không tự nhận mình là nhân viên hoặc Admin.
                """;
    }

    private String buildInput(
            AiAdvisorRequest request,
            List<AiProductProjection> products,
            Map<Integer, String> productSpecs) {
        String history = request.getHistory() == null ? ""
                : request.getHistory().stream()
                        .filter(turn -> "user".equals(turn.getRole()) || "assistant".equals(turn.getRole()))
                        .map(turn -> turn.getRole() + ": " + turn.getContent())
                        .collect(Collectors.joining("\n"));

        String productContext = products.isEmpty()
                ? "Không tìm thấy sản phẩm liên quan trong dữ liệu hiện tại."
                : products.stream()
                        .map(product -> formatProduct(product, productSpecs.get(product.getProductId())))
                        .collect(Collectors.joining("\n"));

        return """
                LỊCH SỬ GẦN NHẤT:
                %s

                NGỮ CẢNH SẢN PHẨM:
                %s

                CÂU HỎI HIỆN TẠI:
                %s
                """.formatted(history, productContext, request.getMessage().trim());
    }

    private String formatProduct(AiProductProjection product, String specs) {
        String price = product.getPrice() == null
                ? "chưa có giá"
                : NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(product.getPrice()) + " VND";
        return "- ID %s | %s | hãng %s | danh mục %s | giá từ %s | %s | thông số %s | mô tả %s".formatted(
                product.getProductId(),
                product.getProductName(),
                valueOrUnknown(product.getBrand()),
                valueOrUnknown(product.getParentCategoryName() != null
                        ? product.getParentCategoryName() + " / " + product.getCategoryName()
                        : product.getCategoryName()),
                price,
                product.getStockQuantity() != null && product.getStockQuantity() > 0 ? "còn hàng" : "tạm hết hàng",
                valueOrUnknown(specs),
                sanitizeDescription(product.getDescription()));
    }

    private Map<Integer, String> loadProductSpecs(List<AiProductProjection> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        List<Integer> productIds = products.stream().map(AiProductProjection::getProductId).toList();
        List<AiProductSpecProjection> rows = homeProductRepository.findProductSpecsForAiAdvisor(productIds);
        Map<Integer, List<String>> groupedSpecs = new HashMap<>();
        for (AiProductSpecProjection row : rows) {
            List<String> specs = groupedSpecs.computeIfAbsent(row.getProductId(), ignored -> new ArrayList<>());
            // Marcus giới hạn ngữ cảnh để tiết kiệm quota miễn phí nhưng vẫn đủ dữ
            // liệu quan trọng cho tư vấn.
            if (specs.size() < 10) {
                String unit = row.getUnit() == null || row.getUnit().isBlank() ? "" : " " + row.getUnit();
                specs.add(row.getSpecName() + ": " + row.getSpecValue() + unit);
            }
        }
        return groupedSpecs.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> String.join(", ", entry.getValue())));
    }

    private List<AiProductProjection> findProducts(ProductSearchCriteria criteria) {
        return homeProductRepository.findProductsForAiAdvisor(
                criteria.keyword(),
                criteria.categoryKeyword(),
                criteria.minPrice(),
                criteria.maxPrice(),
                criteria.targetPrice());
    }

    // Marcus thêm: hiểu loại hàng, hãng/dòng máy và cách nói ngân sách phổ biến.
    private ProductSearchCriteria analyzeProductRequest(String rawMessage) {
        String message = rawMessage.toLowerCase(Locale.forLanguageTag("vi-VN"));
        boolean accessoryIntent = message.matches(".*(phụ kiện|ốp lưng|bao da|sạc|cáp|tai nghe|kính cường lực).*");
        String categoryKeyword = accessoryIntent ? "phụ kiện" : "điện thoại";

        String keyword = PHONE_BRANDS.stream()
                .filter(message::contains)
                .findFirst()
                .orElse("");
        // Trong DB, iPhone nằm ở tên sản phẩm còn brand thường là Apple.
        if ("apple".equals(keyword) && message.contains("iphone")) {
            keyword = "iphone";
        }

        Matcher budgetMatcher = MILLION_PATTERN.matcher(message);
        BigDecimal targetPrice = null;
        if (budgetMatcher.find()) {
            targetPrice = new BigDecimal(budgetMatcher.group(1).replace(',', '.'))
                    .multiply(BigDecimal.valueOf(1_000_000));
        }

        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        if (targetPrice != null) {
            if (message.matches(".*(dưới|không quá|tối đa|tầm|khoảng|trong khoảng|ngân sách).*")) {
                maxPrice = targetPrice;
            } else if (message.matches(".*(trên|từ).*")) {
                minPrice = targetPrice;
            } else {
                maxPrice = targetPrice;
            }
        }
        return new ProductSearchCriteria(keyword, categoryKeyword, minPrice, maxPrice, targetPrice);
    }

    private AiAdvisorResponse buildAdvisorResponse(JsonNode response, List<AiProductProjection> products) {
        String output = extractOutputText(response);
        try {
            JsonNode result = OBJECT_MAPPER.readTree(output);
            String answer = result.path("answer").asText("").trim();
            Set<Integer> recommendedIds = new LinkedHashSet<>();
            result.path("recommendedProductIds").forEach(id -> {
                if (id.canConvertToInt()) {
                    recommendedIds.add(id.asInt());
                }
            });

            List<AiAdvisorResponse.ProductSuggestion> suggestions = products.stream()
                    .filter(product -> recommendedIds.contains(product.getProductId()))
                    .limit(3)
                    .map(this::toSuggestion)
                    .toList();

            return AiAdvisorResponse.builder()
                    .answer(answer.isBlank() ? "Mình chưa tìm được lựa chọn đủ phù hợp." : answer)
                    .products(suggestions)
                    .build();
        } catch (Exception ignored) {
            // Marcus giữ fallback để widget vẫn trả lời nếu nhà cung cấp lỡ không
            // tuân thủ JSON, nhưng không gắn thẻ sản phẩm sai.
            return AiAdvisorResponse.builder()
                    .answer(output)
                    .products(List.of())
                    .build();
        }
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("AI không trả về nội dung.");
        }
        for (JsonNode candidate : response.path("candidates")) {
            for (JsonNode part : candidate.path("content").path("parts")) {
                if (!part.path("text").asText().isBlank()) {
                    return part.path("text").asText();
                }
            }
        }
        throw new IllegalStateException("AI không trả về nội dung phù hợp.");
    }

    private String sanitizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return "chưa có mô tả chi tiết";
        }
        String plainText = description
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return plainText.length() <= 240 ? plainText : plainText.substring(0, 240) + "...";
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "chưa cập nhật" : value;
    }

    private record ProductSearchCriteria(
            String keyword,
            String categoryKeyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal targetPrice) {
    }

    private AiAdvisorResponse.ProductSuggestion toSuggestion(AiProductProjection product) {
        return AiAdvisorResponse.ProductSuggestion.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .slug(product.getSlug())
                .thumbnailUrl(product.getThumbnailUrl())
                .price(product.getPrice())
                .inStock(product.getStockQuantity() != null && product.getStockQuantity() > 0)
                .build();
    }
}
