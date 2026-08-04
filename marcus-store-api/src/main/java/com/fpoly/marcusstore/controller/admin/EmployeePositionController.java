package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.EmployeePositionRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.EmployeePositionResponse;
import com.fpoly.marcusstore.service.EmployeePositionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/positions")
@RequiredArgsConstructor
public class EmployeePositionController {

    private final EmployeePositionService employeePositionService;

    /**
     * Lấy danh sách chức danh
     */
    @GetMapping
    public ApiResponse<List<EmployeePositionResponse>> getAll() {

        return ApiResponse.success(
                employeePositionService.getAll()
        );
    }

    /**
     * Lấy chi tiết chức danh
     */
    @GetMapping("/{id}")
    public ApiResponse<EmployeePositionResponse> getById(
            @PathVariable Integer id) {

        return ApiResponse.success(
                employeePositionService.getById(id)
        );
    }

    /**
     * Thêm chức danh
     */
    @PostMapping
    public ApiResponse<String> create(
            @Valid @RequestBody EmployeePositionRequest request) {

        employeePositionService.create(request);

        return ApiResponse.success("Thêm chức danh thành công");
    }

    /**
     * Cập nhật chức danh
     */
    @PutMapping("/{id}")
    public ApiResponse<String> update(
            @PathVariable Integer id,
            @Valid @RequestBody EmployeePositionRequest request) {

        employeePositionService.update(id, request);

        return ApiResponse.success("Cập nhật chức danh thành công");
    }

    /**
     * Khóa / Mở khóa chức danh
     */
    @PutMapping("/{id}/status")
    public ApiResponse<String> changeStatus(
            @PathVariable Integer id,
            @RequestParam Boolean active) {

        employeePositionService.changeStatus(id, active);

        return ApiResponse.success(
                active
                        ? "Mở khóa chức danh thành công"
                        : "Khóa chức danh thành công"
        );
    }

}