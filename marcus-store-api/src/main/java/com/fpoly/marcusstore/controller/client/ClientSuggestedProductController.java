package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.ClientSuggestedProductResponse;
import com.fpoly.marcusstore.service.ClientSuggestedProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client/products")
public class ClientSuggestedProductController {

    @Autowired
    private ClientSuggestedProductService suggestedProductService;

    @GetMapping("/suggested")
    public ResponseEntity<ApiResponse<List<ClientSuggestedProductResponse>>> getSuggested(
            @RequestParam(required = false) Integer cateID,
            @RequestParam(required = false) Integer limit) {

        List<ClientSuggestedProductResponse> suggested =
                suggestedProductService.getSuggestedProducts(cateID, limit);

        return ResponseEntity.ok(ApiResponse.success(suggested));
    }

    @GetMapping("/best-sellers")
    public ResponseEntity<ApiResponse<List<ClientSuggestedProductResponse>>> bestSellers(
            @RequestParam(required = false, defaultValue = "8") Integer limit) {
        return ResponseEntity.ok(ApiResponse.success(
                suggestedProductService.getBestSellers(limit)));
    }

    @GetMapping("/search/suggest")
    public ResponseEntity<ApiResponse<List<ClientSuggestedProductResponse>>> suggest(
            @RequestParam("q") String q,
            @RequestParam(required = false, defaultValue = "8") Integer limit) {
        return ResponseEntity.ok(ApiResponse.success(
                suggestedProductService.suggestByKeyword(q, limit)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ClientSuggestedProductResponse>>> search(
            @RequestParam("q") String q,
            @RequestParam(required = false) Integer parentCategoryId,
            @RequestParam(required = false) String parentCategorySlug,
            @RequestParam(required = false) Integer brandId,
            @RequestParam(required = false) String brandSlug,
            @RequestParam(required = false, defaultValue = "price_desc") String sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                suggestedProductService.search(q, parentCategoryId, parentCategorySlug,
                        brandId, brandSlug, sortBy, page, size)));
    }
}