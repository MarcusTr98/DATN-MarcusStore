package com.fpoly.marcusstore.service;

import java.util.List;

import com.fpoly.marcusstore.dto.request.EmployeePositionRequest;
import com.fpoly.marcusstore.dto.response.EmployeePositionResponse;

public interface EmployeePositionService {
    List<EmployeePositionResponse> getAll();

    EmployeePositionResponse getById(Integer id);

    void create(EmployeePositionRequest request);

    void update(Integer id, EmployeePositionRequest request);

    void changeStatus(Integer id, Boolean active);
}
