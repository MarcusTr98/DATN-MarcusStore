package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.ProductSpecValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSpecValueRepository extends JpaRepository<ProductSpecValue, Integer> {

    List<ProductSpecValue> findByProductProductId(Integer productId);

    @Query("SELECT psv FROM ProductSpecValue psv " +
            "JOIN FETCH psv.specAttribute " +
            "WHERE psv.product.productId = :productId " +
            "ORDER BY psv.specAttribute.displayOrder ASC")
    List<ProductSpecValue> findByProductIdWithSpec(@Param("productId") Integer productId);

    long countBySpecAttributeSpecAttributeId(Integer specAttributeId);
}
