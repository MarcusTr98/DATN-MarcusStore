package com.fpoly.marcusstore.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fpoly.marcusstore.dto.request.ProductImgRequest;
import com.fpoly.marcusstore.dto.response.ProductImgResponse;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductImage;
import com.fpoly.marcusstore.repository.core.ProductImgRepository;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import com.fpoly.marcusstore.service.CloudinaryService;
import com.fpoly.marcusstore.service.ProductImgService;

@Service
public class ProductImgServiceImpl implements ProductImgService {
    @Autowired
    private ProductImgRepository imgRepo;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private CloudinaryService cloudinaryService;

    private ProductImgResponse toImgResponse(ProductImage productImg) {
        return ProductImgResponse.builder()
                .imageId(productImg.getImageId())
                .imageUrl(productImg.getImageUrl())
                .isPrimary(productImg.getIsPrimary())
                .displayOrder(productImg.getDisplayOrder())
                .build();
    }

    // Tách publicId từ ImageUrl trong db để xóa ảnh trên Cloundinary
    private String extractPublicId(String imageUrl) {
        String[] parts = imageUrl.split("/upload/");
        String afterUpload = parts[1];
        return afterUpload.replaceFirst("v\\d+/", "").replaceAll("\\.[^.]+$", "");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImgResponse> getProductImgByIdProduct(Integer productId) {
        List<ProductImage> findAllImgByProductId = imgRepo.findByProduct_ProductId(productId);
        return findAllImgByProductId.stream().map(this::toImgResponse).collect(Collectors.toList());
    }

    @Override
    public ProductImgResponse createProductImg(Integer productId, MultipartFile file, ProductImgRequest imgRequest) {
        String imageUrl;
        try {
            imageUrl = cloudinaryService.uploadImage(file);
        } catch (IOException e) {
            throw new RuntimeException("Upload ảnh thất bại");
        }

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + productId));

        ProductImage productImage = new ProductImage();
        productImage.setImageUrl(imageUrl);
        productImage.setIsPrimary(imgRequest.getIsPrimary() != null ? imgRequest.getIsPrimary() : false);
        productImage.setDisplayOrder(imgRequest.getDisplayOrder() != null ? imgRequest.getDisplayOrder() : 0);
        productImage.setProduct(product);

        return toImgResponse(imgRepo.save(productImage));
    }

    @Override
    public ProductImgResponse updateProductImg(MultipartFile file, ProductImgRequest imgRequest, Integer id) {
        ProductImage productImage = imgRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh với id: " + id));

        try {
            String oldPublicId = extractPublicId(productImage.getImageUrl());
            cloudinaryService.deleteImage(oldPublicId);
            String newImageUrl = cloudinaryService.uploadImage(file);
            productImage.setImageUrl(newImageUrl);
        } catch (IOException e) {
            throw new RuntimeException("Xử lý ảnh thất bại: ");
        }

        productImage.setIsPrimary(imgRequest.getIsPrimary() != null ? imgRequest.getIsPrimary() : false);
        productImage.setDisplayOrder(imgRequest.getDisplayOrder() != null ? imgRequest.getDisplayOrder() : 0);

        return toImgResponse(imgRepo.save(productImage));
    }

    @Override
    public void deleteProductImg(Integer id){
        ProductImage productImage = imgRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh với id: " + id));

        try {
            String publicId = extractPublicId(productImage.getImageUrl());
            cloudinaryService.deleteImage(publicId);

            imgRepo.delete(productImage);
        } catch (IOException e) {
            throw new RuntimeException("Xóa ảnh thất bại");
        }
    }
}
