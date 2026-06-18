package com.fpoly.marcusstore.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrderStatusRequest {
    private String status;
    private String note;

}
