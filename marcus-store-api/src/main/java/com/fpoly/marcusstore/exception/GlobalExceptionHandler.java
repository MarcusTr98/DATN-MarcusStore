package com.fpoly.marcusstore.exception;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.validation.ConstraintViolationException;
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
        // tích lũy tất cả message để tránh FE chỉ nhận được message cuối
        java.util.List<String> globalMessages = new java.util.ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String errorMessage = error.getDefaultMessage();
            if (error instanceof FieldError fe) {
                String fieldName = fe.getField();
                errors.merge(fieldName, errorMessage, (oldV, newV) -> oldV + "; " + newV);
            } else {
                // ObjectError (vd: @AssertTrue trên method) -> gom vào nhóm "_global"
                globalMessages.add(errorMessage);
            }
        });
        if (!globalMessages.isEmpty()) {
            String combined = String.join("; ", globalMessages);
            errors.merge("_global", combined, (oldV, newV) -> oldV + "; " + newV);
        }

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

    // Marcus thêm: mọi request sai JSON, sai kiểu hoặc vi phạm constraint ở
    // path/query đều trả cùng cấu trúc ApiResponse để frontend đọc ổn định.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnreadableRequest(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(400, "Dữ liệu gửi lên sai định dạng.", "INVALID_REQUEST_BODY"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Tham số không hợp lệ.");
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(400, message, "INVALID_PARAMETER"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(400, "Thiếu tham số bắt buộc: " + ex.getParameterName(),
                        "MISSING_PARAMETER"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(400, "Tham số " + ex.getName() + " sai định dạng.",
                        "INVALID_PARAMETER_TYPE"));
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
