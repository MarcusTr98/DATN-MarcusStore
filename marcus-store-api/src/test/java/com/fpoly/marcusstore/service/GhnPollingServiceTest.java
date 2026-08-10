package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

// Marcus thêm testcase thuộc phạm vi đồng bộ GHN của Marcus:
// một mã lỗi không được giữ transaction dài hoặc chặn polling các mã còn lại.
class GhnPollingServiceTest {
    @Test
    void continuesWithNextTrackingCodeWhenOneCallFails() {
        OrderRepository repository = mock(OrderRepository.class);
        GhnService ghnService = mock(GhnService.class);
        GhnStatusService statusService = mock(GhnStatusService.class);
        when(repository.findTrackingCodesForGhnPolling()).thenReturn(List.of("GHN-ERROR", "GHN-OK"));
        when(ghnService.getTrackingStatus("GHN-ERROR")).thenThrow(new RuntimeException("timeout"));
        when(ghnService.getTrackingStatus("GHN-OK")).thenReturn("delivering");

        new GhnPollingService(repository, ghnService, statusService).syncShippingStatus();

        verify(statusService).applyStatus("GHN-OK", "delivering", "POLLING");
    }
}
