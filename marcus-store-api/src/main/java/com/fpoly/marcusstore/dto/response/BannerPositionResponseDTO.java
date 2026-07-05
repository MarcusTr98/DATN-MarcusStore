package com.fpoly.marcusstore.dto.response;

import com.fpoly.marcusstore.entity.cms.BannerPosition;
import lombok.Builder;
import lombok.Data;
// DTO cho endpoint GET /admin/banners/positions
@Data
@Builder
public class BannerPositionResponseDTO {
 
    private Integer positionId;
    private String positionCode;
    private String description;
    private Boolean allowsOrder;
    private Integer maxSlots;
    
    public static BannerPositionResponseDTO from(BannerPosition pos) {
        boolean allowsOrder = "HOME_SLIDER".equals(pos.getPositionCode());
        int maxSlots = allowsOrder ? 5 : 1;
 
        return BannerPositionResponseDTO.builder()
                .positionId(pos.getPositionId())
                .positionCode(pos.getPositionCode())
                .description(pos.getDescription())
                .allowsOrder(allowsOrder)
                .maxSlots(maxSlots)
                .build();
    }
}