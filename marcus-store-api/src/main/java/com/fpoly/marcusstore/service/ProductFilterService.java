package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.CategoryResponse;
import com.fpoly.marcusstore.dto.response.ClientFilterGroupResponse;
import com.fpoly.marcusstore.dto.response.ClientFilterOptionResponse;
import com.fpoly.marcusstore.repository.core.ClientProductFilterRepository;
import com.fpoly.marcusstore.repository.core.ClientProductFilterRepository.DynamicFilterProjection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductFilterService {

    @Autowired
    private ClientProductFilterRepository filterRepo;

    @Autowired
    private CategoriesService categoriesService;

    private static final List<ClientFilterOptionResponse> PRICE_RANGES = List.of(
            ClientFilterOptionResponse.builder().label("Dưới 5 triệu").extra("0-5000000").build(),
            ClientFilterOptionResponse.builder().label("5 - 10 triệu").extra("5000000-10000000").build(),
            ClientFilterOptionResponse.builder().label("10 - 20 triệu").extra("10000000-20000000").build(),
            ClientFilterOptionResponse.builder().label("Trên 20 triệu").extra("20000000--1").build());

    public List<ClientFilterGroupResponse> getFiltersForCategory(Integer parentCategoryId) {
        List<ClientFilterGroupResponse> result = new ArrayList<>();

        List<CategoryResponse> brands = categoriesService.getActiveChildren(parentCategoryId);
        if (!brands.isEmpty()) {
            List<ClientFilterOptionResponse> brandOptions = brands.stream()
                    .map(b -> ClientFilterOptionResponse.builder()
                            .valueId(b.getCategoryId())
                            .label(b.getCategoryName())
                            .categoryImg(b.getCategoryImg())
                            .build())
                    .toList();
            result.add(ClientFilterGroupResponse.builder()
                    .attributeId(null)
                    .attributeName("Hãng")
                    .options(brandOptions)
                    .build());
        }

        result.add(ClientFilterGroupResponse.builder()
                .attributeId(null)
                .attributeName("Giá")
                .options(PRICE_RANGES)
                .build());

        List<DynamicFilterProjection> rawFilters = parentCategoryId != null
                ? filterRepo.findAvailableFiltersByParentCategory(parentCategoryId)
                : List.of();

        Map<Integer, ClientFilterGroupResponse> groupMap = new LinkedHashMap<>();
        Map<Integer, List<ClientFilterOptionResponse>> optionsMap = new LinkedHashMap<>();

        for (DynamicFilterProjection row : rawFilters) {
            groupMap.putIfAbsent(row.getAttributeId(), ClientFilterGroupResponse.builder()
                    .attributeId(row.getAttributeId())
                    .attributeName(row.getAttributeName())
                    .build());

            optionsMap.computeIfAbsent(row.getAttributeId(), k -> new ArrayList<>())
                    .add(ClientFilterOptionResponse.builder()
                            .valueId(row.getValueId())
                            .label(row.getValueString())
                            .build());
        }

        for (Map.Entry<Integer, ClientFilterGroupResponse> entry : groupMap.entrySet()) {
            ClientFilterGroupResponse group = entry.getValue();
            group.setOptions(optionsMap.get(entry.getKey()));
            result.add(group);
        }

        return result;
    }
}