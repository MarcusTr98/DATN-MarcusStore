package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.ProductSpecsSaveRequest;
import com.fpoly.marcusstore.dto.request.SpecAttributeRequest;
import com.fpoly.marcusstore.dto.response.ProductSpecValueResponse;
import com.fpoly.marcusstore.dto.response.SpecCategoryScopeResponse;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    @Transactional(readOnly = true)
    public List<SpecAttributeResponse> getAttributesByCategory(Integer categoryId) {
        if (categoryId == null) {
            return List.of();
        }

        // Marcus sửa: một sản phẩm được dùng bộ thông số chung của danh mục cha
        // và bộ thông số riêng của chính danh mục, không còn chọn một trong hai.
        List<Category> hierarchy = resolveCategoryHierarchy(categoryId);
        List<Integer> categoryIds = hierarchy.stream().map(Category::getCategoryId).toList();
        Map<Integer, Integer> hierarchyOrder = new HashMap<>();
        for (int index = 0; index < categoryIds.size(); index++) {
            hierarchyOrder.put(categoryIds.get(index), index);
        }

        return specAttrRepo.findByCategoryIdsWithCategory(categoryIds).stream()
                .sorted(Comparator
                        .comparingInt((SpecAttribute attr) -> hierarchyOrder.getOrDefault(
                                attr.getCategory().getCategoryId(), Integer.MAX_VALUE))
                        .thenComparing(attr -> attr.getDisplayOrder() == null ? 0 : attr.getDisplayOrder())
                        .thenComparing(SpecAttribute::getSpecAttributeId))
                .map(this::toAttrResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpecCategoryScopeResponse> getCategoryScopes(Integer categoryId) {
        List<Category> hierarchy = resolveCategoryHierarchy(categoryId);
        return hierarchy.stream()
                .map(category -> SpecCategoryScopeResponse.builder()
                        .categoryId(category.getCategoryId())
                        .categoryName(category.getCategoryName())
                        .productCategory(category.getCategoryId().equals(categoryId))
                        .build())
                .toList();
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
        // Marcus sửa: tạo đúng danh mục Admin chọn. Thuộc tính chung chỉ được tạo
        // khi Admin chủ động chọn danh mục cha, tránh đưa thông số Apple sang Samsung.
        Category category = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục!"));
        if (hasAttributeNameInRelatedScopes(category.getCategoryId(), name, null)) {
            throw new RuntimeException(
                    "Tên thông số đã tồn tại trong danh mục này hoặc phạm vi cha/con được kế thừa!");
        }
        SpecAttribute attr = new SpecAttribute();
        String nextDataType = normalizeDataType(req.getDataType());
        attr.setCategory(category);
        attr.setName(name);
        attr.setUnit(emptyToNull(req.getUnit()));
        attr.setDataType(nextDataType);
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
        if (attr.getCategory() == null) {
            throw new RuntimeException("Thông số chưa được gắn với danh mục hợp lệ!");
        }
        Integer currentCategoryId = attr.getCategory().getCategoryId();
        if (!currentCategoryId.equals(req.getCategoryId())) {
            throw new RuntimeException(
                    "Không thể chuyển thông số sang danh mục khác. Hãy tạo thông số mới ở danh mục cần dùng.");
        }
        Category category = attr.getCategory();

        boolean needCheckDup = !attr.getName().equalsIgnoreCase(name);
        if (needCheckDup && hasAttributeNameInRelatedScopes(
                category.getCategoryId(), name, attr.getSpecAttributeId())) {
            throw new RuntimeException(
                    "Tên thông số đã tồn tại trong danh mục này hoặc phạm vi cha/con được kế thừa!");
        }
        String nextDataType = normalizeDataType(req.getDataType());
        // Marcus thêm: đổi kiểu khi đã có dữ liệu sẽ làm các giá trị cũ sai định dạng.
        if (!nextDataType.equalsIgnoreCase(attr.getDataType())
                && specValueRepo.countBySpecAttributeSpecAttributeId(attr.getSpecAttributeId()) > 0) {
            throw new RuntimeException(
                    "Không thể đổi kiểu dữ liệu khi thông số đang có giá trị. Hãy xóa các giá trị trước.");
        }
        attr.setCategory(category);
        attr.setName(name);
        attr.setUnit(emptyToNull(req.getUnit()));
        attr.setDataType(nextDataType);
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
        if (productId == null)
            return List.of();
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

        if (product.getCategory() == null) {
            throw new RuntimeException("Sản phẩm chưa có danh mục nên không thể lưu thông số!");
        }
        List<ProductSpecsSaveRequest.SpecValueItem> items = req.getSpecs();
        Set<Integer> allowedCategoryIds = new HashSet<>(resolveCategoryHierarchy(
                product.getCategory().getCategoryId()).stream().map(Category::getCategoryId).toList());

        Set<Integer> specAttrIds = new HashSet<>();
        for (ProductSpecsSaveRequest.SpecValueItem item : items) {
            if (item == null)
                continue;
            if (item.getSpecAttributeId() == null) {
                throw new RuntimeException("Mỗi dòng TSKT phải có specAttributeId");
            }
            if (!specAttrIds.add(item.getSpecAttributeId())) {
                throw new RuntimeException("Một thông số không được xuất hiện hai lần trong cùng yêu cầu.");
            }
        }
        Map<Integer, SpecAttribute> attrMap = new HashMap<>();
        if (!specAttrIds.isEmpty()) {
            specAttrRepo.findAllById(specAttrIds).forEach(a -> attrMap.put(a.getSpecAttributeId(), a));
        }

        Map<Integer, ProductSpecValue> existingByAttributeId = new HashMap<>();
        for (ProductSpecValue value : existing) {
            if (value.getSpecAttribute() != null) {
                existingByAttributeId.put(value.getSpecAttribute().getSpecAttributeId(), value);
            }
        }

        List<ValidatedSpecItem> validatedItems = new ArrayList<>();
        Set<Integer> requestedExistingIds = new HashSet<>();
        for (ProductSpecsSaveRequest.SpecValueItem item : items) {
            if (item == null)
                continue;
            SpecAttribute attr = attrMap.get(item.getSpecAttributeId());
            if (attr == null) {
                throw new RuntimeException(
                        "Không tìm thấy thuộc tính thông số ID = " + item.getSpecAttributeId());
            }
            if (attr.getCategory() == null
                    || !allowedCategoryIds.contains(attr.getCategory().getCategoryId())) {
                throw new RuntimeException(
                        "Thông số \"" + attr.getName() + "\" không thuộc danh mục của sản phẩm.");
            }

            ProductSpecValue currentValue = null;
            if (item.getId() != null) {
                currentValue = existingById.get(item.getId());
                if (currentValue == null) {
                    throw new RuntimeException("Giá trị thông số không thuộc sản phẩm đang cập nhật.");
                }
                Integer currentAttributeId = currentValue.getSpecAttribute() == null
                        ? null
                        : currentValue.getSpecAttribute().getSpecAttributeId();
                if (!item.getSpecAttributeId().equals(currentAttributeId)) {
                    throw new RuntimeException("Không được thay đổi thuộc tính của một giá trị đã tồn tại.");
                }
                if (!requestedExistingIds.add(item.getId())) {
                    throw new RuntimeException("ID giá trị thông số bị lặp trong cùng yêu cầu.");
                }
            } else if (existingByAttributeId.containsKey(item.getSpecAttributeId())) {
                throw new RuntimeException(
                        "Thông số đã có giá trị nhưng request bị thiếu ID cập nhật. Vui lòng tải lại trang.");
            }

            String normalizedValue = normalizeValue(attr, item.getValueText());
            validatedItems.add(new ValidatedSpecItem(attr, currentValue, normalizedValue));
        }

        // Marcus sửa: danh sách PUT là ảnh chụp đầy đủ. Dòng bị bỏ khỏi request hoặc
        // được làm rỗng sẽ bị xóa thật, không ghi NULL vào cột value_text NOT NULL.
        Set<Integer> keepIds = new HashSet<>();
        for (ValidatedSpecItem item : validatedItems) {
            if (item.currentValue() != null && item.value() != null) {
                keepIds.add(item.currentValue().getId());
            }
        }
        List<ProductSpecValue> toDelete = existing.stream()
                .filter(value -> !keepIds.contains(value.getId()))
                .toList();
        if (!toDelete.isEmpty()) {
            specValueRepo.deleteAll(toDelete);
            specValueRepo.flush();
        }

        List<ProductSpecValue> toSave = new ArrayList<>();
        for (ValidatedSpecItem item : validatedItems) {
            if (item.value() == null) {
                continue;
            }
            if (item.currentValue() != null) {
                item.currentValue().setValueText(item.value());
                toSave.add(item.currentValue());
            } else {
                ProductSpecValue created = new ProductSpecValue();
                created.setProduct(product);
                created.setSpecAttribute(item.attribute());
                created.setValueText(item.value());
                toSave.add(created);
            }
        }
        if (!toSave.isEmpty()) {
            specValueRepo.saveAll(toSave);
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
        if (s == null)
            return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // Marcus thêm: trả danh mục từ gốc đến danh mục hiện tại và chặn dữ liệu cây bị
    // vòng lặp.
    private List<Category> resolveCategoryHierarchy(Integer categoryId) {
        if (categoryId == null) {
            throw new RuntimeException("Danh mục không hợp lệ!");
        }
        List<Category> hierarchy = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Integer currentId = categoryId;
        while (currentId != null) {
            if (!visited.add(currentId)) {
                throw new RuntimeException("Cấu trúc danh mục đang bị vòng lặp!");
            }
            Category current = categoryRepo.findById(currentId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục!"));
            hierarchy.add(current);
            currentId = current.getParent() == null ? null : current.getParent().getCategoryId();
        }
        Collections.reverse(hierarchy);
        return hierarchy;
    }

    private String normalizeDataType(String dataType) {
        String normalized = dataType == null ? "text" : dataType.trim().toLowerCase();
        if (!Set.of("text", "number", "boolean").contains(normalized)) {
            throw new RuntimeException("Kiểu dữ liệu phải là text, number hoặc boolean.");
        }
        return normalized;
    }

    private boolean hasAttributeNameInRelatedScopes(
            Integer categoryId,
            String name,
            Integer excludedAttributeId) {
        Set<Integer> relatedCategoryIds = new HashSet<>();
        resolveCategoryHierarchy(categoryId).forEach(category -> relatedCategoryIds.add(category.getCategoryId()));
        collectDescendantCategoryIds(categoryId, relatedCategoryIds);

        for (Integer relatedCategoryId : relatedCategoryIds) {
            boolean exists = excludedAttributeId == null
                    ? specAttrRepo.existsByCategoryCategoryIdAndName(relatedCategoryId, name)
                    : specAttrRepo.existsByCategoryCategoryIdAndNameAndSpecAttributeIdNot(
                            relatedCategoryId, name, excludedAttributeId);
            if (exists) {
                return true;
            }
        }
        return false;
    }

    private void collectDescendantCategoryIds(Integer categoryId, Set<Integer> visited) {
        for (Category child : categoryRepo.findByParent_CategoryId(categoryId)) {
            if (visited.add(child.getCategoryId())) {
                collectDescendantCategoryIds(child.getCategoryId(), visited);
            }
        }
    }

    private String normalizeValue(SpecAttribute attribute, String rawValue) {
        String value = emptyToNull(rawValue);
        if (value == null) {
            return null;
        }
        String dataType = normalizeDataType(attribute.getDataType());
        if ("number".equals(dataType)) {
            String number = value.replace(',', '.');
            if (!number.matches("[-+]?\\d+(?:\\.\\d+)?")) {
                throw new RuntimeException(
                        "Thông số \"" + attribute.getName() + "\" phải là một số, không nhập kèm đơn vị.");
            }
            try {
                return new BigDecimal(number).stripTrailingZeros().toPlainString();
            } catch (NumberFormatException ex) {
                throw new RuntimeException("Giá trị số của \"" + attribute.getName() + "\" không hợp lệ.");
            }
        }
        if ("boolean".equals(dataType)) {
            String booleanValue = value.toLowerCase();
            if (Set.of("true", "1", "yes", "có", "co").contains(booleanValue)) {
                return "Có";
            }
            if (Set.of("false", "0", "no", "không", "khong").contains(booleanValue)) {
                return "Không";
            }
            throw new RuntimeException(
                    "Thông số \"" + attribute.getName() + "\" chỉ nhận giá trị Có hoặc Không.");
        }
        return value;
    }

    private record ValidatedSpecItem(
            SpecAttribute attribute,
            ProductSpecValue currentValue,
            String value) {
    }
}
