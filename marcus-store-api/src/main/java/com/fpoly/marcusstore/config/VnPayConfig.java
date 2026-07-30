package com.fpoly.marcusstore.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class VnPayConfig {

    @Value("${vnpay.tmnCode}")
    private String tmnCode;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    @Value("${vnpay.payUrl}")
    private String apiUrl;

    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    @Value("${vnpay.refundUrl:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}")
    private String refundUrl;

    @Value("${vnpay.refundIpAddr:127.0.0.1}")
    private String refundIpAddr;

    // Marcus thêm feature flag để tuyệt đối không xác nhận thủ công ở production.
    // Marcus sửa: nếu môi trường quên cấu hình thì mặc định phải khóa xác nhận giả.
    @Value("${vnpay.sandbox:false}")
    private boolean sandbox;

    @Value("${vnpay.allowManualRefundConfirmation:false}")
    private boolean allowManualRefundConfirmation;

    public String getTmnCode() {
        return tmnCode;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getRefundUrl() {
        return refundUrl;
    }

    public String getRefundIpAddr() {
        return refundIpAddr;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public boolean isAllowManualRefundConfirmation() {
        return allowManualRefundConfirmation;
    }

    // mã hóa chữ ký
    public String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null)
                throw new NullPointerException();
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    // Lấy IP của Client để gửi cho VNPAY
    public String getIpAddress(HttpServletRequest request) {
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null)
                ipAdress = request.getRemoteAddr();
        } catch (Exception e) {
            ipAdress = "127.0.0.1";
        }
        return ipAdress;
    }
}
