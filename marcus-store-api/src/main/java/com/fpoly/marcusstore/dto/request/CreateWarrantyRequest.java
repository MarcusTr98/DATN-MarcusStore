package com.fpoly.marcusstore.dto.request;

import com.fpoly.marcusstore.entity.shopping.WarrantyReturn.WarrantyReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateWarrantyRequest {
    
    @NotNull(message = "Order item ID is required")
    private Integer orderItemId;
    
    @NotNull(message = "Reason is required")
    private WarrantyReason reason;
    
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;
    
    private List<String> attachmentUrls;
}
