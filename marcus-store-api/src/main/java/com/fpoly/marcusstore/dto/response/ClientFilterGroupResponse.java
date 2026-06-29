package com.fpoly.marcusstore.dto.response;
 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClientFilterGroupResponse {
    private Integer attributeId;
    
    private String attributeName; 
    
    private List<ClientFilterOptionResponse> options;
}