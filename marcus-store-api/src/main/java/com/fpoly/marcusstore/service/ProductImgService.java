package com.fpoly.marcusstore.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import com.fpoly.marcusstore.dto.request.ProductImgRequest;
import com.fpoly.marcusstore.dto.response.ProductImgResponse;

public interface ProductImgService {

    List<ProductImgResponse> getProductImgByIdProduct (Integer productId);

    ProductImgResponse createProductImg(Integer productId, MultipartFile file, ProductImgRequest imgRequest);

    List<ProductImgResponse> createMultiProductImg(Integer productId, List<MultipartFile> files, ProductImgRequest imgRequest);

    ProductImgResponse updateProductImg (MultipartFile file,ProductImgRequest productImgRequest, Integer id);

    void deleteProductImg (Integer id);
}
