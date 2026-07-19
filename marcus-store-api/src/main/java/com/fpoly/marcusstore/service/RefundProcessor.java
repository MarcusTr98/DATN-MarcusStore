package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.RefundResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// Marcus thêm processor tách transaction DB khỏi network call và retry lỗi kỹ
// thuật.
public class RefundProcessor {

    private final RefundService refundService;
    private final VnPayRefundClient refundClient;

    public RefundResponse approve(Long refundId) {
        return execute(refundService.prepareApproval(refundId));
    }

    public RefundResponse retry(Long refundId) {
        return execute(refundService.prepareAdminRetry(refundId));
    }

    @Scheduled(fixedDelay = 60000)
    public void retryTechnicalFailures() {
        for (Long refundId : refundService.findRetryableIds(PageRequest.of(0, 20))) {
            VnPayRefundClient.RefundCommand command = refundService.prepareAutomaticRetry(refundId);
            if (command != null) {
                execute(command);
            }
        }
    }

    private RefundResponse execute(VnPayRefundClient.RefundCommand command) {
        VnPayRefundClient.RefundGatewayResult result = refundClient.refund(command);
        return refundService.completeAttempt(command.refundId(), result);
    }
}
