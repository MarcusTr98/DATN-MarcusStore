package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.AttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AttributeValueRepository extends JpaRepository<AttributeValue, Integer> {
    List<AttributeValue> findByAttribute_AttributeId(Integer attributeId);

    boolean existsByValueStringAndAttribute_AttributeId(String valueString, Integer attributeId);
}