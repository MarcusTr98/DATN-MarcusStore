package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.ProductSku;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSkuRepository extends JpaRepository<ProductSku, Integer> {

    Optional<ProductSku> findBySkuCode(String skuCode);

    @EntityGraph(attributePaths = { "attributeValues" })
    List<ProductSku> findByProductProductIdAndIsActiveTrue(Integer productId);

    @EntityGraph(attributePaths = { "attributeValues" })
    Optional<ProductSku> findBySkuId(Integer skuId);
}