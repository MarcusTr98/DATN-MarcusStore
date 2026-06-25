package com.fpoly.marcusstore.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder(toBuilder = true)
public class GhnCreateOrderRequest {
    @JsonProperty("payment_type_id")
    private Integer paymentTypeId; // 1: Shop trả, 2: Khách trả

    @JsonProperty("service_type_id")
    private Integer serviceTypeId;

    private String note;

    @JsonProperty("required_note")
    private String requiredNote;

    @JsonProperty("to_name")
    private String toName;

    @JsonProperty("to_phone")
    private String toPhone;

    @JsonProperty("to_address")
    private String toAddress;

    @JsonProperty("to_district_id")
    private Integer toDistrictId;

    @JsonProperty("to_ward_code")
    private String toWardCode;

    @JsonProperty("from_district_id")
    private Integer fromDistrictId;

    @JsonProperty("from_ward_code")
    private String fromWardCode;

    private Integer weight;
    @JsonProperty("cod_amount")

    private Integer codAmount;

    private List<Item> items;

    @Data
    @Builder
    public static class Item {
        private String name;
        private String code;
        private Integer quantity;
    }
}