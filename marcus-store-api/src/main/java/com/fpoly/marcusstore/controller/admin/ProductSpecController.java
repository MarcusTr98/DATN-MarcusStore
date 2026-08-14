package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.ProductSpecsSaveRequest;
import com.fpoly.marcusstore.dto.request.SpecAttributeRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.ProductSpecValueResponse;
import com.fpoly.marcusstore.dto.response.SpecAttributeResponse;
import com.fpoly.marcusstore.service.ProductSpecService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/specs")
@Validated
public class ProductSpecController {

    @Autowired
    private ProductSpecService specService;


@GetMapping("/attributes")
@PreAuthorize("hasAuthority('PRODUCT_VIEW')")
public ApiResponse<List<SpecAttributeResponse>> getAttributesByCategory(
        @RequestParam(required = false) @Positive Integer categoryId) {
    return ApiResponse.success(specService.getAttributesByCategory(categoryId));
}

    @PostMapping("/attributes")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public ApiResponse<SpecAttributeResponse> createAttribute(
            @Valid @RequestBody SpecAttributeRequest req) {
        return ApiResponse.success(specService.createAttribute(req));
    }

    @PutMapping("/attributes/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public ApiResponse<SpecAttributeResponse> updateAttribute(
            @PathVariable @Positive Integer id,
            @Valid @RequestBody SpecAttributeRequest req) {
        return ApiResponse.success(specService.updateAttribute(id, req));
    }

    @DeleteMapping("/attributes/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public ApiResponse<String> deleteAttribute(@PathVariable @Positive Integer id) {
        specService.deleteAttribute(id);
        return ApiResponse.success("Đã xóa thông số thành công!");
    }


    @GetMapping("/products/{productId}")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public ApiResponse<List<ProductSpecValueResponse>> getProductSpecs(
            @PathVariable @Positive Integer productId) {
        return ApiResponse.success(specService.getSpecValuesByProduct(productId));
    }

    @PutMapping("/products")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public ApiResponse<List<ProductSpecValueResponse>> saveProductSpecs(
            @Valid @RequestBody ProductSpecsSaveRequest req) {
        return ApiResponse.success(specService.saveSpecValuesForProduct(req));
    }
}
