package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code; // 200: Thành công, 400: Bad Request, 401: Unauthorized, 403: Forbidden, 500:
                      // Lỗi Server
    private String message;
    private T data;

    // 1. Helper cho API trả về cấu trúc Data (VD: Lấy danh sách sản phẩm)
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("Success")
                .data(data)
                .build();
    }

    // 2. Helper cho API chỉ cần trả về câu thông báo (VD: Thêm thành công)
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(null)
                .build();
    }

    // 3. Helper cho API bắn lỗi (VD: Validation sai, Lỗi logic)
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}