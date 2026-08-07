package com.fpoly.marcusstore.listener;

import com.fpoly.marcusstore.event.OrderConfirmedEvent;
import com.fpoly.marcusstore.service.OrderShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class GhnOrderListener {

        private final OrderShippingService orderShippingService;

        @Async
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void handleOrderConfirmedEvent(OrderConfirmedEvent event) {
                // Marcus sửa: listener chỉ điều phối sau commit. Service tự chia
                // transaction trước/sau HTTP và lưu FAILED để Admin retry.
                try {
                        orderShippingService.createOrRetryGhnOrder(event.getOrderId());
                } catch (Exception e) {
                        log.warn("Tạo vận đơn GHN tự động thất bại orderId={}", event.getOrderId());
                }
        }
}
