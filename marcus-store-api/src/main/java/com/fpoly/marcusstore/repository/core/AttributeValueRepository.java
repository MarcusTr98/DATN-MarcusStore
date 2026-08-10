package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.AttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface AttributeValueRepository extends JpaRepository<AttributeValue, Integer> {
        List<AttributeValue> findByAttribute_AttributeId(Integer attributeId);

        boolean existsByValueStringAndAttribute_AttributeId(String valueString, Integer attributeId);

        boolean existsByValueStringIgnoreCaseAndAttribute_AttributeId(String valueString, Integer attributeId);

        boolean existsByValueStringIgnoreCaseAndAttribute_AttributeIdAndValueIdNot(
                        String valueString, Integer attributeId, Integer valueId);

        // Marcus thêm: bảo vệ căn cước biến thể của SKU/đơn hàng/kho/bảo hành.
        @Query(value = "SELECT CASE WHEN EXISTS (SELECT 1 FROM Sku_Attribute_Values WHERE value_id = :valueId) THEN 1 ELSE 0 END", nativeQuery = true)
        boolean isUsedBySku(@Param("valueId") Integer valueId);

        @Query(value = "SELECT CASE WHEN EXISTS (SELECT 1 FROM Sku_Attribute_Values sav "
                        + "INNER JOIN Attribute_Values av ON av.value_id = sav.value_id "
                        + "WHERE av.attribute_id = :attributeId) THEN 1 ELSE 0 END", nativeQuery = true)
        boolean isAttributeUsedBySku(@Param("attributeId") Integer attributeId);
}
