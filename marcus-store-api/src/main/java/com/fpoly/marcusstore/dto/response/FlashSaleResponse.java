package com.fpoly.marcusstore.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleResponse {

    private Integer slotId;
    private String name;
    private String bannerImageUrl;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer quantityFlashSaleSlot;
    private Integer usedQuantity;
    private Short status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Flag đánh dấu slot đã bị admin hủy (status=4). FE dựa vào đây để hiển thị modal
    // thông báo "Flash Sale đã bị admin hủy" khi khách tương tác với sản phẩm thuộc slot này.
    // BE trả kèm slot CANCELLED trong cùng response để FE biết slotId nào đã bị hủy.
    private Boolean isCancelled = false;

    private List<FlashSaleItemResponse> items;
}
