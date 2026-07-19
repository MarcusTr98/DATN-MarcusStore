package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.config.VnPayConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

@Service
@RequiredArgsConstructor
// Marcus thêm client gọi Refund API chính thức của VNPAY và kiểm tra checksum
// phản hồi.
public class VnPayRefundClient {

    private static final String VERSION = "2.1.0";
    private static final String COMMAND = "refund";
    private static final String FULL_REFUND = "02";
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnPayConfig vnPayConfig;

    public RefundGatewayResult refund(RefundCommand refund) {
        try {
            Map<String, String> payload = buildPayload(refund);
            RestTemplate restTemplate = createSecureRestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    vnPayConfig.getRefundUrl(), payload, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return RefundGatewayResult.retryable("HTTP " + response.getStatusCode().value());
            }
            Map<String, String> body = stringify(response.getBody());
            if (!verifyResponseChecksum(body)) {
                return RefundGatewayResult.failed("97", null, "Phản hồi VNPAY có checksum không hợp lệ");
            }

            String responseCode = body.get("vnp_ResponseCode");
            String transactionStatus = body.get("vnp_TransactionStatus");
            String message = body.get("vnp_Message");
            String responseId = body.get("vnp_ResponseId");
            String refundTransactionId = body.get("vnp_TransactionNo");

            if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
                return RefundGatewayResult.success(
                        responseCode, transactionStatus, message, responseId, refundTransactionId);
            }
            if ("94".equals(responseCode)
                    || "05".equals(transactionStatus)
                    || "06".equals(transactionStatus)) {
                return RefundGatewayResult.processing(
                        responseCode, transactionStatus, message, responseId, refundTransactionId);
            }
            if ("99".equals(responseCode)) {
                return RefundGatewayResult.retryable(message == null ? "VNPAY response 99" : message);
            }
            return RefundGatewayResult.failed(responseCode, transactionStatus, message);
        } catch (RestClientException ex) {
            return RefundGatewayResult.retryable(ex.getMessage());
        } catch (RuntimeException ex) {
            return RefundGatewayResult.failed("CLIENT_ERROR", null, ex.getMessage());
        }
    }

    private RestTemplate createSecureRestTemplate() {
        try {
            HttpClient.Builder httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5));

            // không dùng TrustAll/NoopHostnameVerifier cho nghiệp vụ tiền.
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                KeyStore windowsRoot = KeyStore.getInstance("Windows-ROOT");
                windowsRoot.load(null, null);
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(windowsRoot);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
                httpClient.sslContext(sslContext);
            }

            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient.build());
            requestFactory.setReadTimeout(Duration.ofSeconds(15));
            return new RestTemplate(requestFactory);
        } catch (Exception ex) {
            throw new IllegalStateException("Không khởi tạo được TLS client an toàn cho VNPAY", ex);
        }
    }

    private Map<String, String> buildPayload(RefundCommand refund) {
        String transactionDate = refund.paymentTransactionDate();
        String amount = toVnPayAmount(refund.amount());
        String transactionNo = nullToEmpty(refund.paymentProviderTransactionId());
        String createdAt = LocalDateTime.now(VIETNAM_ZONE).format(VNPAY_DATE);
        String safeCreateBy = sanitize(refund.createBy(), "SYSTEM");
        String orderInfo = "Refund order " + refund.orderCode();

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("vnp_RequestId", refund.requestCode());
        payload.put("vnp_Version", VERSION);
        payload.put("vnp_Command", COMMAND);
        payload.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        payload.put("vnp_TransactionType", FULL_REFUND);
        payload.put("vnp_TxnRef", refund.orderCode());
        payload.put("vnp_Amount", amount);
        payload.put("vnp_TransactionNo", transactionNo);
        payload.put("vnp_TransactionDate", transactionDate);
        payload.put("vnp_CreateBy", safeCreateBy);
        payload.put("vnp_CreateDate", createdAt);
        payload.put("vnp_IpAddr", vnPayConfig.getRefundIpAddr());
        payload.put("vnp_OrderInfo", orderInfo);

        String hashData = String.join("|",
                payload.get("vnp_RequestId"),
                payload.get("vnp_Version"),
                payload.get("vnp_Command"),
                payload.get("vnp_TmnCode"),
                payload.get("vnp_TransactionType"),
                payload.get("vnp_TxnRef"),
                payload.get("vnp_Amount"),
                payload.get("vnp_TransactionNo"),
                payload.get("vnp_TransactionDate"),
                payload.get("vnp_CreateBy"),
                payload.get("vnp_CreateDate"),
                payload.get("vnp_IpAddr"),
                payload.get("vnp_OrderInfo"));
        payload.put("vnp_SecureHash", vnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData));
        return payload;
    }

    private boolean verifyResponseChecksum(Map<String, String> body) {
        String receivedHash = body.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }
        String hashData = String.join("|",
                value(body, "vnp_ResponseId"),
                value(body, "vnp_Command"),
                value(body, "vnp_ResponseCode"),
                value(body, "vnp_Message"),
                value(body, "vnp_TmnCode"),
                value(body, "vnp_TxnRef"),
                value(body, "vnp_Amount"),
                value(body, "vnp_BankCode"),
                value(body, "vnp_PayDate"),
                value(body, "vnp_TransactionNo"),
                value(body, "vnp_TransactionType"),
                value(body, "vnp_TransactionStatus"),
                value(body, "vnp_OrderInfo"));
        String computed = vnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        return computed.equalsIgnoreCase(receivedHash);
    }

    private Map<String, String> stringify(Map<?, ?> source) {
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value == null ? "" : String.valueOf(value)));
        return result;
    }

    private String toVnPayAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền refund phải lớn hơn 0");
        }
        String value = amount.movePointRight(2).toBigIntegerExact().toString();
        if (value.length() > 12) {
            throw new IllegalArgumentException("Số tiền refund vượt giới hạn VNPAY");
        }
        return value;
    }

    private String sanitize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String sanitized = value.replaceAll("[^A-Za-z0-9]", "");
        return sanitized.isBlank() ? fallback : sanitized.substring(0, Math.min(sanitized.length(), 245));
    }

    private String value(Map<String, String> body, String key) {
        return nullToEmpty(body.get(key));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record RefundGatewayResult(
            Outcome outcome,
            String responseCode,
            String transactionStatus,
            String message,
            String responseId,
            String refundTransactionId) {

        static RefundGatewayResult success(String code, String status, String message,
                String responseId, String transactionId) {
            return new RefundGatewayResult(Outcome.SUCCESS, code, status, message, responseId, transactionId);
        }

        static RefundGatewayResult processing(String code, String status, String message,
                String responseId, String transactionId) {
            return new RefundGatewayResult(Outcome.PROCESSING, code, status, message, responseId, transactionId);
        }

        static RefundGatewayResult retryable(String message) {
            return new RefundGatewayResult(Outcome.RETRYABLE, null, null, message, null, null);
        }

        static RefundGatewayResult failed(String code, String status, String message) {
            return new RefundGatewayResult(Outcome.FAILED, code, status, message, null, null);
        }
    }

    public enum Outcome {
        SUCCESS, PROCESSING, RETRYABLE, FAILED
    }

    public record RefundCommand(
            Long refundId,
            String requestCode,
            String orderCode,
            BigDecimal amount,
            String paymentProviderTransactionId,
            String paymentTransactionDate,
            String createBy) {
    }
}
