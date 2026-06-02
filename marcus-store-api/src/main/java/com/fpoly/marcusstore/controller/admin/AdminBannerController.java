package com.fpoly.marcusstore.controller.admin;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import com.fpoly.marcusstore.entity.cms.Banner;
import com.fpoly.marcusstore.service.BannerService;

@RequestMapping("/banners")
@RestController
public class AdminBannerController {

    @Autowired
    BannerService ser;

    // xem tất cả banner
    @GetMapping("/all")
    public List<Map<String, Object>> getAll() {
        return ser.getAll();
    }

    // xem chi tiết banner
    @GetMapping("/{id}")
    public Optional<Map<String, Object>> getOne(@PathVariable Integer id) {
        return ser.getOne(id);
    }

    // thêm
    @PostMapping
    public Banner add(@RequestBody Map<String, Object> req) {
        return ser.add(req);
    }

    // sửa
    @PutMapping("/{id}")
    public Banner update(@PathVariable Integer id,
                         @RequestBody Map<String, Object> req) {
        return ser.update(id, req);
    }

    // xóa mềm
    @DeleteMapping("/{id}")
    public Map<String, Object> remove(@PathVariable Integer id) {
        ser.remove(id);
        return Map.of("message", "remove oke");
    }
}