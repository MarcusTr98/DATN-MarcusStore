package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.core.Attribute;
import com.fpoly.marcusstore.repository.core.AttributeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AttributeService {

    @Autowired
    private AttributeRepository attributeRepository;

    public List<Attribute> getAllAttributes() {
        return attributeRepository.findAll();
    }

    @Transactional
    public Attribute createAttribute(String name) {
        if (attributeRepository.existsByAttributeName(name)) {
            throw new RuntimeException("Tên thuộc tính đã tồn tại!");
        }
        Attribute attribute = new Attribute();
        attribute.setAttributeName(name);
        return attributeRepository.save(attribute);
    }

    @Transactional
    public Attribute updateAttribute(Integer id, String newName) {
        Attribute attribute = attributeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuộc tính!"));
        if (!attribute.getAttributeName().equals(newName) && attributeRepository.existsByAttributeName(newName)) {
            throw new RuntimeException("Tên thuộc tính đã tồn tại!");
        }
        attribute.setAttributeName(newName);
        return attributeRepository.save(attribute);
    }

    @Transactional
    public void deleteAttribute(Integer id) {
        if (!attributeRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy thuộc tính!");
        }
        attributeRepository.deleteById(id);
    }
}