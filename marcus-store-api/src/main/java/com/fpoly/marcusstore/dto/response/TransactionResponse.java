package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private Integer transactionId; // mã nội bộ
    private String providerTransactionId; // mã giao dịch do VNPAY/GHN cung cấp
    private String orderCode;
    private BigDecimal amount;
    private String type;
    private String status;
    // Marcus thêm: Đối soát cần biết trạng thái đơn để tách tiền thực giữ và
    // doanh thu đã đủ điều kiện ghi nhận.
    private String orderStatus;
    private String paymentStatus;
    // Marcus thêm: frontend đối soát có thể kiểm chứng COD giao hàng và COD tại
    // quầy.
    private String fulfillmentMethod;
    private String note;
    private LocalDateTime createdAt;

    // bổ sung thông tin vào modal chi tiết
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;

    // bổ sung tích đối soát
    private Boolean isReconciled;
}
