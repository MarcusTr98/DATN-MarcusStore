package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Marcus thêm: quét COD quá hạn theo lô nhỏ, không giữ transaction dài. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CodPendingExpiryScheduler {

    private final OrderRepository orderRepository;
    private final CodPendingExpiryService expiryService;

    @Value("${checkout.cod.pending-timeout-hours:24}")
    private long pendingTimeoutHours;

    @Scheduled(
            initialDelayString = "${checkout.cod.expiry-initial-delay-ms:60000}",
            fixedDelayString = "${checkout.cod.expiry-scan-delay-ms:300000}")
    public void cancelExpiredPendingCodOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(pendingTimeoutHours);
        orderRepository.findExpiredPendingCodOrderIds(cutoff, PageRequest.of(0, 100))
                .forEach(orderId -> {
                    try {
                        expiryService.cancelOneExpiredCod(orderId);
                    } catch (Exception exception) {
                        log.error("Không thể tự hủy đơn COD quá hạn orderId={}", orderId, exception);
                    }
                });
    }
}
