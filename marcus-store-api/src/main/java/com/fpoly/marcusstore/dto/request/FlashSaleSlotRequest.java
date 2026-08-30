package com.fpoly.marcusstore.dto.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleSlotRequest {
    @NotBlank(message = "Tên flash sale không được để trống")
    private String name;
    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;
    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;
    @Builder.Default
    private Short status = 1;
    // FE đã upload ảnh lên Cloudinary rồi, chỉ gửi URL về đây
    @Pattern(
            regexp = "^(https?://).+\\.(jpg|jpeg|png|gif|webp|svg)(\\?.*)?$",
            message = "bannerImageUrl phải là URL hợp lệ và kết thúc bằng đuôi ảnh"
    )
    private String bannerImageUrl;
    @NotEmpty(message = "Phải có ít nhất 1 sản phẩm trong flash sale")
    @Valid
    private List<FlashSaleItemRequest> items;
}
