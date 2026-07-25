package com.fpoly.marcusstore.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RatingStatisticResponse {

    private Integer star;

    private Long count;
}