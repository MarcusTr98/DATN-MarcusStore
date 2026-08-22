package com.fpoly.marcusstore.dto.analytics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BehaviorEventRequest {
    @NotBlank
    @Pattern(regexp = "^(PRODUCT_VIEW|CART_ADDED|CHECKOUT_STARTED)$")
    private String eventType;
    @Pattern(regexp = "^[0-9a-fA-F-]{36}$")
    private String sessionId;
    private Integer productId;
}
