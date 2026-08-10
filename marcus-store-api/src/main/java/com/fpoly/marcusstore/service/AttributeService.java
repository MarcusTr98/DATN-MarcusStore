package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.core.Attribute;
import com.fpoly.marcusstore.repository.core.AttributeRepository;
import com.fpoly.marcusstore.repository.core.AttributeValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AttributeService {

    @Autowired
    private AttributeRepository attributeRepository;

    @Autowired
    private AttributeValueRepository attributeValueRepository;

    public List<Attribute> getAllAttributes() {
        return attributeRepository.findAll();
    }

    @Transactional
    public Attribute createAttribute(String name) {
        String normalizedName = normalize(name);
        if (attributeRepository.existsByAttributeNameIgnoreCase(normalizedName)) {
            throw new RuntimeException("Tên thuộc tính đã tồn tại!");
        }
        Attribute attribute = new Attribute();
        attribute.setAttributeName(normalizedName);
        return attributeRepository.save(attribute);
    }

    @Transactional
    public Attribute updateAttribute(Integer id, String newName) {
        Attribute attribute = attributeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuộc tính!"));
        String normalizedName = normalize(newName);
        if (attributeRepository.existsByAttributeNameIgnoreCaseAndAttributeIdNot(normalizedName, id)) {
            throw new RuntimeException("Tên thuộc tính đã tồn tại!");
        }
        attribute.setAttributeName(normalizedName);
        return attributeRepository.save(attribute);
    }

    @Transactional
    public void deleteAttribute(Integer id) {
        Attribute attribute = attributeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuộc tính!"));
        // Marcus thêm: Attribute đã tham gia tạo SKU không được hard-delete vì
        // sẽ phá dữ liệu lịch sử mà Kho/IMEI/Bảo hành của thành viên sử dụng.
        if (attributeValueRepository.isAttributeUsedBySku(id)) {
            throw new IllegalStateException(
                    "Thuộc tính đã được sử dụng để tạo SKU nên không thể xóa. Hãy ngừng hoạt động SKU liên quan.");
        }
        attributeRepository.delete(attribute);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên thuộc tính không được để trống!");
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
