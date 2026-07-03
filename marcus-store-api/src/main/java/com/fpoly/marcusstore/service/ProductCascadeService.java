package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.ProductCascadeResponse;

import java.util.List;

public interface ProductCascadeService {



    List<ProductCascadeResponse> getAllProductCascade(boolean includeOutOfStock);


     // Lấy cây sản phẩm của 1 brand cụ thể.
      //Trả về null nếu brand không có sản phẩm active.

    ProductCascadeResponse getProductCascadeByBrand(String brand, boolean includeOutOfStock);

    // ---- Overload giữ tương thích ngược, mặc định ẩn SKU hết hàng ----
    default List<ProductCascadeResponse> getAllProductCascade() {
        return getAllProductCascade(false);
    }

    default ProductCascadeResponse getProductCascadeByBrand(String brand) {
        return getProductCascadeByBrand(brand, false);
    }
}
