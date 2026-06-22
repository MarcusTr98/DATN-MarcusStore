package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.UpdateOrderStatusRequest;
import com.fpoly.marcusstore.dto.response.OrderDetailResponse;
import com.fpoly.marcusstore.dto.response.OrderResponse;
import com.fpoly.marcusstore.dto.response.OrderStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    // Lấy dữ liệu theo keyword, phương thức thanh toán và trạng thái đơn hàng
    Page<OrderResponse> getOrdersPage(String keyword, String paymentMethod, String orderStatus, Pageable pageable);
    OrderStatsResponse getOrderStats(String keyword, String paymentMethod, String orderStatus);
    List<String> getPaymentMethods();

    List<String> getOrderStatuses();
    OrderDetailResponse getOrderDetailResponse(String orderCode);
    OrderDetailResponse updateStatusOrder(String orderCode, UpdateOrderStatusRequest request);
     List<OrderResponse> getUserOrder();
     OrderDetailResponse getUserOrderDetail(String orderCode);

}
