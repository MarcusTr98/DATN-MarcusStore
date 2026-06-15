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
            // Cấu hình Header bắt buộc của GHN
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", ghnToken);
            headers.set("ShopId", ghnShopId);

            // Build Payload gửi sang GHN
            Map<String, Object> payload = new HashMap<>();
            payload.put("service_type_id", 2); // 2: Chuyển phát truyền thống
            payload.put("from_district_id", fromDistrictId);
            payload.put("to_district_id", toDistrictId);
            payload.put("to_ward_code", toWardCode);
            payload.put("weight", totalWeight > 0 ? totalWeight : 500);

            // Gọi API GHN
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(ghnFeeUrl, request, Map.class);

            // JSON lấy total
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null && data.containsKey("total")) {
                    return (Integer) data.get("total");
                }
            }
            throw new RuntimeException("GHN không trả về phí ship hợp lệ");

        } catch (Exception e) {
            log.error("Lỗi khi gọi API tính phí GHN: {}", e.getMessage());
            // Trả về phí ship mặc định 30.000đ nếu GHN lỗi mạng để ko làm die
            // luồng mua hàng
            return 30000;
        }
    }
}