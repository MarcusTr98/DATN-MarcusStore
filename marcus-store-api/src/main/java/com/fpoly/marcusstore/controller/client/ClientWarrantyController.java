package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.CreateWarrantyRequest;
import com.fpoly.marcusstore.dto.response.WarrantyResponse;
import com.fpoly.marcusstore.security.jwt.JwtUtils;
import com.fpoly.marcusstore.service.WarrantyService;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Key;
import java.util.List;

@RestController
@RequestMapping("/api/client/warranties")
@RequiredArgsConstructor
public class ClientWarrantyController {

    private final WarrantyService warrantyService;
    private final JwtUtils jwtUtils;

    @Value("${marcusstore.app.jwtSecret}")
    private String jwtSecret;

    @PostMapping
    public ResponseEntity<ApiResponse<WarrantyResponse>> createWarranty(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateWarrantyRequest request) {

        Integer userId = extractUserIdFromToken(authHeader);
        WarrantyResponse response = warrantyService.createWarranty(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarrantyResponse>>> getMyWarranties(
            @RequestHeader("Authorization") String authHeader) {

        Integer userId = extractUserIdFromToken(authHeader);
        List<WarrantyResponse> warranties =
                warrantyService.getWarrantiesByUser(userId);

        return ResponseEntity.ok(
                ApiResponse.success(warranties)
        );
    }

    @GetMapping("/{warrantyId}")
    public ResponseEntity<ApiResponse<WarrantyResponse>> getWarrantyDetail(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer warrantyId) {

        Integer userId = extractUserIdFromToken(authHeader);
        WarrantyResponse warranty =
                warrantyService.getWarrantyById(warrantyId);

        if (!warranty.getUserId().equals(userId)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(
                            403,
                            "Bạn không có quyền xem yêu cầu này"
                    ));
        }

        return ResponseEntity.ok(
                ApiResponse.success(warranty)
        );
    }

    @GetMapping("/check/{orderItemId}")
    public ResponseEntity<ApiResponse<Boolean>> canRequestWarranty(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer orderItemId) {

        Integer userId = extractUserIdFromToken(authHeader);
        boolean canRequest =
                warrantyService.canRequestWarranty(userId, orderItemId);

        return ResponseEntity.ok(
                ApiResponse.success(canRequest)
        );
    }
    
    @GetMapping("/order-item/{orderItemId}")
    public ResponseEntity<ApiResponse<WarrantyResponse>> getWarrantyByOrderItem(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer orderItemId) {

        Integer userId = extractUserIdFromToken(authHeader);
        WarrantyResponse response = warrantyService.getWarrantyByOrderItemId(userId, orderItemId);

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    private Integer extractUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");

        Key key = io.jsonwebtoken.security.Keys
                .hmacShaKeyFor(jwtSecret.getBytes());

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("uid", Integer.class);
    }
}
