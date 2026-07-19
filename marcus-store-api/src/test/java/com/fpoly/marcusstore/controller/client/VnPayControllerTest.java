package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.config.VnPayConfig;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.service.OrderCancellationService;
import com.fpoly.marcusstore.service.OrderTransactionService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class VnPayControllerTest {

    private final VnPayController controller = new VnPayController(
            mock(VnPayConfig.class),
            mock(OrderRepository.class),
            mock(OrderStatusHistoryRepository.class),
            mock(OrderCancellationService.class),
            mock(OrderTransactionService.class));

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
}
