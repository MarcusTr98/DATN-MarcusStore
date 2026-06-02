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
    public AttributeValue createValue(Integer attributeId, String valueString) {
        if (valueRepository.existsByValueStringAndAttribute_AttributeId(valueString, attributeId)) {
            throw new RuntimeException("Giá trị này đã tồn tại trong thuộc tính!");
        }
        AttributeValue value = new AttributeValue();
        Attribute attribute = new Attribute();
        attribute.setAttributeId(attributeId);
        value.setAttribute(attribute);
        value.setValueString(valueString);
        return valueRepository.save(value);
    }

    @Transactional
    public AttributeValue updateValue(Integer id, String newValue) {
        AttributeValue value = valueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giá trị!"));
        value.setValueString(newValue);
        return valueRepository.save(value);
    }

    @Transactional
    public void deleteValue(Integer id) {
        valueRepository.deleteById(id);
    }
}