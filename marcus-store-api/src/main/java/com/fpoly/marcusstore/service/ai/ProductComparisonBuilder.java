package com.fpoly.marcusstore.service.ai;

import com.fpoly.marcusstore.dto.ai.AiAdvisorResponse;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ProductComparisonBuilder {

    private static final int MAX_PRODUCTS = 3;
    private static final int MAX_SPEC_ROWS = 8;

    public AiAdvisorResponse.ProductComparison build(
            List<AiAdvisorResponse.ProductSuggestion> suggestions,
            Map<Integer, String> productSpecs) {
        List<AiAdvisorResponse.ProductSuggestion> products = suggestions.stream().limit(MAX_PRODUCTS).toList();
        LinkedHashSet<String> specNames = new LinkedHashSet<>();
        Map<Integer, Map<String, String>> parsedSpecs = new LinkedHashMap<>();
        products.forEach(product -> {
            Map<String, String> specs = parseSpecs(productSpecs.get(product.getProductId()));
            parsedSpecs.put(product.getProductId(), specs);
            specs.keySet().stream().limit(MAX_SPEC_ROWS).forEach(specNames::add);
        });

        List<AiAdvisorResponse.ComparisonRow> rows = new ArrayList<>();
        rows.add(new AiAdvisorResponse.ComparisonRow("Giá từ", products.stream()
                .map(product -> product.getPrice() == null ? "Chưa cập nhật" : formatPrice(product)).toList()));
        rows.add(new AiAdvisorResponse.ComparisonRow("Tình trạng", products.stream()
                .map(product -> product.isInStock() ? "Còn hàng" : "Tạm hết hàng").toList()));
        specNames.stream().limit(MAX_SPEC_ROWS).forEach(name -> rows.add(new AiAdvisorResponse.ComparisonRow(
                name,
                products.stream().map(product -> parsedSpecs.get(product.getProductId())
                        .getOrDefault(name, "Chưa có dữ liệu")).toList())));

        Integer bestProductId = products.stream()
                .filter(product -> product.getCompatibilityScore() != null)
                .max(java.util.Comparator.comparingInt(AiAdvisorResponse.ProductSuggestion::getCompatibilityScore))
                .map(AiAdvisorResponse.ProductSuggestion::getProductId).orElse(null);
        return new AiAdvisorResponse.ProductComparison(
                products.stream().map(AiAdvisorResponse.ProductSuggestion::getProductId).toList(),
                products.stream().map(AiAdvisorResponse.ProductSuggestion::getProductName).toList(),
                rows,
                bestProductId);
    }

    private Map<String, String> parseSpecs(String rawSpecs) {
        Map<String, String> result = new LinkedHashMap<>();
        if (rawSpecs == null || rawSpecs.isBlank())
            return result;
        for (String part : rawSpecs.split(", (?=[^,:]{1,60}: )")) {
            int separator = part.indexOf(':');
            if (separator <= 0 || separator == part.length() - 1)
                continue;
            result.putIfAbsent(part.substring(0, separator).trim(), part.substring(separator + 1).trim());
        }
        return result;
    }

    private String formatPrice(AiAdvisorResponse.ProductSuggestion product) {
        String minimum = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(product.getPrice());
        if (product.getMaxPrice() != null && product.getMaxPrice().compareTo(product.getPrice()) > 0) {
            return minimum + "–" + NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
                    .format(product.getMaxPrice()) + " VND";
        }
        return minimum + " VND";
    }
}
