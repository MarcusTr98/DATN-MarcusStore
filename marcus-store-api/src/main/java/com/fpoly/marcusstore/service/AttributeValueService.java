package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.core.Attribute;
import com.fpoly.marcusstore.entity.core.AttributeValue;
import com.fpoly.marcusstore.repository.core.AttributeValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AttributeValueService {

    @Autowired
    private AttributeValueRepository valueRepository;

    public List<AttributeValue> getValuesByAttributeId(Integer attributeId) {
        return valueRepository.findByAttribute_AttributeId(attributeId);
    }

    @Transactional
    public AttributeValue createValue(Integer attributeId, String valueString, String valueMeta) {
        String normalizedValue = normalize(valueString);
        if (valueRepository.existsByValueStringIgnoreCaseAndAttribute_AttributeId(normalizedValue, attributeId)) {
            throw new RuntimeException("Giá trị này đã tồn tại trong thuộc tính!");
        }
        AttributeValue value = new AttributeValue();
        Attribute attribute = new Attribute();
        attribute.setAttributeId(attributeId);
        value.setAttribute(attribute);
        value.setValueString(normalizedValue);
        value.setValueMeta(valueMeta);
        return valueRepository.save(value);
    }

    @Transactional
    public AttributeValue updateValue(Integer id, String newValue, String valueMeta) {
        AttributeValue value = valueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giá trị!"));
        String normalizedValue = normalize(newValue);
        if (valueRepository.existsByValueStringIgnoreCaseAndAttribute_AttributeIdAndValueIdNot(
                normalizedValue, value.getAttribute().getAttributeId(), id)) {
            throw new RuntimeException("Giá trị này đã tồn tại trong thuộc tính!");
        }
        value.setValueString(normalizedValue);
        value.setValueMeta(valueMeta);
        return valueRepository.save(value);
    }

    @Transactional
    public void deleteValue(Integer id) {
        AttributeValue value = valueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giá trị!"));
        // Marcus thêm: không làm mất liên kết biến thể mà SKU đã sử dụng.
        if (valueRepository.isUsedBySku(id)) {
            throw new IllegalStateException(
                    "Giá trị thuộc tính đã được SKU sử dụng nên không thể xóa. Hãy ngừng hoạt động SKU liên quan.");
        }
        valueRepository.delete(value);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Giá trị thuộc tính không được để trống!");
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
