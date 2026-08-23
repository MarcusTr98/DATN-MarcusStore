package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCompletionScheduler {

    private final OrderRepository orderRepository;
    private final OrderAutoCompletionService autoCompletionService;
    private final VoucherService voucherService;

    @Value("${order.auto-complete.delivered-after-hours:72}")
    private long deliveredAfterHours;

    // Marcus thêm: mặc định quét mỗi 30 phút; chỉ xử lý tối đa 100 đơn mỗi lượt.
    @Scheduled(initialDelayString = "${order.auto-complete.initial-delay-ms:60000}", fixedDelayString = "${order.auto-complete.scan-delay-ms:1800000}")
    public void autoCompleteDeliveredOrders() {
        long safeHours = Math.max(24, deliveredAfterHours);
        LocalDateTime cutoff = LocalDateTime.now().minusHours(safeHours);
        for (Integer orderId : orderRepository.findDeliveredOrderIdsEligibleForAutoCompletion(cutoff)) {
            try {
                autoCompletionService.completeDeliveredOrder(orderId);
            } catch (RuntimeException exception) {
                // Marcus sửa: một đơn lỗi không được chặn toàn bộ batch; không log
                // thông tin khách hàng hay dữ liệu thanh toán.
                log.warn("Không thể tự hoàn thành orderId={}: {}", orderId, exception.getMessage());
            }
        }
    }

    // Marcus thêm: deactivate voucher hết hạn hoặc hết quantity mỗi 5 phút
    @Scheduled(initialDelayString = "${voucher.expire.initial-delay-ms:60000}", fixedDelayString = "${voucher.expire.scan-delay-ms:300000}")
    public void deactivateExpiredVouchers() {
        try {
            int count = voucherService.deactivateExpiredVouchers();
            if (count > 0) {
                log.info("Da deactivate {} voucher het han/het so luong", count);
            }
        } catch (RuntimeException exception) {
            log.warn("Loi khi deactivate voucher het han: {}", exception.getMessage());
        }
    }
}
