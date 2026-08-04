package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponseDTO<T> {
    private List<T> content;
    private int page;           // trang hiện tại 
    private int size;           // số phần tử mỗi trang
    private long totalElements; // tổng số bản ghi
    private int totalPages;     // tổng số trang
    private boolean first;      // có phải trang đầu không
    private boolean last;       // có phải trang cuối không
}