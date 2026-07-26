package com.fpoly.marcusstore.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fpoly.marcusstore.dto.ai.AiAdvisorRequest;
import com.fpoly.marcusstore.dto.ai.AiAdvisorResponse;
import com.fpoly.marcusstore.repository.core.HomeProductRepository;
import com.fpoly.marcusstore.repository.core.HomeProductRepository.AiProductProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.text.NumberFormat;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiAdvisorService {

    private static final Set<String> STOP_WORDS = Set.of(
            "marcus", "store", "giúp", "mình", "tôi", "bạn", "sản", "phẩm", "có", "không",
            "cho", "với", "muốn", "cần", "tư", "vấn", "kiểm", "tra", "giá", "còn", "hàng");

    private final HomeProductRepository homeProductRepository;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-5-mini}")
    private String model;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    public AiAdvisorResponse advise(AiAdvisorRequest request) {
        AiAdvisorResponse knownAnswer = answerKnownStoreQuestion(request.getMessage());
        if (knownAnswer != null) {
            return knownAnswer;
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "AI chưa được cấu hình. Admin cần thiết lập biến môi trường OPENAI_API_KEY.");
        }

        String keyword = extractSearchKeyword(request.getMessage());
        List<AiProductProjection> products = homeProductRepository.findProductsForAiAdvisor(keyword);
        // Marcus thêm: câu hỏi theo nhu cầu/ngân sách không có tên máy vẫn nhận được
        // catalog gợi ý.
        if (products.isEmpty() && !keyword.isBlank()) {
            products = homeProductRepository.findProductsForAiAdvisor("");
        }
        String input = buildInput(request, products);

        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofSeconds(5));
            requestFactory.setReadTimeout(Duration.ofSeconds(25));

            JsonNode response = RestClient.builder()
                    .baseUrl(baseUrl)
                    .requestFactory(requestFactory)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .build()
                    .post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "instructions", systemInstructions(),
                            "input", input,
                            "max_output_tokens", 700,
                            "reasoning", Map.of("effort", "minimal"),
                            "store", false))
                    .retrieve()
                    .body(JsonNode.class);

            return AiAdvisorResponse.builder()
                    .answer(extractOutputText(response))
                    .products(products.stream().limit(3).map(this::toSuggestion).toList())
                    .build();
        } catch (RestClientException exception) {
            // Marcus sửa: không trả chi tiết API key/provider về client.
            throw new IllegalStateException("AI đang bận hoặc mất kết nối. Vui lòng thử lại sau.");
        }
    }

    // Marcus thêm: thông tin cố định của cửa hàng trả ngay, không tốn token và
    // không thể bị AI bịa.
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
                Marcus Store chỉ có một địa chỉ: 118 Cát Bi, Hải An, Hải Phòng.
                Chỉ dùng dữ liệu sản phẩm được cung cấp trong NGỮ CẢNH SẢN PHẨM.
                Không bịa giá, tồn kho, khuyến mãi, cấu hình, bảo hành hoặc chính sách.
                Khi ngữ cảnh không đủ, nói rõ cần nhân viên kiểm tra và gợi ý khách dùng Live Chat.
                Không yêu cầu mật khẩu, OTP, số thẻ hay thông tin thanh toán nhạy cảm.
                Nếu đề xuất sản phẩm, nêu tối đa 3 lựa chọn và lý do ngắn gọn.
                Bạn là AI, không tự nhận mình là nhân viên hoặc Admin.
                """;
    }

    private String buildInput(AiAdvisorRequest request, List<AiProductProjection> products) {
        String history = request.getHistory() == null ? ""
                : request.getHistory().stream()
                        .filter(turn -> "user".equals(turn.getRole()) || "assistant".equals(turn.getRole()))
                        .map(turn -> turn.getRole() + ": " + turn.getContent())
                        .collect(Collectors.joining("\n"));

        String productContext = products.isEmpty()
                ? "Không tìm thấy sản phẩm liên quan trong dữ liệu hiện tại."
                : products.stream().map(this::formatProduct).collect(Collectors.joining("\n"));

        return """
                LỊCH SỬ GẦN NHẤT:
                %s

                NGỮ CẢNH SẢN PHẨM:
                %s

                CÂU HỎI HIỆN TẠI:
                %s
                """.formatted(history, productContext, request.getMessage().trim());
    }

    private String formatProduct(AiProductProjection product) {
        String price = product.getPrice() == null
                ? "chưa có giá"
                : NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(product.getPrice()) + " VND";
        return "- %s | giá từ %s | %s | đường dẫn /product/%s".formatted(
                product.getProductName(),
                price,
                product.getStockQuantity() != null && product.getStockQuantity() > 0 ? "còn hàng" : "tạm hết hàng",
                product.getSlug());
    }

    private String extractSearchKeyword(String message) {
        return List.of(message.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}\\s]", " ").split("\\s+"))
                .stream()
                .filter(word -> word.length() >= 3 && !STOP_WORDS.contains(word))
                .findFirst()
                .orElse("");
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("AI không trả về nội dung.");
        }
        for (JsonNode output : response.path("output")) {
            if (!"message".equals(output.path("type").asText())) {
                continue;
            }
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())
                        && !content.path("text").asText().isBlank()) {
                    return content.path("text").asText();
                }
            }
        }
        throw new IllegalStateException("AI không trả về nội dung phù hợp.");
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
