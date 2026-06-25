package com.fpoly.marcusstore.repository.core;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fpoly.marcusstore.entity.core.Product;

import java.util.List;

@Repository
public interface ClientProductFilterRepository extends JpaRepository<Product, Integer> {

    @Query(value = """
            SELECT DISTINCT
                a.attribute_id   AS attributeId,
                a.attribute_name AS attributeName,
                v.value_id       AS valueId,
                v.value_string   AS valueString
            FROM Sku_Attribute_Values sav
            JOIN Attribute_Values v ON v.value_id = sav.value_id
            JOIN Attributes a ON a.attribute_id = v.attribute_id
            JOIN Product_Skus s ON s.sku_id = sav.sku_id
            JOIN Products p ON p.product_id = s.product_id
            WHERE s.is_active = 1
              AND p.status = 1
              AND (
                  p.category_id = :parentCategoryId
                  OR p.category_id IN (
                      SELECT child.category_id FROM Categories child WHERE child.parent_id = :parentCategoryId
                  )
              )
            ORDER BY a.attribute_id, v.value_id
            """, nativeQuery = true)
    List<DynamicFilterProjection> findAvailableFiltersByParentCategory(
            @Param("parentCategoryId") Integer parentCategoryId);

    interface DynamicFilterProjection {
        Integer getAttributeId();
        String getAttributeName();
        Integer getValueId();
        String getValueString();
    }
}
