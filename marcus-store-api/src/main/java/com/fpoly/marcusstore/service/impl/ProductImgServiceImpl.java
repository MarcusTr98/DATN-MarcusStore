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

    private String extractPublicId(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains("/upload/")) {
            return null;
        }
        String[] parts = imageUrl.split("/upload/");
        if (parts.length < 2 || parts[1] == null || parts[1].isBlank()) {
            return null;
        }
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
    @Transactional
    public ProductImgResponse createProductImg(Integer productId, MultipartFile file, ProductImgRequest imgRequest) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + productId));

        String imageUrl;
        try {
            imageUrl = cloudinaryService.uploadImage(file);
        } catch (IOException e) {
            throw new RuntimeException("Upload ảnh thất bại");
        }

        List<ProductImage> images = imgRepo.findByProduct_ProductId(productId);

        ProductImage productImage = new ProductImage();
        productImage.setImageUrl(imageUrl);
        productImage.setProduct(product);

        if (Boolean.TRUE.equals(imgRequest.getIsPrimary())) {
            List<ProductImage> secondaryImages = images.stream()
                    .sorted((a, b) -> {
                        Integer oa = a.getDisplayOrder() != null ? a.getDisplayOrder() : Integer.MAX_VALUE;
                        Integer ob = b.getDisplayOrder() != null ? b.getDisplayOrder() : Integer.MAX_VALUE;
                        return oa.compareTo(ob);
                    })
                    .collect(Collectors.toList());

            int order = 1;
            for (ProductImage img : secondaryImages) {
                img.setIsPrimary(false);
                img.setDisplayOrder(order++);
            }
            imgRepo.saveAll(secondaryImages);

            productImage.setIsPrimary(true);
            productImage.setDisplayOrder(null);

            product.setThumbnailUrl(imageUrl);
            productRepo.save(product);
        } else {
            long secondaryCount = images.stream()
                    .filter(img -> !Boolean.TRUE.equals(img.getIsPrimary()))
                    .count();
            productImage.setIsPrimary(false);
            productImage.setDisplayOrder((int) secondaryCount + 1);
        }

        return toImgResponse(imgRepo.save(productImage));
    }

    @Override
    @Transactional
    public ProductImgResponse updateProductImg(MultipartFile file, ProductImgRequest imgRequest, Integer id) {
        ProductImage productImage = imgRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh với id: " + id));

        if (file != null && !file.isEmpty()) {
            try {
                String oldPublicId = extractPublicId(productImage.getImageUrl());
                if (oldPublicId != null) {
                    cloudinaryService.deleteImage(oldPublicId);
                }
                String newImageUrl = cloudinaryService.uploadImage(file);

                if (!newImageUrl.equals(productImage.getImageUrl())) {
                    productImage.setImageUrl(newImageUrl);
                }
            } catch (IOException e) {
                throw new RuntimeException("Xử lý ảnh thất bại");
            }
        }

        if (imgRequest.getIsPrimary() != null) {
            if (Boolean.TRUE.equals(imgRequest.getIsPrimary())) {
                Product product = productImage.getProduct();
                List<ProductImage> images = imgRepo.findByProduct_ProductId(product.getProductId());

                List<ProductImage> secondaryImages = images.stream()
                        .filter(img -> !img.getImageId().equals(productImage.getImageId()))
                        .sorted((a, b) -> {
                            Integer oa = a.getDisplayOrder() != null ? a.getDisplayOrder() : Integer.MAX_VALUE;
                            Integer ob = b.getDisplayOrder() != null ? b.getDisplayOrder() : Integer.MAX_VALUE;
                            return oa.compareTo(ob);
                        })
                        .collect(Collectors.toList());

                int order = 1;
                for (ProductImage img : secondaryImages) {
                    img.setIsPrimary(false);
                    img.setDisplayOrder(order++);
                }
                imgRepo.saveAll(secondaryImages);

                productImage.setIsPrimary(true);
                productImage.setDisplayOrder(null);

                product.setThumbnailUrl(productImage.getImageUrl());
                productRepo.save(product);
            } else {
                boolean wasPrimary = Boolean.TRUE.equals(productImage.getIsPrimary());
                productImage.setIsPrimary(false);

                if (wasPrimary) {
                    List<ProductImage> images = imgRepo.findByProduct_ProductId(productImage.getProduct().getProductId());
                    long secondaryCount = images.stream()
                            .filter(img -> !Boolean.TRUE.equals(img.getIsPrimary())
                                    && !img.getImageId().equals(productImage.getImageId()))
                            .count();
                    productImage.setDisplayOrder((int) secondaryCount + 1);
                }
            }
        }

        if (imgRequest.getDisplayOrder() != null && !Boolean.TRUE.equals(productImage.getIsPrimary())) {
            productImage.setDisplayOrder(imgRequest.getDisplayOrder());
        }

        return toImgResponse(imgRepo.save(productImage));
    }

    @Override
    @Transactional
    public void deleteProductImg(Integer id) {
        ProductImage productImage = imgRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh với id: " + id));

        Product product = productImage.getProduct();
        boolean wasPrimary = Boolean.TRUE.equals(productImage.getIsPrimary());

        try {
            String publicId = extractPublicId(productImage.getImageUrl());
            if (publicId != null) {
                cloudinaryService.deleteImage(publicId);
            }
        } catch (IOException e) {
            throw new RuntimeException("Xóa ảnh thất bại");
        }

        imgRepo.delete(productImage);

        List<ProductImage> remainingImages = imgRepo.findByProduct_ProductId(product.getProductId());

        if (wasPrimary) {
            List<ProductImage> sorted = remainingImages.stream()
                    .sorted((a, b) -> {
                        Integer oa = a.getDisplayOrder() != null ? a.getDisplayOrder() : Integer.MAX_VALUE;
                        Integer ob = b.getDisplayOrder() != null ? b.getDisplayOrder() : Integer.MAX_VALUE;
                        return oa.compareTo(ob);
                    })
                    .collect(Collectors.toList());

            if (!sorted.isEmpty()) {
                ProductImage newPrimary = sorted.get(0);
                List<ProductImage> newSecondary = sorted.subList(1, sorted.size());

                int order = 1;
                for (ProductImage img : newSecondary) {
                    img.setIsPrimary(false);
                    img.setDisplayOrder(order++);
                }
                imgRepo.saveAll(newSecondary);

                newPrimary.setIsPrimary(true);
                newPrimary.setDisplayOrder(null);
                imgRepo.save(newPrimary);

                product.setThumbnailUrl(newPrimary.getImageUrl());
                productRepo.save(product);
            } else {
                product.setThumbnailUrl(null);
                productRepo.save(product);
            }
        } else {
            List<ProductImage> secondaryImages = remainingImages.stream()
                    .filter(img -> !Boolean.TRUE.equals(img.getIsPrimary()))
                    .sorted((a, b) -> {
                        Integer oa = a.getDisplayOrder() != null ? a.getDisplayOrder() : Integer.MAX_VALUE;
                        Integer ob = b.getDisplayOrder() != null ? b.getDisplayOrder() : Integer.MAX_VALUE;
                        return oa.compareTo(ob);
                    })
                    .collect(Collectors.toList());

            int order = 1;
            for (ProductImage img : secondaryImages) {
                img.setDisplayOrder(order++);
            }
            imgRepo.saveAll(secondaryImages);
        }
    }
}