package com.fpoly.marcusstore.exception;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void hidesInternalExceptionDetailsFromClient() {
        Exception exception = new Exception("SQL Server password=secret");

        ResponseEntity<ApiResponse<Object>> response = handler.handleGlobalExceptions(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(500);
        assertThat(response.getBody().getMessage())
                .isEqualTo("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.")
                .doesNotContain(exception.getMessage());
        assertThat(response.getBody().getData()).isNull();
    }
}
