package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewUserStatsDTO {
    private String registerDate;   
    private Long totalNewUsers;
}