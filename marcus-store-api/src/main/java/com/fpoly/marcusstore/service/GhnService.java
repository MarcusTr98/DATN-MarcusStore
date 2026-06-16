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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnService {

    // lấy ruột của file application.properties để ko lộ token lên github nhé
    @Value("${ghn.api.token}")
    private String ghnToken;

    @Value("${ghn.api.shop-id}")
    private String ghnShopId;

    @Value("${ghn.api.url.fee}")
    private String ghnFeeUrl;

    @Value("${ghn.store.district-id}")
    private Integer fromDistrictId;

    @Value("${ghn.store.ward-code}")
    private String fromWardCode;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Gọi API GHN để tính phí ship
     * * @param toDistrictId ID Quận/Huyện người nhận
     * 
     * @param toWardCode  Mã Phường/Xã người nhận
     * @param totalWeight Tổng khối lượng đơn hàng (gram)
     * @return Số tiền phí ship (Integer)
     */
    public Integer calculateShippingFee(Integer toDistrictId, String toWardCode, Integer totalWeight) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", ghnToken);
            headers.set("ShopId", ghnShopId); // Chú ý: ShopID của GHN đôi khi yêu cầu là số, nhưng set Header String
                                              // thường vẫn pass

            Map<String, Object> payload = new HashMap<>();
            payload.put("service_type_id", 2);
            payload.put("from_district_id", fromDistrictId);
            payload.put("to_district_id", toDistrictId);
            payload.put("to_ward_code", toWardCode);
            payload.put("weight", totalWeight > 0 ? totalWeight : 500);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(ghnFeeUrl, request, Map.class);

            if (response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null && data.containsKey("total")) {
                    // Ép kiểu an toàn, chống ClassCastException (Double/Long -> String -> Integer)
                    return Double.valueOf(data.get("total").toString()).intValue();
                }
            }
            throw new RuntimeException("GHN trả về 200 OK nhưng không có field 'total'");

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // BẮT TRÚNG TIM ĐEN CỦA LỖI HTTP (400, 401, 500)
            log.error("❌ GHN API Error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            return 30000;
        } catch (Exception e) {
            // Bắt các lỗi vặt khác (mất mạng, ép kiểu sai...)
            log.error("❌ Exception Nội bộ: ", e);
            return 30000;
        }
    }
}