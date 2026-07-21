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

    // Marcus thêm đối soát thủ công để admin kiểm tra ngay sau khi duyệt trên
    // VNPAY.
    public RefundResponse reconcile(Long refundId) {
        return reconcile(refundService.prepareReconciliation(refundId));
    }

    @Scheduled(fixedDelay = 60000)
    public void retryTechnicalFailures() {
        for (Long refundId : refundService.findRetryableIds(PageRequest.of(0, 20))) {
            VnPayRefundClient.RefundCommand command = refundService.prepareAutomaticRetry(refundId);
            if (command != null) {
                execute(command);
            }
        }
        // Marcus thêm scheduler QueryDR cho các refund VNPAY còn PROCESSING.
        for (Long refundId : refundService.findProcessingIds(PageRequest.of(0, 20))) {
            reconcile(refundService.prepareReconciliation(refundId));
        }
    }

    private RefundResponse execute(VnPayRefundClient.RefundCommand command) {
        VnPayRefundClient.RefundGatewayResult result = refundClient.refund(command);
        return refundService.completeAttempt(command.refundId(), result);
    }

    private RefundResponse reconcile(VnPayRefundClient.QueryCommand command) {
        return refundService.completeReconciliation(
                command.refundId(), refundClient.queryRefund(command));
    }
}
