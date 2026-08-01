package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.BannerRequestDTO;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.BannerPositionResponseDTO;
import com.fpoly.marcusstore.dto.response.BannerResponseDTO;
import com.fpoly.marcusstore.repository.cms.BannerPositionRepository;
import com.fpoly.marcusstore.service.BannerService;
import com.fpoly.marcusstore.service.CloudinaryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/banners")
@PreAuthorize("hasAuthority('BANNER_VIEW')")
public class AdminBannerController {

    @Autowired
    private BannerService bannerService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private BannerPositionRepository positionRepository;

    // Lấy tất cả banner
    @GetMapping
    public ApiResponse<List<BannerResponseDTO>> getAll() {
        return ApiResponse.success(bannerService.getAll());
    }

    // Lấy chi tiết 1 banner
    @GetMapping("/{id}")
    public ApiResponse<BannerResponseDTO> getOne(@PathVariable Integer id) {
        return ApiResponse.success(bannerService.getOne(id));
    }

    // Lấy tất cả vị trí banner
    @GetMapping("/positions")
    public ApiResponse<List<BannerPositionResponseDTO>> getPositions() {
        List<BannerPositionResponseDTO> result = positionRepository.findAll().stream()
                .map(BannerPositionResponseDTO::from)
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }

    // Thêm banner mới
    @PostMapping
    @PreAuthorize("hasAuthority('BANNER_CREATE')")
    public ApiResponse<BannerResponseDTO> add(@Valid @RequestBody BannerRequestDTO req) {
        return ApiResponse.success(bannerService.add(req));
    }

    // Sửa banner
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BANNER_UPDATE')")
    public ApiResponse<BannerResponseDTO> update(@PathVariable Integer id, @Valid @RequestBody BannerRequestDTO req) {
        return ApiResponse.success(bannerService.update(id, req));
    }

    // Xóa mềm banner (set isActive = false)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BANNER_DELETE')")
    public ApiResponse<String> remove(@PathVariable Integer id) {
        bannerService.remove(id);
        return ApiResponse.success("Xóa banner thành công");
    }

    // Upload ảnh banner lên Cloudinary, trả về URL
    @PostMapping("/upload-image")
    @PreAuthorize("hasAuthority('BANNER_CREATE')")
    public ApiResponse<String> uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty())
            throw new RuntimeException("File không được để trống");
        if (file.getSize() > 5 * 1024 * 1024)
            throw new RuntimeException("File quá lớn, tối đa 5MB");
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/"))
            throw new RuntimeException("Chỉ chấp nhận file ảnh (JPG, PNG, WEBP...)");
        return ApiResponse.<String>success(cloudinaryService.uploadImage(file));
    }
}