package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnPollingService {
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final GhnService ghnService;

    // Map trạng thái GHN => trạng thái nội bộ
    private static final Map<String, String> GHN_STATUS_MAP = Map.of(
            "picking", "SHIPPING",
            "picked", "SHIPPING",
            "delivering", "SHIPPING",
            "delivered", "DELIVERED",
            "delivery_fail", "FAILED",
            "return", "FAILED",
            "cancel", "CANCELLED");

    @Scheduled(fixedRate = 60000) // Chạy mỗi 60 giây
    @Transactional
    public void syncShippingStatus() {
        // Lấy tất cả đơn đang ở trạng thái SHIPPING
        List<Order> shippingOrders = orderRepository.findByOrderStatus("SHIPPING");

        if (shippingOrders.isEmpty())
            return;

        log.info("⏳ [Polling] Đang kiểm tra trạng thái cho {} đơn hàng...", shippingOrders.size());

        for (Order order : shippingOrders) {
            try {
                // xử lý thêm chốt để qua đc VNPAY
                if (order.getTrackingCode() == null || order.getTrackingCode().trim().isEmpty()) {
                    log.warn("⚠️ [Polling] Đơn {} đang SHIPPING nhưng chưa có mã vận đơn GHN. Bỏ qua.",
                            order.getOrderCode());
                    continue;
                }
                String ghnStatus = ghnService.getTrackingStatus(order.getTrackingCode());
                if (ghnStatus == null)
                    continue;

                String newStatus = GHN_STATUS_MAP.get(ghnStatus.toLowerCase());

                // Nếu trạng thái khớp và khác với hiện tại thì update
                if (newStatus != null && !newStatus.equals(order.getOrderStatus())) {
                    log.info("[Polling] Cập nhật đơn {} từ {} -> {}", order.getOrderCode(), order.getOrderStatus(),
                            newStatus);

                    order.setOrderStatus(newStatus);
                    if ("COMPLETED".equals(newStatus))
                        order.setPaymentStatus("PAID");
                    orderRepository.save(order);

                    // Ghi lịch sử
                    OrderStatusHistory history = new OrderStatusHistory();
                    history.setOrder(order);
                    history.setStatus(newStatus);
                    history.setTitle(getTitle(newStatus));
                    history.setNote("Cập nhật tự động (Polling). Status GHN: " + ghnStatus);
                    historyRepository.save(history);
                }
            } catch (Exception e) {
                log.error("❌ Lỗi sync đơn {}: {}", order.getOrderCode(), e.getMessage());
            }
        }
    }

    private String getTitle(String status) {
        return switch (status) {
            case "SHIPPING" -> "Đơn hàng đang được giao";
            case "DELIVERED" -> "Giao hàng thành công";
            case "COMPLETED" -> "Đơn hàng hoàn thành";
            case "FAILED" -> "Giao hàng không thành công";
            case "CANCELLED" -> "Đơn hàng đã hủy";
            default -> "Cập nhật trạng thái";
        };
    }
}