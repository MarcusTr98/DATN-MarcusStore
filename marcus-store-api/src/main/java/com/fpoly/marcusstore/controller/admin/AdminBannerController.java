package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.BannerRequestDTO;
import com.fpoly.marcusstore.dto.response.BannerResponseDTO;
import com.fpoly.marcusstore.service.BannerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/banners")
public class AdminBannerController {

    @Autowired
    private BannerService bannerService;

    //lấy tất cả banner
    @GetMapping
    public ResponseEntity<List<BannerResponseDTO>> getAll() {
        return ResponseEntity.ok(bannerService.getAll());
    }

    //lấy chi tiết 1 banner
    @GetMapping("/{id}")
    public ResponseEntity<BannerResponseDTO> getOne(@PathVariable Integer id) {
        return ResponseEntity.ok(bannerService.getOne(id));
    }

    // thêm banner mới
    @PostMapping
    public ResponseEntity<BannerResponseDTO> add(@Valid @RequestBody BannerRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bannerService.add(req));
    }

    // sửa banner
    @PutMapping("/{id}")
    public ResponseEntity<BannerResponseDTO> update( @PathVariable Integer id, @Valid @RequestBody BannerRequestDTO req) {
        return ResponseEntity.ok(bannerService.update(id, req));
    }

    //xóa mềm banner
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> remove(@PathVariable Integer id) {
        bannerService.remove(id);
        return ResponseEntity.ok(Map.of("message", "Xóa banner thành công"));
    }
}