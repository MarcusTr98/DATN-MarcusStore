package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressResponseDTO {
    private Integer id;
    private String recipientName;
    private String phone;
    private String provinceName;
    private String districtName;
    private String wardName;
    private String detail;
    private Boolean isDefault;
}