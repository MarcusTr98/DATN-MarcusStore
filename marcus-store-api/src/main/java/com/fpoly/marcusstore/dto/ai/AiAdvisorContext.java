package com.fpoly.marcusstore.dto.ai;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Marcus thêm: ngữ cảnh tư vấn có cấu trúc, không phụ thuộc việc quét lại câu
// văn AI đã trả lời ở lượt trước.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAdvisorContext {
        @Pattern(regexp = "PHONE|ACCESSORY", message = "Loại sản phẩm tư vấn không hợp lệ.")
        private String category;

        @Pattern(regexp = "ANDROID|IOS|ANY", message = "Nền tảng tư vấn không hợp lệ.")
        private String platform;

        @Size(max = 4)
        @Builder.Default
        private List<@Pattern(regexp = "[\\p{L}\\p{N} .&+_-]{1,50}", message = "Tên hãng tư vấn không hợp lệ.") String> brands = new ArrayList<>();

        @DecimalMin(value = "0")
        @DecimalMax(value = "1000000000")
        private BigDecimal minBudget;

        @DecimalMin(value = "0")
        @DecimalMax(value = "1000000000")
        private BigDecimal maxBudget;

        @Size(max = 6)
        @Builder.Default
        private List<@Pattern(regexp = "CAMERA|PERFORMANCE|BATTERY|DISPLAY|STORAGE|DURABILITY|CONNECTIVITY|EASY_TO_USE|BRAND") String> priorities = new ArrayList<>();

        @Size(max = 3)
        @Builder.Default
        private List<Integer> selectedProductIds = new ArrayList<>();

        // Marcus thêm: sản phẩm khách thực sự click khác với danh sách AI đề xuất.
        // Backend không được tự ghi đè lựa chọn này bằng bestProductId của Gemini.
        private Integer focusedProductId;
}
