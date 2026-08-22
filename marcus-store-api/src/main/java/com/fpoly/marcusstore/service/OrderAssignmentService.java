package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.OrderAssignmentResponse;
import com.fpoly.marcusstore.dto.response.OrderAssignmentDashboardResponse;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.dto.response.StaffAssignmentStatusResponse;

public interface OrderAssignmentService {
    void assignAutomatically(Order order);

    OrderAssignmentResponse assignManually(String orderCode, Integer staffId, String reason);

    OrderAssignmentResponse getCurrentAssignment(Integer orderId);

    void assignDueOrders();

    OrderAssignmentDashboardResponse getDashboard(int pendingPage, int pendingSize);

    void assertCurrentStaffCanAccess(Integer orderId);

    StaffAssignmentStatusResponse getCurrentStaffStatus();

    StaffAssignmentStatusResponse setCurrentStaffAvailability(boolean acceptingOrders);

    String claimNextOrder();

    StaffAssignmentStatusResponse updateStaffSettings(Integer staffId, boolean acceptingOrders, int maxActiveOrders);
}
