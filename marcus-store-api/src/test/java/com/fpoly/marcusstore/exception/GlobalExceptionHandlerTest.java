package com.fpoly.marcusstore.exception;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

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

    @Test
    void translatesIdentityCollisionWithoutLeakingSql() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute insert into Categories (...) values (...) ",
                new SQLException("Violation of PRIMARY KEY constraint 'PK_Categories'. duplicate key (5)"));

        ResponseEntity<ApiResponse<Object>> response = handler.handleDataIntegrityViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo("IDENTITY_OUT_OF_SYNC");
        assertThat(response.getBody().getMessage())
                .contains("MarcusUpdateHeThong0908.sql")
                .doesNotContain("insert into", "PK_Categories", "duplicate key");
    }
}
