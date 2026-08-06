package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.UpdateWarrantyStatusRequest;
import com.fpoly.marcusstore.dto.response.WarrantyResponse;
import com.fpoly.marcusstore.entity.shopping.WarrantyReturn.WarrantyReason;
import com.fpoly.marcusstore.entity.shopping.WarrantyReturn.WarrantyStatus;
import com.fpoly.marcusstore.security.jwt.JwtUtils;
import com.fpoly.marcusstore.service.WarrantyService;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/warranties")
@RequiredArgsConstructor
public class AdminWarrantyController {

    private final WarrantyService warrantyService;
    private final JwtUtils jwtUtils;

    @Value("${marcusstore.app.jwtSecret}")
    private String jwtSecret;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<WarrantyResponse>>> getAllWarranties(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        WarrantyStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = WarrantyStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        WarrantyReason reasonEnum = null;
        if (reason != null && !reason.isBlank()) {
            try {
                reasonEnum = WarrantyReason.valueOf(reason.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        PageRequest pageable = PageRequest.of(page, size);
        Page<WarrantyResponse> result = warrantyService.getWarrantiesPage(statusEnum, reasonEnum, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{warrantyId}")
    public ResponseEntity<ApiResponse<WarrantyResponse>> getWarrantyDetail(
            @PathVariable Integer warrantyId) {

        WarrantyResponse warranty = warrantyService.getWarrantyById(warrantyId);
        return ResponseEntity.ok(ApiResponse.success(warranty));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getWarrantyStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", warrantyService.countByStatus(WarrantyStatus.PENDING));
        stats.put("approved", warrantyService.countByStatus(WarrantyStatus.APPROVED));
        stats.put("rejected", warrantyService.countByStatus(WarrantyStatus.REJECTED));
        stats.put("total", warrantyService.countAll());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PutMapping("/{warrantyId}/status")
    public ResponseEntity<ApiResponse<WarrantyResponse>> updateStatus(
            @PathVariable Integer warrantyId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateWarrantyStatusRequest request) {

        Integer adminId = extractUserIdFromToken(authHeader);

        WarrantyResponse updated =
                warrantyService.updateWarrantyStatus(
                        warrantyId,
                        adminId,
                        request
                );

        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    private Integer extractUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Key key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("uid", Integer.class);
    }
}
