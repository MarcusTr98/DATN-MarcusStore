package com.fpoly.marcusstore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fpoly.marcusstore.dto.request.ProductComparisonRequest;
import com.fpoly.marcusstore.dto.response.ProductComparisonResponse;
import com.fpoly.marcusstore.dto.response.ProductComparisonResponse.*;
import com.fpoly.marcusstore.repository.core.HomeProductRepository;
import com.fpoly.marcusstore.repository.core.HomeProductRepository.AiProductSpecProjection;
import com.fpoly.marcusstore.repository.core.HomeProductRepository.HomeProductRawProjection;
import com.fpoly.marcusstore.service.ai.GeminiClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductComparisonService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String COMPARISON_PROMPT_TEMPLATE = """
            Bạn là chuyên gia tư vấn điện thoại. Dưới đây là thông số kỹ thuật của %d sản phẩm dạng JSON
            (mỗi object có "product_id" để bạn tham chiếu khi trả lời):

            %s

            NHIỆM VỤ 1 - So sánh tổng thể và theo nhu cầu sử dụng (như cũ).

            NHIỆM VỤ 2 - So sánh TỪNG thông số kỹ thuật (spec_winners):
            Với mỗi tên spec xuất hiện trong dữ liệu (bỏ qua "product_id", "product_name", "price"),
            nếu các sản phẩm có giá trị KHÁC NHAU và bạn xác định được rõ ràng sản phẩm nào vượt trội hơn,
            hãy thêm 1 entry vào "spec_winners" với "winner_product_id" là product_id của sản phẩm thắng.

            Quy tắc xác định chiều tốt/xấu (RẤT QUAN TRỌNG, áp dụng đúng chiều):
            - RAM, ROM/bộ nhớ trong, dung lượng pin, độ phân giải camera (MP), tốc độ sạc (W), tần số quét màn hình (Hz), độ sáng màn hình (nits): giá trị CÀNG CAO CÀNG TỐT.
            - Trọng lượng máy, độ dày máy: giá trị CÀNG THẤP CÀNG TỐT (máy nhẹ/mỏng hơn là lợi thế).
            - Chip xử lý, độ bền/kháng nước (IP rating), chất liệu khung, loại màn hình (OLED/LCD...): đây là giá trị định tính, chỉ chấm thắng khi bạn chắc chắn dựa trên kiến thức phổ thông (ví dụ IP68 > IP54), KHÔNG suy đoán nếu không chắc.
            - Nếu 1 spec không thể so sánh khách quan được (ví dụ màu sắc, tên mã sản phẩm) hoặc bạn không đủ tự tin, BỎ QUA spec đó, không thêm vào spec_winners.
            - Không tự bịa thêm spec ngoài dữ liệu được cung cấp.

            Trả lời CHỈ bằng JSON hợp lệ theo đúng schema bên dưới, không thêm markdown, không thêm text giải thích ngoài JSON.

            Schema bắt buộc:
            {
              "overall_winner": "tên sản phẩm tổng thể tốt nhất",
              "overall_reason": "1-2 câu giải thích ngắn gọn",
              "use_cases": [
                {
                  "use_case": "Gaming / hiệu năng",
                  "winner": "tên sản phẩm",
                  "reason": "1 câu, dựa trên chip/RAM cụ thể trong data"
                },
                {
                  "use_case": "Chụp ảnh",
                  "winner": "tên sản phẩm",
                  "reason": "1 câu, dựa trên camera cụ thể trong data"
                },
                {
                  "use_case": "Thời lượng pin",
                  "winner": "tên sản phẩm",
                  "reason": "1 câu, dựa trên dung lượng pin cụ thể trong data"
                },
                {
                  "use_case": "Giá trị / giá tiền",
                  "winner": "tên sản phẩm",
                  "reason": "1 câu, so sánh giá và cấu hình nhận được"
                }
              ],
              "spec_winners": [
                {
                  "spec_name": "tên spec ĐÚNG như key trong dữ liệu JSON ở trên",
                  "winner_product_id": 123,
                  "reason": "1 câu ngắn giải thích tại sao vượt trội"
                }
              ]
            }
            """;

    private static final String SYSTEM_INSTRUCTION = """
            Bạn là chuyên gia tư vấn công nghệ, chuyên so sánh điện thoại và thiết bị di động.
            Luôn phân tích khách quan dựa trên dữ liệu thực tế, không bịa đặt thông số.
            Trả lời ngắn gọn, dễ hiểu cho người dùng phổ thông.
            """;

    private List<SpecWinner> parseSpecWinners(JsonNode node) {
        List<SpecWinner> results = new ArrayList<>();
        JsonNode specWinners = node.get("spec_winners");
        if (specWinners == null || !specWinners.isArray()) {
            return results; // rỗng cũng OK, FE sẽ tự fallback
        }
        for (JsonNode sw : specWinners) {
            JsonNode idNode = sw.get("winner_product_id");
            if (idNode == null || idNode.isNull())
                continue; // bỏ qua entry thiếu id
            results.add(SpecWinner.builder()
                    .specName(getText(sw, "spec_name", ""))
                    .winnerProductId(idNode.asInt())
                    .reason(getText(sw, "reason", ""))
                    .build());
        }
        return results;
    }

    private static final Map<String, Object> RESPONSE_SCHEMA;

    static {
        RESPONSE_SCHEMA = new LinkedHashMap<>();
        RESPONSE_SCHEMA.put("type", "object");
        RESPONSE_SCHEMA.put("properties", Map.of(
                "overall_winner", Map.of("type", "string"),
                "overall_reason", Map.of("type", "string"),
                "use_cases", Map.of("type", "array", "items", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "use_case", Map.of("type", "string"),
                                "winner", Map.of("type", "string"),
                                "reason", Map.of("type", "string")),
                        "required", List.of("use_case", "winner", "reason"))),
                "spec_winners", Map.of("type", "array", "items", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "spec_name", Map.of("type", "string"),
                                "winner_product_id", Map.of("type", "integer"),
                                "reason", Map.of("type", "string")),
                        "required", List.of("spec_name", "winner_product_id")))));
        RESPONSE_SCHEMA.put("required", List.of("overall_winner", "overall_reason", "use_cases", "spec_winners"));
    }

    private final HomeProductRepository homeProductRepository;
    private final GeminiClient geminiClient;

    public ProductComparisonService(HomeProductRepository homeProductRepository, GeminiClient geminiClient) {
        this.homeProductRepository = homeProductRepository;
        this.geminiClient = geminiClient;
    }

    public ProductComparisonResponse compareProducts(ProductComparisonRequest request) {
        List<Integer> productIds = request.getProductIds();

        List<HomeProductRawProjection> productData = homeProductRepository.findSkuOverviewByProductIds(productIds);

        Map<Integer, HomeProductRawProjection> productMap = productData.stream()
                .collect(Collectors.toMap(HomeProductRawProjection::getProductId, p -> p));

        List<AiProductSpecProjection> rawSpecs = homeProductRepository.findProductSpecsForAiAdvisor(productIds);
        Map<Integer, List<SpecItem>> specsByProduct = rawSpecs.stream()
                .collect(Collectors.groupingBy(
                        AiProductSpecProjection::getProductId,
                        Collectors.mapping(p -> SpecItem.builder()
                                .specName(p.getSpecName())
                                .specValue(p.getSpecValue())
                                .unit(p.getUnit())
                                .build(), Collectors.toList())));

        List<ComparedProduct> comparedProducts = productIds.stream()
                .map(id -> {
                    HomeProductRawProjection p = productMap.get(id);
                    List<SpecItem> specs = specsByProduct.getOrDefault(id, List.of());
                    return ComparedProduct.builder()
                            .productId(id)
                            .productName(p != null ? p.getProductName() : "Sản phẩm #" + id)
                            .thumbnailUrl(p != null ? p.getThumbnailUrl() : null)
                            .slug(p != null ? p.getSlug() : null)
                            .price(p != null ? p.getPrice() : null)
                            .originalPrice(p != null ? p.getOriginalPrice() : null)
                            .defaultSkuId(p != null ? p.getSkuId() : null)
                            .specs(specs)
                            .build();
                })
                .toList();

        ComparisonResult comparisonResult = buildComparisonResult(comparedProducts);

        return ProductComparisonResponse.builder()
                .products(comparedProducts)
                .result(comparisonResult)
                .build();
    }

    private ComparisonResult buildComparisonResult(List<ComparedProduct> products) {
        String jsonData = buildSpecsJson(products);

        int count = products.size();
        String prompt = String.format(COMPARISON_PROMPT_TEMPLATE, count, jsonData);

        if (!geminiClient.isConfigured()) {
            return buildFallbackResult(products);
        }

        try {
            JsonNode aiResult = geminiClient.generate(
                    SYSTEM_INSTRUCTION,
                    prompt,
                    RESPONSE_SCHEMA,
                    2048);

            JsonNode candidates = aiResult.get("candidates");
            if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
                return buildFallbackResult(products);
            }

            JsonNode content = candidates.get(0).get("content");
            if (content == null) {
                return buildFallbackResult(products);
            }

            JsonNode parts = content.get("parts");
            if (parts == null || !parts.isArray() || parts.isEmpty()) {
                return buildFallbackResult(products);
            }

            String rawText = parts.get(0).has("text") ? parts.get(0).get("text").asText() : "";
            JsonNode resultNode = parseJsonSafely(rawText);

            if (resultNode == null) {
                return buildFallbackResult(products);
            }

            return ComparisonResult.builder()
                    .overallWinner(getText(resultNode, "overall_winner", "Không xác định"))
                    .overallReason(getText(resultNode, "overall_reason", ""))
                    .useCases(parseUseCases(resultNode))
                    .specWinners(parseSpecWinners(resultNode))
                    .build();

        } catch (Exception e) {
            return buildFallbackResult(products);
        }
    }

    private String buildSpecsJson(List<ComparedProduct> products) {
        NumberFormat nf = NumberFormat.getInstance(Locale.GERMAN);
        List<Map<String, Object>> list = new ArrayList<>();

        for (ComparedProduct p : products) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("product_id", p.getProductId());
            item.put("product_name", p.getProductName());
            item.put("price", p.getPrice() != null ? nf.format(p.getPrice()) : null);
            if (p.getSpecs() != null) {
                for (SpecItem spec : p.getSpecs()) {
                    String value = spec.getSpecValue() != null ? spec.getSpecValue() : "";
                    if (spec.getUnit() != null && !spec.getUnit().isBlank()) {
                        value += " " + spec.getUnit();
                    }
                    item.put(spec.getSpecName(), value);
                }
            }
            list.add(item);
        }

        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private JsonNode parseJsonSafely(String raw) {
        if (raw == null || raw.isBlank())
            return null;
        try {
            String cleaned = raw.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            }
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            int firstBrace = cleaned.indexOf('{');
            int lastBrace = cleaned.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                cleaned = cleaned.substring(firstBrace, lastBrace + 1);
            }
            return OBJECT_MAPPER.readTree(cleaned);
        } catch (Exception e) {
            return null;
        }
    }

    private String getText(JsonNode node, String field, String fallback) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? n.asText() : fallback;
    }

    private List<UseCaseResult> parseUseCases(JsonNode node) {
        List<UseCaseResult> results = new ArrayList<>();
        JsonNode useCases = node.get("use_cases");
        if (useCases == null || !useCases.isArray()) {
            return getDefaultUseCases();
        }
        for (JsonNode uc : useCases) {
            results.add(UseCaseResult.builder()
                    .useCase(getText(uc, "use_case", ""))
                    .winner(getText(uc, "winner", ""))
                    .reason(getText(uc, "reason", ""))
                    .build());
        }
        return results.isEmpty() ? getDefaultUseCases() : results;
    }

    private List<UseCaseResult> getDefaultUseCases() {
        return List.of(
                UseCaseResult.builder()
                        .useCase("Gaming / hiệu năng")
                        .winner("")
                        .reason("Không đủ dữ liệu để đánh giá.")
                        .build(),
                UseCaseResult.builder()
                        .useCase("Chụp ảnh")
                        .winner("")
                        .reason("Không đủ dữ liệu để đánh giá.")
                        .build(),
                UseCaseResult.builder()
                        .useCase("Thời lượng pin")
                        .winner("")
                        .reason("Không đủ dữ liệu để đánh giá.")
                        .build(),
                UseCaseResult.builder()
                        .useCase("Giá trị / giá tiền")
                        .winner("")
                        .reason("Không đủ dữ liệu để đánh giá.")
                        .build());
    }

    private ComparisonResult buildFallbackResult(List<ComparedProduct> products) {
        if (products.size() < 2) {
            return ComparisonResult.builder()
                    .overallWinner("Không đủ sản phẩm")
                    .overallReason("")
                    .useCases(getDefaultUseCases())
                    .build();
        }

        ComparedProduct cheapest = products.stream()
                .filter(p -> p.getPrice() != null)
                .min(Comparator.comparing(ComparedProduct::getPrice))
                .orElse(products.get(0));

        ComparedProduct mostExpensive = products.stream()
                .filter(p -> p.getPrice() != null)
                .max(Comparator.comparing(ComparedProduct::getPrice))
                .orElse(products.get(products.size() - 1));

        List<UseCaseResult> fallback = new ArrayList<>();
        fallback.add(UseCaseResult.builder()
                .useCase("Gaming / hiệu năng")
                .winner("")
                .reason("Vui lòng tham khảo chi tiết từng sản phẩm.")
                .build());
        fallback.add(UseCaseResult.builder()
                .useCase("Chụp ảnh")
                .winner("")
                .reason("Vui lòng tham khảo chi tiết từng sản phẩm.")
                .build());
        fallback.add(UseCaseResult.builder()
                .useCase("Thời lượng pin")
                .winner("")
                .reason("Vui lòng tham khảo chi tiết từng sản phẩm.")
                .build());
        fallback.add(UseCaseResult.builder()
                .useCase("Giá trị / giá tiền")
                .winner(cheapest.getProductName())
                .reason(String.format("%s có giá thấp nhất trong nhóm so sánh.",
                        cheapest.getProductName()))
                .build());

        return ComparisonResult.builder()
                .overallWinner("Xem chi tiết từng sản phẩm")
                .overallReason("AI tư vấn tạm thời không khả dụng. Vui lòng tham khảo bảng thông số bên dưới.")
                .useCases(fallback)
                .specWinners(List.of()) // MỚI
                .build();
    }
}
