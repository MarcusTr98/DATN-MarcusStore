package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttributeRepository extends JpaRepository<Attribute, Integer> {
    boolean existsByAttributeName(String attributeName);

    // Marcus thêm: tên thuộc tính trùng nhau không phụ thuộc hoa/thường.
    boolean existsByAttributeNameIgnoreCase(String attributeName);

    boolean existsByAttributeNameIgnoreCaseAndAttributeIdNot(String attributeName, Integer attributeId);
}
