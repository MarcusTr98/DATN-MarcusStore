package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.UpdateOrderImeiRequest;
import com.fpoly.marcusstore.dto.request.UpdateOrderStatusRequest;
import com.fpoly.marcusstore.dto.response.OrderDetailResponse;
import com.fpoly.marcusstore.dto.response.OrderImeiAssignmentResponse;
import com.fpoly.marcusstore.dto.response.OrderResponse;
import com.fpoly.marcusstore.dto.response.OrderStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    // Lấy dữ liệu theo keyword, phương thức thanh toán, trạng thái đơn hàng và
    // khoảng thời gian tạo đơn
    Page<OrderResponse> getOrdersPage(String keyword, String paymentMethod, String orderStatus, LocalDate fromDate,
            LocalDate toDate, Pageable pageable);

    OrderStatsResponse getOrderStats(String keyword, String paymentMethod, String orderStatus);

    List<String> getPaymentMethods();

    List<String> getOrderStatuses();

    OrderDetailResponse getOrderDetailResponse(String orderCode);

    OrderDetailResponse updateStatusOrder(String orderCode, UpdateOrderStatusRequest request);

    // Marcus thêm: cho phép Admin chủ động thử tạo lại vận đơn GHN bị lỗi.
    OrderDetailResponse retryGhnShipment(String orderCode);

    List<OrderResponse> getUserOrder();

    OrderDetailResponse getUserOrderDetail(String orderCode);

    OrderDetailResponse cancelUserOrder(String orderCode, String reasonCode, String reason);

    OrderDetailResponse confirmUserReceivedOrder(String orderCode);

    //Đức thêm xử lý imei cho order
    List<OrderImeiAssignmentResponse> getImeiPreview(String orderCode);

    OrderDetailResponse assignOrderImeis(String orderCode, List<UpdateOrderImeiRequest> requests);

    OrderDetailResponse startProcessingWithImei(String orderCode, List<UpdateOrderImeiRequest> requests);

    OrderDetailResponse assignOrderToStaff(String orderCode, Integer staffId, String reason);
}
