package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.ProductSpecsSaveRequest;
import com.fpoly.marcusstore.dto.request.SpecAttributeRequest;
import com.fpoly.marcusstore.dto.response.ProductSpecValueResponse;
import com.fpoly.marcusstore.dto.response.SpecAttributeResponse;
import com.fpoly.marcusstore.entity.core.Category;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSpecValue;
import com.fpoly.marcusstore.entity.core.SpecAttribute;
import com.fpoly.marcusstore.repository.core.CategoryRepository;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import com.fpoly.marcusstore.repository.core.ProductSpecValueRepository;
import com.fpoly.marcusstore.repository.core.SpecAttributeRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProductSpecService {

    @Autowired
    private SpecAttributeRepository specAttrRepo;

    @Autowired
    private ProductSpecValueRepository specValueRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private ProductRepository productRepo;

    // ===== SpecAttribute CRUD =====

    // Lấy parent category ID (nếu là category con thì trả về parent, ngược lại trả về chính nó)
    private Integer resolveEffectiveCategoryId(Integer categoryId) {
        if (categoryId == null) return null;
        Category cat = categoryRepo.findById(categoryId).orElse(null);
        if (cat != null && cat.getParent() != null) {
            return cat.getParent().getCategoryId();
        }
        return categoryId;
    }

    @Transactional(readOnly = true)
public List<SpecAttributeResponse> getAttributesByCategory(Integer categoryId) {
    if (categoryId == null) {
        return List.of();
    }
    // Luôn lấy spec của parent category
    Integer effectiveCategoryId = resolveEffectiveCategoryId(categoryId);
    List<SpecAttribute> attrs = specAttrRepo.findByCategoryIdsWithCategory(List.of(effectiveCategoryId));
    // Fallback về category trực tiếp nếu parent không có spec
    if (attrs.isEmpty()) {
        attrs = specAttrRepo.findByCategoryIdsWithCategory(List.of(categoryId));
    }
    return attrs.stream().map(this::toAttrResponse).toList();
}

    @Transactional(rollbackFor = Exception.class)
    public SpecAttributeResponse createAttribute(SpecAttributeRequest req) {
        if (req == null) {
            throw new RuntimeException("Dữ liệu không hợp lệ");
        }
        String name = req.getName() == null ? null : req.getName().trim();
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("Tên thông số không được để trống");
        }
        // Luôn tạo attribute cho parent category (nếu có)
        Integer effectiveCategoryId = resolveEffectiveCategoryId(req.getCategoryId());
        Category category = categoryRepo.findById(effectiveCategoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục!"));
        if (specAttrRepo.existsByCategoryCategoryIdAndName(category.getCategoryId(), name)) {
            throw new RuntimeException("Tên thông số đã tồn tại trong danh mục này!");
        }
        SpecAttribute attr = new SpecAttribute();
        attr.setCategory(category);
        attr.setName(name);
        attr.setUnit(emptyToNull(req.getUnit()));
        attr.setDataType(req.getDataType() == null ? "text" : req.getDataType());
        attr.setDisplayOrder(req.getDisplayOrder() == null ? 0 : req.getDisplayOrder());
        SpecAttribute saved = specAttrRepo.save(attr);
        return toAttrResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public SpecAttributeResponse updateAttribute(Integer id, SpecAttributeRequest req) {
        SpecAttribute attr = specAttrRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông số!"));
        String name = req.getName() == null ? null : req.getName().trim();
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("Tên thông số không được để trống");
        }
        // Nếu có categoryId mới trong request, resolve về parent
        Integer effectiveCategoryId = (req.getCategoryId() != null) 
                ? resolveEffectiveCategoryId(req.getCategoryId()) 
                : (attr.getCategory() != null ? attr.getCategory().getCategoryId() : null);
        Category category = categoryRepo.findById(effectiveCategoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục!"));

        boolean needCheckDup =
                !attr.getName().equalsIgnoreCase(name)
                || attr.getCategory() == null
                || !attr.getCategory().getCategoryId().equals(category.getCategoryId());
        if (needCheckDup
                && specAttrRepo.existsByCategoryCategoryIdAndName(category.getCategoryId(), name)) {
            throw new RuntimeException("Tên thông số đã tồn tại trong danh mục này!");
        }
        attr.setCategory(category);
        attr.setName(name);
        attr.setUnit(emptyToNull(req.getUnit()));
        if (req.getDataType() != null && !req.getDataType().isBlank()) {
            attr.setDataType(req.getDataType());
        }
        if (req.getDisplayOrder() != null) {
            attr.setDisplayOrder(req.getDisplayOrder());
        }
        SpecAttribute saved = specAttrRepo.save(attr);
        return toAttrResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAttribute(Integer id) {
        SpecAttribute attr = specAttrRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông số!"));
        long usageCount = specValueRepo.countBySpecAttributeSpecAttributeId(attr.getSpecAttributeId());
        if (usageCount > 0) {
            throw new RuntimeException(
                    "Không thể xóa: thông số đang được dùng bởi " + usageCount
                    + " sản phẩm. Hãy gỡ khỏi sản phẩm trước.");
        }
        specAttrRepo.delete(attr);
    }

    // ===== ProductSpecValue =====

    @Transactional(readOnly = true)
    public List<ProductSpecValueResponse> getSpecValuesByProduct(Integer productId) {
        if (productId == null) return List.of();
        return specValueRepo.findByProductIdWithSpec(productId).stream()
                .map(this::toValueResponse).toList();
    }

    /**
     * Lưu toàn bộ danh sách TSKT của 1 sản phẩm:
     * - Xóa các dòng không còn trong request (id cũ không được gửi lại).
     * - Bỏ qua (không tạo) các dòng chưa có id và giá trị rỗng — tránh rác DB.
     * - Tạo mới các dòng chưa có id nhưng có giá trị.
     * - Cập nhật các dòng có id.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<ProductSpecValueResponse> saveSpecValuesForProduct(ProductSpecsSaveRequest req) {
        if (req == null || req.getProductId() == null) {
            throw new RuntimeException("Dữ liệu không hợp lệ");
        }
        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm!"));

        List<ProductSpecValue> existing = specValueRepo.findByProductProductId(product.getProductId());

        Map<Integer, ProductSpecValue> existingById = new HashMap<>();
        for (ProductSpecValue psv : existing) {
            existingById.put(psv.getId(), psv);
        }

        List<ProductSpecsSaveRequest.SpecValueItem> items =
                req.getSpecs() == null ? List.of() : req.getSpecs();

        Set<Integer> specAttrIds = new HashSet<>();
        for (ProductSpecsSaveRequest.SpecValueItem item : items) {
            if (item == null) continue;
            if (item.getSpecAttributeId() == null) {
                throw new RuntimeException("Mỗi dòng TSKT phải có specAttributeId");
            }
            specAttrIds.add(item.getSpecAttributeId());
        }
        Map<Integer, SpecAttribute> attrMap = new HashMap<>();
        if (!specAttrIds.isEmpty()) {
            specAttrRepo.findAllById(specAttrIds).forEach(a -> attrMap.put(a.getSpecAttributeId(), a));
        }

        // Chỉ giữ lại (không xóa) những id thực sự có trong request
        Set<Integer> keepIds = new HashSet<>();
        for (ProductSpecsSaveRequest.SpecValueItem item : items) {
            if (item != null && item.getId() != null) keepIds.add(item.getId());
        }

        List<ProductSpecValue> toDelete = new ArrayList<>();
        for (ProductSpecValue psv : existing) {
            if (!keepIds.contains(psv.getId())) {
                toDelete.add(psv);
            }
        }
        if (!toDelete.isEmpty()) {
            specValueRepo.deleteAll(toDelete);
        }

        for (ProductSpecsSaveRequest.SpecValueItem item : items) {
            if (item == null) continue;
            SpecAttribute attr = attrMap.get(item.getSpecAttributeId());
            if (attr == null) {
                throw new RuntimeException(
                        "Không tìm thấy thuộc tính thông số ID = " + item.getSpecAttributeId());
            }
            String value = emptyToNull(item.getValueText());

            // FIX: bỏ qua tạo mới dòng rỗng chưa từng tồn tại — tránh rác Product_Spec_Values
            if (value == null && item.getId() == null) {
                continue;
            }

            if (item.getId() != null && existingById.containsKey(item.getId())) {
                ProductSpecValue target = existingById.get(item.getId());
                target.setSpecAttribute(attr);
                target.setValueText(value);
                specValueRepo.save(target);
            } else {
                ProductSpecValue created = new ProductSpecValue();
                created.setProduct(product);
                created.setSpecAttribute(attr);
                created.setValueText(value);
                specValueRepo.save(created);
            }
        }

        return specValueRepo.findByProductIdWithSpec(product.getProductId()).stream()
                .map(this::toValueResponse).toList();
    }

    // ===== Mappers =====

    private SpecAttributeResponse toAttrResponse(SpecAttribute attr) {
        Category cat = attr.getCategory();
        return SpecAttributeResponse.builder()
                .specAttributeId(attr.getSpecAttributeId())
                .categoryId(cat != null ? cat.getCategoryId() : null)
                .categoryName(cat != null ? cat.getCategoryName() : null)
                .name(attr.getName())
                .unit(attr.getUnit())
                .dataType(attr.getDataType())
                .displayOrder(attr.getDisplayOrder())
                .build();
    }

    private ProductSpecValueResponse toValueResponse(ProductSpecValue psv) {
        SpecAttribute attr = psv.getSpecAttribute();
        return ProductSpecValueResponse.builder()
                .id(psv.getId())
                .productId(psv.getProduct() != null ? psv.getProduct().getProductId() : null)
                .specAttributeId(attr != null ? attr.getSpecAttributeId() : null)
                .specAttributeName(attr != null ? attr.getName() : null)
                .unit(attr != null ? attr.getUnit() : null)
                .dataType(attr != null ? attr.getDataType() : null)
                .displayOrder(attr != null ? attr.getDisplayOrder() : null)
                .valueText(psv.getValueText())
                .build();
    }

    private String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}