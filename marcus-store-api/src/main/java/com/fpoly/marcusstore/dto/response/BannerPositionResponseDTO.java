package com.fpoly.marcusstore.dto.response;

import com.fpoly.marcusstore.entity.cms.BannerPosition;
import com.fpoly.marcusstore.utils.BannerPositionRules;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BannerPositionResponseDTO {

    private Integer positionId;
    private String positionCode;
    private String description;
    private Boolean allowsOrder;
    private Integer maxSlots;

    public static BannerPositionResponseDTO from(BannerPosition pos) {
        return BannerPositionResponseDTO.builder()
                .positionId(pos.getPositionId())
                .positionCode(pos.getPositionCode())
                .description(pos.getDescription())
                .allowsOrder(BannerPositionRules.allowsOrder(pos.getPositionCode()))
                .maxSlots(BannerPositionRules.maxSlots(pos.getPositionCode()))
                .build();
    }
}