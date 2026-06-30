package com.fpoly.marcusstore.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fpoly.marcusstore.dto.request.GhnCreateOrderRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnService {

    @Value("${ghn.api.token}")
    private String ghnToken;

    @Value("${ghn.api.shop-id}")
    private String ghnShopId;

    @Value("${ghn.api.url.fee}")
    private String ghnFeeUrl;

    @Value("${ghn.api.url.create}")
    private String ghnCreateUrl;

    // Thêm link check detail
    @Value("${ghn.api.url.detail:https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/detail}")
    private String ghnDetailUrl;

    @Value("${ghn.store.district-id}")
    private Integer fromDistrictId;

    @Value("${ghn.store.ward-code}")
    private String fromWardCode;

    private final RestTemplate restTemplate = new RestTemplate();

    public Integer calculateShippingFee(Integer toDistrictId, String toWardCode, Integer totalWeight,
            Integer insuranceValue) {
        try {
            HttpHeaders headers = buildHeaders();

            Map<String, Object> payload = new HashMap<>();
            payload.put("service_type_id", 2);
            payload.put("from_district_id", fromDistrictId);
            payload.put("to_district_id", toDistrictId);
            payload.put("to_ward_code", toWardCode);
            payload.put("weight", totalWeight > 0 ? totalWeight : 500);

            // FIX: giá trị khai giá tối đa 5 triệu theo chuẩn GHN
            if (insuranceValue != null && insuranceValue > 0) {
                int validInsurance = Math.min(insuranceValue, 5000000);
                payload.put("insurance_value", validInsurance);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(ghnFeeUrl, request, Map.class);

            if (response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null && data.containsKey("total")) {
                    return Double.valueOf(data.get("total").toString()).intValue();
                }
            }
            throw new RuntimeException("GHN trả về 200 OK nhưng không có field 'total'");
        } catch (Exception e) {
            log.error("❌ Exception nội bộ tính phí GHN: ", e);
            return 30000; // Phí fallback an toàn
        }
    }

    public String createOrderOnGhn(GhnCreateOrderRequest request) {
        GhnCreateOrderRequest enrichedRequest = request.toBuilder()
                .fromDistrictId(fromDistrictId)
                .fromWardCode(fromWardCode)
                .build();

        HttpEntity<GhnCreateOrderRequest> entity = new HttpEntity<>(enrichedRequest, buildHeaders());

        try {
            Map response = restTemplate.postForObject(ghnCreateUrl, entity, Map.class);
            if (response != null && response.containsKey("data")) {
                Map data = (Map) response.get("data");
                String trackingCode = (String) data.get("order_code");
                log.info("[GHN] Tạo đơn thành công, tracking_code={}", trackingCode);
                return trackingCode;
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("❌ GHN tạo đơn lỗi HTTP ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("GHN từ chối tạo đơn: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ Lỗi khi bắn đơn sang GHN: {}", e.getMessage());
            throw new RuntimeException("Không thể tạo đơn vận chuyển trên GHN");
        }
        return null;
    }

    // Hàm lấy trạng thái GHN (Đã sửa link thành DEV URL)
    public String getTrackingStatus(String trackingCode) {
        try {
            HttpHeaders headers = buildHeaders();
            Map<String, Object> payload = new HashMap<>();
            payload.put("order_code", trackingCode);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(ghnDetailUrl, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                Map data = (Map) response.getBody().get("data");
                return (String) data.get("status");
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("❌ Lỗi HTTP check trạng thái GHN {}: {}", trackingCode, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ Lỗi check trạng thái GHN cho {}: {}", trackingCode, e.getMessage());
        }
        return null;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("token", ghnToken);
        headers.set("ShopId", String.valueOf(ghnShopId));
        return headers;
    }
}