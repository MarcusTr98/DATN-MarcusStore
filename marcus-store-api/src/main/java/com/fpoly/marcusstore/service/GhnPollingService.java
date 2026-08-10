package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnPollingService {
    private final OrderRepository orderRepository;
    private final GhnService ghnService;
    private final GhnStatusService ghnStatusService;

    // Marcus sửa: fixedDelay tránh chồng lượt quét; không giữ transaction/Entity trong lúc gọi GHN.
    @Scheduled(initialDelayString = "${ghn.polling.initial-delay-ms:60000}",
            fixedDelayString = "${ghn.polling.delay-ms:60000}")
    public void syncShippingStatus() {
        // Marcus sửa: query projection chỉ lấy mã vận đơn cần thiết, không mở transaction dài.
        List<String> trackingCodes = orderRepository.findTrackingCodesForGhnPolling();

        if (trackingCodes.isEmpty())
            return;

        log.info("[GHN Polling] Đang kiểm tra {} vận đơn", trackingCodes.size());

        for (String trackingCode : trackingCodes) {
            try {
                String ghnStatus = ghnService.getTrackingStatus(trackingCode);
                if (ghnStatus == null)
                    continue;

                GhnStatusService.SyncResult result = ghnStatusService.applyStatus(
                        trackingCode, ghnStatus, "POLLING");
                if (result == GhnStatusService.SyncResult.UPDATED) {
                    log.info("[GHN Polling] Đã đồng bộ vận đơn {} theo trạng thái {}", trackingCode, ghnStatus);
                }
            } catch (Exception e) {
                // Marcus sửa: một vận đơn lỗi không làm dừng các vận đơn còn lại.
                log.warn("[GHN Polling] Không đồng bộ được vận đơn {}: {}", trackingCode, e.getMessage());
            }
        }
    }

}
