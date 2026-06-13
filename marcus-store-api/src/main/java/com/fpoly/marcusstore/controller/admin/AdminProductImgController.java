package com.fpoly.marcusstore.controller.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fpoly.marcusstore.dto.request.ProductImgRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.ProductImgResponse;
import com.fpoly.marcusstore.service.ProductImgService;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductImgController {
    @Autowired
    ProductImgService productImgService;

    @GetMapping("/{productId}/images")
    public ApiResponse<List<ProductImgResponse>> getImagesByProductId(@PathVariable Integer productId) {
        return ApiResponse.success(productImgService.getProductImgByIdProduct(productId));
    }

    @PostMapping("/{productId}/images")
    public ApiResponse<ProductImgResponse> createProductImg(
            @PathVariable Integer productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrimary", required = false, defaultValue = "false") Boolean isPrimary,
            @RequestParam(value = "displayOrder", required = false, defaultValue = "0") Integer displayOrder) {

        ProductImgRequest productImgRequest = new ProductImgRequest();
        productImgRequest.setIsPrimary(isPrimary);
        productImgRequest.setDisplayOrder(displayOrder);

        return ApiResponse.success(productImgService.createProductImg(productId, file, productImgRequest));
    }

    @PutMapping("/images/{id}")
    public ApiResponse<ProductImgResponse> updateProductImg(
            @PathVariable Integer id,
            @RequestParam(value="file", required = false) MultipartFile file,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder) {

        ProductImgRequest productImgRequest = new ProductImgRequest();
        productImgRequest.setIsPrimary(isPrimary);
        productImgRequest.setDisplayOrder(displayOrder);

        return ApiResponse.success(productImgService.updateProductImg(file, productImgRequest, id));
    }

     @DeleteMapping("/images/{id}")
    public ApiResponse<String> deleteProductImg(@PathVariable Integer id) {
        productImgService.deleteProductImg(id);
        return ApiResponse.success("Xóa ảnh thành công");
    }
}
