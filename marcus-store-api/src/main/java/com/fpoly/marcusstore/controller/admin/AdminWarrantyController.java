package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.UpdateWarrantyStatusRequest;
import com.fpoly.marcusstore.dto.response.WarrantyResponse;
import com.fpoly.marcusstore.security.jwt.JwtUtils;
import com.fpoly.marcusstore.service.WarrantyService;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Key;
import java.util.List;

@RestController
@RequestMapping("/api/admin/warranties")
@RequiredArgsConstructor
public class AdminWarrantyController {

    private final WarrantyService warrantyService;
    private final JwtUtils jwtUtils;

    @Value("${marcusstore.app.jwtSecret}")
    private String jwtSecret;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarrantyResponse>>> getAllWarranties(
            @RequestParam(required = false) String status) {

        List<WarrantyResponse> warranties;
        if (status != null && !status.isEmpty()) {
            warranties = warrantyService.getWarrantiesByStatus(status);
        } else {
            warranties = warrantyService.getAllWarranties();
        }
        return ResponseEntity.ok(ApiResponse.success(warranties));
    }

    @GetMapping("/{warrantyId}")
    public ResponseEntity<ApiResponse<WarrantyResponse>> getWarrantyDetail(
            @PathVariable Integer warrantyId) {

        WarrantyResponse warranty = warrantyService.getWarrantyById(warrantyId);
        return ResponseEntity.ok(ApiResponse.success(warranty));
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
