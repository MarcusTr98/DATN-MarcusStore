package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.CreateWarrantyRequest;
import com.fpoly.marcusstore.dto.request.UpdateWarrantyStatusRequest;
import com.fpoly.marcusstore.dto.response.WarrantyResponse;

import java.util.List;

public interface WarrantyService {
    
    WarrantyResponse createWarranty(Integer userId, CreateWarrantyRequest request);
    
    List<WarrantyResponse> getWarrantiesByUser(Integer userId);
    
    List<WarrantyResponse> getAllWarranties();
    
    List<WarrantyResponse> getWarrantiesByStatus(String status);
    
    WarrantyResponse getWarrantyById(Integer warrantyId);
    
    WarrantyResponse updateWarrantyStatus(Integer warrantyId, Integer adminId, UpdateWarrantyStatusRequest request);
    
    WarrantyResponse getWarrantyByOrderItemId(Integer userId, Integer orderItemId);
    
    boolean canRequestWarranty(Integer userId, Integer orderItemId);
}
