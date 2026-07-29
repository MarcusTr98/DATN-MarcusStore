package com.fpoly.marcusstore.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewItemResponse {

    private Integer productId;

    private String productName;

    private Long reviewCount;

}