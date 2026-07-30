package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FinancialReconcileRequest {
    @NotNull(message = "Trạng thái đối soát không được để trống")
    private Boolean status;
}
