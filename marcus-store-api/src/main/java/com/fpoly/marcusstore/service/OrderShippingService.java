package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.GhnCreateOrderRequest;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.core.ShippingConfig;
import com.fpoly.marcusstore.repository.core.ShippingConfigRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderShippingService {
        private static final BigDecimal GHN_MAX_DELIVERY_ORDER_VALUE = new BigDecimal("50000000");

        private final GhnService ghnService;
        private final OrderRepository orderRepository;
        private final ShippingConfigRepository shippingConfigRepository;
        private final OrderStatusHistoryRepository historyRepository;
        private final TransactionTemplate transactionTemplate;

        // Marcus làm: transaction 1 chỉ khóa/đánh dấu attempt và dựng payload;
        // HTTP GHN chạy ngoài transaction; transaction 2 chỉ chốt kết quả.
        public Order createOrRetryGhnOrder(Integer orderId) {
                GhnAttempt attempt = transactionTemplate.execute(status -> prepareAttempt(orderId));
                if (attempt == null) {
                        return transactionTemplate.execute(status -> orderRepository.findById(orderId).orElse(null));
                }

                try {
                        String trackingCode = ghnService.createOrderOnGhn(attempt.request());
                        if (trackingCode == null || trackingCode.isBlank()) {
                                throw new IllegalStateException("GHN không trả về mã vận đơn");
                        }
                        return transactionTemplate.execute(status -> markCreated(orderId, trackingCode));
                } catch (RuntimeException exception) {
                        transactionTemplate.executeWithoutResult(status -> markFailed(orderId, exception));
                        throw exception;
                }
        }

        private GhnAttempt prepareAttempt(Integer orderId) {
                Order order = orderRepository.findByIdForUpdate(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

                if (order.getTrackingCode() != null && !order.getTrackingCode().isBlank()) {
                        order.setGhnIntegrationStatus("SUCCESS");
                        order.setGhnLastError(null);
                        orderRepository.save(order);
                        return null;
                }
                if ("STORE_PICKUP".equalsIgnoreCase(order.getFulfillmentMethod())) {
                        order.setGhnIntegrationStatus("NOT_REQUIRED");
                        orderRepository.save(order);
                        return null;
                }
                if (order.getTotalAmount() != null
                                && order.getTotalAmount().compareTo(GHN_MAX_DELIVERY_ORDER_VALUE) > 0) {
                        throw new IllegalStateException(
                                        "Đơn hàng trên 50 triệu không hỗ trợ giao GHN, vui lòng nhận tại cửa hàng");
                }
                if (!"PACKED".equalsIgnoreCase(order.getOrderStatus())) {
                        throw new IllegalStateException("Chỉ tạo vận đơn khi đơn đã đóng gói");
                }

                // Marcus thêm: chặn hai worker cùng tạo vận đơn; attempt CREATING bị
                // treo quá hai phút mới được phép thử lại.
                if (("PROCESSING".equalsIgnoreCase(order.getGhnIntegrationStatus())
                                || "CREATING".equalsIgnoreCase(order.getGhnIntegrationStatus()))
                                && order.getGhnLastAttemptAt() != null
                                && order.getGhnLastAttemptAt().isAfter(LocalDateTime.now().minusMinutes(2))) {
                        return null;
                }

                boolean isVnPay = "VNPAY".equalsIgnoreCase(order.getPaymentMethod());
                boolean isPaid = "PAID".equalsIgnoreCase(order.getPaymentStatus());

                if (isVnPay && !isPaid) {
                        throw new IllegalStateException(
                                        "Đơn VNPAY chưa thanh toán thành công, không thể tạo vận đơn GHN");
                }

                int codAmount = calculateCodAmount(order, isPaid);

                // 1. Lấy giới hạn bảo hiểm từ DB
                ShippingConfig config = shippingConfigRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()
                                .orElse(null);

                BigDecimal maxInsuranceLimit = config != null && config.getMaxInsuranceValue() != null
                                ? config.getMaxInsuranceValue()
                                : new BigDecimal("5000000");
                int insuranceValue = calculateInsuranceValue(order.getTotalAmount(), maxInsuranceLimit);

                // 2. Tính toán tổng khối lượng
                int totalWeight = order.getOrderItems().stream()
                                .mapToInt(i -> {
                                        Integer weightGram = i.getSku().getWeightGram();
                                        int safeWeight = weightGram != null && weightGram > 0 ? weightGram : 500;
                                        return safeWeight * i.getQuantity();
                                })
                                .sum();

                order.setGhnIntegrationStatus("PROCESSING");
                order.setGhnRetryCount((order.getGhnRetryCount() == null ? 0 : order.getGhnRetryCount()) + 1);
                order.setGhnLastAttemptAt(LocalDateTime.now());
                order.setGhnLastError(null);
                orderRepository.save(order);

                // 3. Khởi tạo Request với dữ liệu động
                GhnCreateOrderRequest request = GhnCreateOrderRequest.builder()
                                .paymentTypeId(1) // Shop trả phí (1)
                                .serviceTypeId(2)
                                .requiredNote("KHONGCHOXEMHANG")
                                .toName(order.getRecipientName())
                                .toPhone(order.getRecipientPhone())
                                .toAddress(order.getShippingAddress())
                                .toDistrictId(order.getToDistrictId())
                                .toWardCode(order.getToWardCode())
                                .weight(totalWeight)
                                .codAmount(codAmount)
                                // Chỉ giới hạn tiền bảo hiểm, không giới hạn giá trị đơn hàng.
                                .insuranceValue(insuranceValue)
                                .items(order.getOrderItems().stream()
                                                .map(i -> GhnCreateOrderRequest.Item.builder()
                                                                .name(i.getSku().getProduct().getProductName())
                                                                .code(i.getSku().getSkuCode())
                                                                .quantity(i.getQuantity())
                                                                .build())
                                                .collect(Collectors.toList()))
                                .build();
                return new GhnAttempt(request);
        }

        private Order markCreated(Integer orderId, String trackingCode) {
                Order order = orderRepository.findByIdForUpdate(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
                order.setTrackingCode(trackingCode);
                order.setGhnIntegrationStatus("SUCCESS");
                order.setGhnLastError(null);
                Order saved = orderRepository.save(order);
                saveIntegrationHistory(saved, "Đã tạo vận đơn GHN",
                                "GHN trả mã vận đơn " + trackingCode + " sau lần thử " + saved.getGhnRetryCount());
                return saved;
        }

        private void markFailed(Integer orderId, RuntimeException exception) {
                orderRepository.findByIdForUpdate(orderId).ifPresent(order -> {
                        int retryCount = order.getGhnRetryCount() == null ? 0 : order.getGhnRetryCount();
                        // Marcus thêm: sau ba lần lỗi không tiếp tục retry mù; đưa
                        // đơn sang hàng đợi để Admin kiểm tra dashboard GHN.
                        order.setGhnIntegrationStatus(retryCount >= 3 ? "NEEDS_REVIEW" : "FAILED");
                        order.setGhnLastError(safeError(exception));
                        orderRepository.save(order);
                        saveIntegrationHistory(order,
                                        "NEEDS_REVIEW".equals(order.getGhnIntegrationStatus())
                                                        ? "Cần kiểm tra tích hợp GHN"
                                                        : "Tạo vận đơn GHN thất bại",
                                        "Lần thử " + retryCount + ": " + order.getGhnLastError());
                });
        }

        private void saveIntegrationHistory(Order order, String title, String note) {
                OrderStatusHistory history = new OrderStatusHistory();
                history.setOrder(order);
                history.setStatus(order.getOrderStatus());
                history.setTitle(title);
                history.setNote(note);
                historyRepository.save(history);
        }

        private String safeError(RuntimeException exception) {
                String message = exception.getMessage();
                if (message == null || message.isBlank()) {
                        return "Không thể kết nối GHN";
                }
                return message.length() <= 500 ? message : message.substring(0, 500);
        }

        private int calculateCodAmount(Order order, boolean isPaid) {
                if (!"COD".equalsIgnoreCase(order.getPaymentMethod()) || isPaid) {
                        return 0;
                }

                try {
                        return order.getFinalAmount().intValueExact();
                } catch (ArithmeticException e) {
                        throw new IllegalArgumentException(
                                        "Số tiền thu hộ COD vượt giới hạn hỗ trợ của GHN", e);
                }
        }

        private int calculateInsuranceValue(BigDecimal orderAmount, BigDecimal configuredLimit) {
                if (orderAmount == null || orderAmount.signum() <= 0
                                || configuredLimit == null || configuredLimit.signum() <= 0) {
                        return 0;
                }

                BigDecimal integerLimit = BigDecimal.valueOf(Integer.MAX_VALUE);
                BigDecimal safeLimit = configuredLimit.min(integerLimit);
                return orderAmount.min(safeLimit).intValue();
        }

        private record GhnAttempt(GhnCreateOrderRequest request) {
        }
}
