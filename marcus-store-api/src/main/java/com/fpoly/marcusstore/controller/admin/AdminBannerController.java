package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.BannerRequestDTO;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.BannerResponseDTO;
import com.fpoly.marcusstore.service.BannerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
public class AdminBannerController {

    @Autowired
    private BannerService bannerService;

    //lấy tất cả banner
    @GetMapping
    public ApiResponse<List<BannerResponseDTO>> getAll() {
        return ApiResponse.success(bannerService.getAll());
    }

    //lấy chi tiết 1 banner
    @GetMapping("/{id}")
    public ApiResponse<BannerResponseDTO> getOne(@PathVariable Integer id) {
        return ApiResponse.success(bannerService.getOne(id));
    }

    // thêm banner mới
    @PostMapping
    public ApiResponse<BannerResponseDTO> add(@Valid @RequestBody BannerRequestDTO req) {
        return ApiResponse.success(bannerService.add(req));
    }

    // sửa banner
    @PutMapping("/{id}")
    public ApiResponse<BannerResponseDTO> update( @PathVariable Integer id, @Valid @RequestBody BannerRequestDTO req) {
        return ApiResponse.success(bannerService.update(id, req));
    }

    //xóa mềm banner
    @DeleteMapping("/{id}")
    public ApiResponse<String> remove(@PathVariable Integer id) {
        bannerService.remove(id);
        return ApiResponse.success("Xóa banner thành công");
    }
}