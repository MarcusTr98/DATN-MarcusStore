package com.fpoly.marcusstore.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fpoly.marcusstore.dto.request.GhnCreateOrderRequest;

import lombok.extern.slf4j.Slf4j;

@Service
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
    @Value("${ghn.store.district-id}")
    private Integer fromDistrictId;
    @Value("${ghn.store.ward-code}")
    private String fromWardCode;

    private final RestTemplate restTemplate = new RestTemplate();

    public Integer calculateShippingFee(Integer toDistrictId, String toWardCode, Integer totalWeight) {
        try {
            HttpHeaders headers = buildHeaders();
            Map<String, Object> payload = new HashMap<>();
            payload.put("service_type_id", 2);
            payload.put("from_district_id", fromDistrictId);
            payload.put("to_district_id", toDistrictId);
            payload.put("to_ward_code", toWardCode);
            payload.put("weight", totalWeight > 0 ? totalWeight : 500);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            Map response = restTemplate.postForObject(ghnFeeUrl, request, Map.class);

            if (response != null && response.containsKey("data")) {
                Map data = (Map) response.get("data");
                return Double.valueOf(data.get("total").toString()).intValue();
            }
            throw new RuntimeException("GHN trả về không có field 'total'");
        } catch (Exception e) {
            log.error("❌ Lỗi tính phí GHN: {}", e.getMessage());
            return 30000;
        }
    }

    public String createOrderOnGhn(GhnCreateOrderRequest request) {
        // Enriched request với thông tin cửa hàng (Shop)
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
        } catch (Exception e) {
            log.error("❌ Lỗi bắn đơn sang GHN: {}", e.getMessage());
            throw new RuntimeException("Không thể tạo đơn vận chuyển trên GHN: " + e.getMessage());
        }
        return null;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("token", ghnToken);
        headers.set("ShopId", String.valueOf(ghnShopId)); // Đảm bảo String
        return headers;
    }
}