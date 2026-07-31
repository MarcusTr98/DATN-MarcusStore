package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.CloudinaryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/flashsale-banner")
public class AdminFlashSaleBannerController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping(consumes = "multipart/form-data")
    // Marcus sửa: chỉ người có quyền tạo/cập nhật Flash Sale mới được upload
    // banner.
    @PreAuthorize("hasAnyAuthority('FLASHSALE_CREATE', 'FLASHSALE_UPDATE')")
    public ApiResponse<Map<String, String>> uploadBanner(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh banner không được để trống");
        }
        try {
            String imageUrl = cloudinaryService.uploadImage(file);
            Map<String, String> data = new HashMap<>();
            data.put("imageUrl", imageUrl);
            return ApiResponse.success(data);
        } catch (IOException e) {
            throw new RuntimeException("Upload ảnh banner thất bại", e);
        }
    }
}
