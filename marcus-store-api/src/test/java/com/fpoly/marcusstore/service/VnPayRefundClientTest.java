package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.config.VnPayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VnPayRefundClientTest {

    private VnPayConfig config;
    private VnPayRefundClient client;

    @BeforeEach
    void setUp() {
        config = new VnPayConfig();
        ReflectionTestUtils.setField(config, "hashSecret", "Marcus-Test-Secret");
        client = new VnPayRefundClient(config);
    }

    @Test
    void acceptsOfficialRefundResponseChecksumWithoutPromotionFields() {
        Map<String, String> body = responseBody();
        body.put("vnp_SecureHash", config.hmacSHA512(
                config.getHashSecret(), officialRefundHashData(body)));

        Boolean valid = ReflectionTestUtils.invokeMethod(client, "verifyResponseChecksum", body);

        assertThat(valid).isTrue();
    }

    @Test
    void rejectsRefundChecksumThatIncorrectlyIncludesQueryDrPromotionFields() {
        Map<String, String> body = responseBody();
        String incorrect = officialRefundHashData(body)
                + "|" + body.get("vnp_PromotionCode")
                + "|" + body.get("vnp_PromotionAmount");
        body.put("vnp_SecureHash", config.hmacSHA512(config.getHashSecret(), incorrect));

        Boolean valid = ReflectionTestUtils.invokeMethod(client, "verifyResponseChecksum", body);

        assertThat(valid).isFalse();
    }

    @Test
    void acceptsOfficialQueryDrChecksumIncludingPromotionFields() {
        Map<String, String> body = responseBody();
        String queryHashData = officialRefundHashData(body)
                + "|" + body.get("vnp_PromotionCode")
                + "|" + body.get("vnp_PromotionAmount");
        body.put("vnp_SecureHash", config.hmacSHA512(config.getHashSecret(), queryHashData));

        Boolean valid = ReflectionTestUtils.invokeMethod(client, "verifyQueryChecksum", body);

        assertThat(valid).isTrue();
    }

    // Marcus thêm fixture khóa đúng thứ tự trường checksum trong tài liệu VNPAY.
    private Map<String, String> responseBody() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_ResponseId", "response-1");
        body.put("vnp_Command", "refund");
        body.put("vnp_ResponseCode", "00");
        body.put("vnp_Message", "Success");
        body.put("vnp_TmnCode", "MARCUS");
        body.put("vnp_TxnRef", "ORD-10");
        body.put("vnp_Amount", "100000000");
        body.put("vnp_BankCode", "NCB");
        body.put("vnp_PayDate", "20260723170000");
        body.put("vnp_TransactionNo", "12345678");
        body.put("vnp_TransactionType", "02");
        body.put("vnp_TransactionStatus", "00");
        body.put("vnp_OrderInfo", "Refund order ORD-10");
        body.put("vnp_PromotionCode", "");
        body.put("vnp_PromotionAmount", "");
        return body;
    }

    private String officialRefundHashData(Map<String, String> body) {
        return String.join("|",
                body.get("vnp_ResponseId"),
                body.get("vnp_Command"),
                body.get("vnp_ResponseCode"),
                body.get("vnp_Message"),
                body.get("vnp_TmnCode"),
                body.get("vnp_TxnRef"),
                body.get("vnp_Amount"),
                body.get("vnp_BankCode"),
                body.get("vnp_PayDate"),
                body.get("vnp_TransactionNo"),
                body.get("vnp_TransactionType"),
                body.get("vnp_TransactionStatus"),
                body.get("vnp_OrderInfo"));
    }
}
