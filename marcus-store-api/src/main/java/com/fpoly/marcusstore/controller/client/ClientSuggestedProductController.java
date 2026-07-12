package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.ClientSuggestedProductResponse;
import com.fpoly.marcusstore.service.ClientSuggestedProductService;
import org.springframework.beans.factory.annotation.Autowired;
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
}