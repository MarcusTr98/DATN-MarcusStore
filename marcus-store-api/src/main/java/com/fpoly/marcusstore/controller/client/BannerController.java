package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.response.BannerResponseDTO;
import com.fpoly.marcusstore.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Controller public - dành cho trang Home (khách vãng lai, không cần đăng nhập).
// Route này nằm dưới /api/public/** nên KHÔNG bị chặn bởi:
// .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "STAFF")
// vì đã có sẵn: .requestMatchers("/api/public/**").permitAll()
@RestController
@RequestMapping("/api/public/banners")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping("/positions")
    public List<Map<String, Object>> getPositions() {
        return bannerService.getAllPositions();
    }

    @GetMapping
    public List<BannerResponseDTO> getActiveBanners() {
        return bannerService.getActiveBanners(); // chỉ trả banner isActive = true
    }
}
