package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.config.VnPayConfig;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.service.OrderCancellationService;
import com.fpoly.marcusstore.service.OrderTransactionService;
import com.fpoly.marcusstore.service.AdminNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VnPayControllerTest {

        private final VnPayConfig vnPayConfig = mock(VnPayConfig.class);
        private final OrderRepository orderRepository = mock(OrderRepository.class);
        private final OrderStatusHistoryRepository historyRepository = mock(OrderStatusHistoryRepository.class);
        private final OrderCancellationService cancellationService = mock(OrderCancellationService.class);
        private final OrderTransactionService transactionService = mock(OrderTransactionService.class);
        private final VnPayController controller = new VnPayController(
                        vnPayConfig,
                        orderRepository,
                        historyRepository,
                        cancellationService,
                        transactionService,
                        mock(AdminNotificationService.class));

        @Test
        void parsesVnPayAmountWithoutLongOverflowOrTruncation() {
                assertEquals(new BigDecimal("12345678901234567890.12"),
                                controller.parseVnPayAmount("1234567890123456789012"));
                assertEquals(new BigDecimal("100.50"), controller.parseVnPayAmount("10050"));
        }

        @Test
        void rejectsMissingMalformedNegativeAndOversizedAmounts() {
                assertNull(controller.parseVnPayAmount(null));
                assertNull(controller.parseVnPayAmount(""));
                assertNull(controller.parseVnPayAmount("12x00"));
                assertNull(controller.parseVnPayAmount("-100"));
                assertNull(controller.parseVnPayAmount("1".repeat(31)));
        }

        @Test
        void lateSuccessfulIpnAfterAutomaticCancellationCreatesRefundWithoutRestoringAgain() {
                HttpServletRequest request = mock(HttpServletRequest.class);
                Map<String, String> parameters = new LinkedHashMap<>();
                parameters.put("vnp_Amount", "100000");
                parameters.put("vnp_ResponseCode", "00");
                parameters.put("vnp_TransactionNo", "VNP-LATE-1");
                parameters.put("vnp_TxnRef", "ORD-LATE-1");
                parameters.put("vnp_SecureHash", "valid-hash");
                when(request.getParameterNames())
                                .thenReturn(Collections.enumeration(parameters.keySet()));
                parameters.forEach((name, value) -> when(request.getParameter(name)).thenReturn(value));
                when(vnPayConfig.getHashSecret()).thenReturn("test-secret");
                when(vnPayConfig.hmacSHA512(org.mockito.ArgumentMatchers.eq("test-secret"),
                                org.mockito.ArgumentMatchers.anyString()))
                                .thenReturn("valid-hash");

                Order order = new Order();
                order.setOrderId(20);
                order.setOrderCode("ORD-LATE-1");
                order.setFinalAmount(new BigDecimal("1000"));
                order.setPaymentMethod("VNPAY");
                order.setPaymentStatus("FAILED");
                order.setOrderStatus("CANCELLED");
                when(orderRepository.findByOrderCodeForUpdate("ORD-LATE-1"))
                                .thenReturn(Optional.of(order));
                when(transactionService.getVnPayTransactionState(order))
                                .thenReturn(new OrderTransactionService.VnPayTransactionState(
                                                "FAILED", null, null));

                Map<String, String> response = controller.receiveIPN(request).getBody();

                assertEquals("00", response.get("RspCode"));
                assertEquals("CANCELLED", order.getOrderStatus());
                assertEquals("PAID", order.getPaymentStatus());
                verify(transactionService).markLateVnPayPaymentSuccess(order, "VNP-LATE-1", "00");
                verify(cancellationService).requestRefundForCancelledPaidOrder(
                                org.mockito.ArgumentMatchers.eq(order),
                                org.mockito.ArgumentMatchers.contains("VNP-LATE-1"));
                verify(orderRepository).save(order);
        }
}
