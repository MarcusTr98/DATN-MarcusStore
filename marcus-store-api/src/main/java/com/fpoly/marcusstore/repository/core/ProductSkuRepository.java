package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.ProductSku;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Khóa PESSIMISTIC_WRITE
    // ai gọi hàm này đều sẽ khóa cứng các Row SKU tương ứng trong DB
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ProductSku s WHERE s.skuId IN :skuIds")
    List<ProductSku> findByIdsForUpdate(@Param("skuIds") List<Integer> skuIds);
}