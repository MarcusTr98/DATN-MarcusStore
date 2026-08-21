package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.OrderAssignmentResponse;
import com.fpoly.marcusstore.dto.response.OrderAssignmentDashboardResponse;
import com.fpoly.marcusstore.entity.shopping.Order;

public interface OrderAssignmentService {
    void assignAutomatically(Order order);

    OrderAssignmentResponse assignManually(String orderCode, Integer staffId, String reason);

    OrderAssignmentResponse getCurrentAssignment(Integer orderId);

    void assignDueOrders();

    OrderAssignmentDashboardResponse getDashboard();

    void assertCurrentStaffCanAccess(Integer orderId);
}
