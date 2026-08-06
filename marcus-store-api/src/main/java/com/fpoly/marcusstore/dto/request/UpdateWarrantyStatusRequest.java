package com.fpoly.marcusstore.dto.request;

import com.fpoly.marcusstore.entity.shopping.WarrantyReturn.WarrantyStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWarrantyStatusRequest {
    
    private WarrantyStatus status;
    private String adminNote;
}
