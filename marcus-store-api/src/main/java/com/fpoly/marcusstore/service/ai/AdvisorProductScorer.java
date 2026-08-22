package com.fpoly.marcusstore.service.ai;

import com.fpoly.marcusstore.dto.ai.AiAdvisorContext;
import com.fpoly.marcusstore.repository.core.HomeProductRepository.AiProductProjection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class AdvisorProductScorer {

    private static final Map<String, EvidenceRule> EVIDENCE_RULES = Map.of(
            "CAMERA", new EvidenceRule("camera", "camera|ống kính|megapixel|\\bmp\\b|ois|quay video"),
            "PERFORMANCE", new EvidenceRule("hiệu năng", "chip|cpu|gpu|ram|bộ xử lý|processor"),
            "BATTERY", new EvidenceRule("pin/sạc", "pin|mah|sạc|watt|\\bw\\b"),
            "DISPLAY", new EvidenceRule("màn hình", "màn hình|oled|amoled|ltpo|hz|tần số quét"),
            "STORAGE", new EvidenceRule("dung lượng", "dung lượng|storage|rom|gb|tb"),
            "DURABILITY", new EvidenceRule("độ bền", "kháng nước|chống nước|ip6|kính|gorilla|độ bền"),
            "CONNECTIVITY", new EvidenceRule("kết nối", "5g|wifi|wi-fi|bluetooth|nfc|esim"),
            "EASY_TO_USE", new EvidenceRule("dễ sử dụng", "kích thước|trọng lượng|giao diện|màn hình"));

    public ScoreResult score(AiProductProjection product, String specs, AiAdvisorContext context) {
        List<String> reasons = new ArrayList<>();
        List<String> priorities = context == null || context.getPriorities() == null
                ? List.of() : context.getPriorities();
        String normalizedSpecs = specs == null ? "" : specs.toLowerCase(Locale.forLanguageTag("vi-VN"));

        int matchedPriorities = 0;
        for (String priority : priorities) {
            EvidenceRule rule = EVIDENCE_RULES.get(priority);
            if (rule != null && normalizedSpecs.matches(".*(" + rule.pattern() + ").*")) {
                matchedPriorities++;
                reasons.add("Có thông số phù hợp ưu tiên " + rule.label());
            }
        }

        int score = priorities.isEmpty() ? 55
                : 35 + (int) Math.round(50.0 * matchedPriorities / priorities.size());
        BigDecimal budget = context == null ? null : context.getMaxBudget();
        if (budget != null && budget.signum() > 0 && product.getPrice() != null) {
            if (product.getPrice().compareTo(budget) <= 0) {
                score += 10;
                reasons.add("Trong ngân sách " + formatCurrency(budget));
                BigDecimal usage = product.getPrice().divide(budget, 2, RoundingMode.HALF_UP);
                if (usage.compareTo(new BigDecimal("0.75")) >= 0) score += 3;
            } else {
                score -= 30;
            }
        }
        if (product.getStockQuantity() != null && product.getStockQuantity() > 0) {
            score += 2;
            reasons.add("Đang còn hàng");
        }
        if (reasons.isEmpty()) reasons.add("Phù hợp bộ lọc catalog hiện tại");
        return new ScoreResult(Math.max(0, Math.min(100, score)), reasons.stream().limit(3).toList());
    }

    private String formatCurrency(BigDecimal value) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value) + " VND";
    }

    public record ScoreResult(int score, List<String> reasons) {}

    private record EvidenceRule(String label, String pattern) {}
}
