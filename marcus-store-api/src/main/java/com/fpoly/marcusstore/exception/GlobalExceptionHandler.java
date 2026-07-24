package com.fpoly.marcusstore.exception;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String INTERNAL_ERROR_MESSAGE = "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.";

    // 1. Bắt các lỗi validate form (ví dụ: để trống username, email sai định dạng)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // Đóng gói vào ApiResponse
        ApiResponse<Map<String, String>> response = new ApiResponse<>(400, "Dữ liệu đầu vào không hợp lệ", errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 2. Xử lý ResponseStatusException - tách errorCode nhúng trong reason theo
    // pattern "MESSAGE|CODE"
    // VD: "Voucher đã ngừng hoạt động.|VOUCHER_INACTIVE" -> message="Voucher đã
    // ngừng hoạt động.", data="VOUCHER_INACTIVE"
    // Lý do: cho phép service throw kèm mã lỗi để FE phân biệt loại lỗi mà không
    // cần tạo class Exception mới.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Object>> handleResponseStatusException(ResponseStatusException ex) {
        String reason = ex.getReason() != null ? ex.getReason() : "Đã xảy ra lỗi";
        String message = reason;
        String errorCode = null;

        int pipeIdx = reason.lastIndexOf('|');
        if (pipeIdx > 0 && pipeIdx < reason.length() - 1) {
            String candidate = reason.substring(pipeIdx + 1).trim();
            // Chỉ coi là errorCode nếu khớp pattern VOUCHER_XXX (chữ in hoa + số/_)
            if (candidate.matches("^[A-Z_][A-Z0-9_]*$")) {
                message = reason.substring(0, pipeIdx).trim();
                errorCode = candidate;
            }
        }

        ApiResponse<Object> response = new ApiResponse<>(
                ex.getStatusCode().value(),
                message,
                errorCode // null nếu không phải lỗi có code nhúng
        );
        return new ResponseEntity<>(response, ex.getStatusCode());
    }

    // 3. Bắt các lỗi văng ra từ logic nghiệp vụ (Custom Exception)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeExceptions(RuntimeException ex) {
        ApiResponse<Object> response = new ApiResponse<>(400, ex.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 4. Bắt lỗi hệ thống không xác định, chỉ ghi chi tiết ở server.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalExceptions(Exception ex) {
        log.error("Lỗi hệ thống chưa được xử lý", ex);
        ApiResponse<Object> response = ApiResponse.error(500, INTERNAL_ERROR_MESSAGE);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        403,
                        "Bạn không có quyền truy cập"));
    }
}
