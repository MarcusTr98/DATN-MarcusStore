package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.ProductSku;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSkuRepository extends JpaRepository<ProductSku, Integer>,
        JpaSpecificationExecutor<ProductSku> {

    Optional<ProductSku> findBySkuCode(String skuCode);

    boolean existsBySkuCode(String skuCode);

    boolean existsBySkuCodeIgnoreCase(String skuCode);

    boolean existsByProductProductId(Integer productId);

    @EntityGraph(attributePaths = { "attributeValues" })
    List<ProductSku> findByProductProductIdAndIsActiveTrue(Integer productId);

    @EntityGraph(attributePaths = { "attributeValues", "attributeValues.attribute" })
    List<ProductSku> findByProductProductId(Integer productId);

    @EntityGraph(attributePaths = { "attributeValues" })
    Optional<ProductSku> findBySkuId(Integer skuId);

    // Khóa PESSIMISTIC_WRITE
    // ai gọi hàm này đều sẽ khóa cứng các Row SKU tương ứng trong DB
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ProductSku s WHERE s.skuId IN :skuIds")
    List<ProductSku> findByIdsForUpdate(@Param("skuIds") List<Integer> skuIds);

    // Marcus thêm: khóa đúng một SKU khi chỉnh giá để hai Admin không ghi đè nhau.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = { "attributeValues", "attributeValues.attribute" })
    @Query("SELECT s FROM ProductSku s WHERE s.skuId = :skuId")
    Optional<ProductSku> findByIdForUpdate(@Param("skuId") Integer skuId);

    @EntityGraph(attributePaths = { "attributeValues", "attributeValues.attribute" })
    List<ProductSku> findBySkuIdIn(List<Integer> skuIds);
}
