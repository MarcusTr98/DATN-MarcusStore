package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ClientWishlistToggleResponse {

    private Integer productId;

    private Integer skuId;

    private Boolean wished;

    private Long countWislist;
}
