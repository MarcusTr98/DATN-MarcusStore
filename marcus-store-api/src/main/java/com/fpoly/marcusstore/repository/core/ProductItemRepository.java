package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.ProductItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductItemRepository extends JpaRepository<ProductItem, Integer> {

    List<ProductItem> findByProductSku_SkuIdOrderByItemIdDesc(Integer skuId);

    long countByProductSku_SkuIdAndStatus(Integer skuId, Integer status);

    @Query("SELECT COUNT(pi) FROM ProductItem pi WHERE pi.productSku.skuId = :skuId")
    long countBySkuId(@Param("skuId") Integer skuId);

    boolean existsByImeiCode(String imeiCode);

    @Query("SELECT pi.imeiCode FROM ProductItem pi WHERE pi.imeiCode IN :codes")
    List<String> findExistingImeiCodes(@Param("codes") List<String> codes);
}
