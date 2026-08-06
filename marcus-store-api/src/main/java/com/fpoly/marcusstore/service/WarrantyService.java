package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.CreateWarrantyRequest;
import com.fpoly.marcusstore.dto.request.UpdateWarrantyStatusRequest;
import com.fpoly.marcusstore.dto.response.WarrantyResponse;
import com.fpoly.marcusstore.entity.shopping.WarrantyReturn.WarrantyReason;
import com.fpoly.marcusstore.entity.shopping.WarrantyReturn.WarrantyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WarrantyService {

    WarrantyResponse createWarranty(Integer userId, CreateWarrantyRequest request);

    List<WarrantyResponse> getWarrantiesByUser(Integer userId);

    Page<WarrantyResponse> getWarrantiesPage(WarrantyStatus status, WarrantyReason reason, String keyword, Pageable pageable);

    long countByStatus(WarrantyStatus status);

    long countAll();

    WarrantyResponse getWarrantyById(Integer warrantyId);

    WarrantyResponse updateWarrantyStatus(Integer warrantyId, Integer adminId, UpdateWarrantyStatusRequest request);

    WarrantyResponse getWarrantyByOrderItemId(Integer userId, Integer orderItemId);

    boolean canRequestWarranty(Integer userId, Integer orderItemId);
}
