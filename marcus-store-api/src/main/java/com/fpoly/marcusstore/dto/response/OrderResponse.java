package com.fpoly.marcusstore.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponse {
    private Integer orderId;
    private String orderCode;
    private String recipientName;
    private String recipientPhone;
    private BigDecimal finalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    // Marcus thêm: hiển thị badge nhận tại cửa hàng trong danh sách đơn.
    private String fulfillmentMethod;
    private Integer itemCount;

    // Marcus bổ sung
    private String transactionId;

    // format ngày giờ về dạng dd/MM/yyyy HH:mm
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime createdAt;
    // Marcus bổ sung
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime paymentDate;

}
