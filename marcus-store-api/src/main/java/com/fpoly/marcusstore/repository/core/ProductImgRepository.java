package com.fpoly.marcusstore.repository.core;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.fpoly.marcusstore.entity.core.ProductImage;

@Repository
public interface ProductImgRepository extends JpaRepository<ProductImage, Integer>{
    //để tim theo câu lệnh SELECT * FROM Product_Images WHERE product_id = ?
    List<ProductImage> findByProduct_ProductId(Integer productId);
}
