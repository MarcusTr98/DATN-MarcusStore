package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.repository.shopping.OrderTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class VnPayPaymentExpiryScheduler {

    private final OrderTransactionRepository transactionRepository;
    private final VnPayPaymentExpiryService expiryService;

    @Value("${vnpay.paymentTimeoutMinutes:20}")
    private long paymentTimeoutMinutes;

    // Marcus thêm: scheduler tách riêng để mỗi đơn đi qua proxy transaction và khóa
    // độc lập.
    @Scheduled(initialDelayString = "${vnpay.expiryInitialDelayMs:60000}", fixedDelayString = "${vnpay.expiryScanDelayMs:60000}")
    public void cancelExpiredPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(paymentTimeoutMinutes);
        transactionRepository.findExpiredVnPayOrderIds(cutoff, PageRequest.of(0, 100))
                .forEach(orderId -> {
                    try {
                        expiryService.cancelOneExpiredPayment(orderId);
                    } catch (Exception exception) {
                        // Marcus thêm: một đơn lỗi không được chặn các đơn treo còn lại trong lô.
                        log.error("Không thể tự hủy đơn VNPAY quá hạn orderId={}", orderId, exception);
                    }
                });
    }
}
