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

    // Marcus thêm xác nhận thủ công có kiểm soát cho môi trường Sandbox.
    public RefundResponse confirmSandbox(Long refundId, String note) {
        return refundService.confirmSandboxRefund(refundId, note);
    }

    @Scheduled(fixedDelay = 60000)
    public void retryTechnicalFailures() {
        for (Long refundId : refundService.findRetryableIds(PageRequest.of(0, 20))) {
            try {
                VnPayRefundClient.RefundCommand command = refundService.prepareAutomaticRetry(refundId);
                if (command != null) {
                    execute(command);
                }
            } catch (RuntimeException ignored) {
                // Marcus sửa: một refund lỗi không được làm dừng toàn bộ batch scheduler.
            }
        }
        // Marcus thêm scheduler QueryDR cho các refund VNPAY còn PROCESSING.
        for (Long refundId : refundService.findProcessingIds(PageRequest.of(0, 20))) {
            try {
                reconcile(refundService.prepareReconciliation(refundId));
            } catch (RuntimeException ignored) {
                // Marcus sửa: batch tiếp tục đối soát các refund còn lại.
            }
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
