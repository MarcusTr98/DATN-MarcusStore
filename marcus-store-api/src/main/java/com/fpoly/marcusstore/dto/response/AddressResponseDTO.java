package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class AddressResponseDTO {
    private Integer addressId;
    private String recipientName;
    private String phoneNumber;
    private String provinceName;
    private String districtName;
    private String wardName;
    private String detailAddress;
    private String note;
    private Boolean isDefault;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer provinceId;
    private Integer districtId;
    private String wardCode;
}