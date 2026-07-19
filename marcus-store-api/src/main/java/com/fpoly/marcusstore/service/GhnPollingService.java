package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnPollingService {
    private final OrderRepository orderRepository;
    private final GhnService ghnService;
    private final GhnStatusService ghnStatusService;

    @Scheduled(fixedRate = 60000) // Chạy mỗi 60 giây
    @Transactional
    public void syncShippingStatus() {
        // Theo dõi cả đơn vừa đóng gói, đang giao và giao lỗi để hỗ trợ giao lại.
        List<Order> shippingOrders = orderRepository.findByOrderStatusIn(
                List.of("PACKED", "SHIPPING", "FAILED"));

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

                GhnStatusService.SyncResult result = ghnStatusService.applyStatus(
                        order.getTrackingCode(), ghnStatus, "POLLING");
                if (result == GhnStatusService.SyncResult.UPDATED) {
                    log.info("[Polling] Đã đồng bộ đơn {} theo trạng thái GHN {}",
                            order.getOrderCode(), ghnStatus);
                }
            } catch (Exception e) {
                log.error("❌ Lỗi sync đơn {}: {}", order.getOrderCode(), e.getMessage());
            }
        }
    }

}
