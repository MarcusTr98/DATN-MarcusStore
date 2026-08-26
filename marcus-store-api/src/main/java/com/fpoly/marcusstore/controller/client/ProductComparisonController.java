package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.ProductComparisonRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.ProductComparisonResponse;
import com.fpoly.marcusstore.service.ProductComparisonService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/client/compare")
public class ProductComparisonController {

    private final ProductComparisonService comparisonService;

    public ProductComparisonController(ProductComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductComparisonResponse>> compareProducts(
            @Valid @RequestBody ProductComparisonRequest request) {
        ProductComparisonResponse result = comparisonService.compareProducts(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
