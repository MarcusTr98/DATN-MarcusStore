package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.OrderResponse;
import com.fpoly.marcusstore.dto.response.OrderStatsResponse;
import com.fpoly.marcusstore.dto.response.VoucherStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
// get dữ liệu theo keyword, phương thức thanh toán và trạng thái thanh toán
    Page<OrderResponse> getOrdersPage(String keyword, String paymentMethod, String orderStatus, Pageable pageable);
    OrderStatsResponse getOrderStats(String keyword, String paymentMethod, String orderStatus);
    List<String> getPaymentMethods();

    List<String> getOrderStatuses();
}
