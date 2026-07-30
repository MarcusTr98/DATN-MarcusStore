package com.fpoly.marcusstore.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GhnWebhookRequest {
    @JsonProperty("order_code")
    @NotBlank(message = "Mã vận đơn GHN không được để trống")
    @Size(max = 50, message = "Mã vận đơn GHN không hợp lệ")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã vận đơn GHN sai định dạng")
    private String orderCode;

    @NotBlank(message = "Trạng thái GHN không được để trống")
    @Size(max = 50, message = "Trạng thái GHN không hợp lệ")
    @Pattern(regexp = "^[a-z_]+$", message = "Trạng thái GHN sai định dạng")
    private String status;
}
