package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.response.OrderResponse;
import com.fpoly.marcusstore.dto.response.OrderStatsResponse;
import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();
    }

    private String normalizePaymentMethod(String paymentMethod) {
        return paymentMethod == null ||
                paymentMethod.isBlank() ||
                "ALL".equalsIgnoreCase(paymentMethod)
                ? null
                : paymentMethod.trim();
    }

    private String normalizeOrderStatus(String orderStatus) {
        return orderStatus == null ||
                orderStatus.isBlank() ||
                "ALL".equalsIgnoreCase(orderStatus)
                ? null
                : orderStatus.trim()    ;
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .finalAmount(order.getFinalAmount())
                .paymentMethod(order.getPaymentMethod())
                .itemCount(orderRepository.countItemsByOrderId(order.getOrderId()))
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .createdAt(order.getCreatedAt()).build();

    }

    @Override
    public Page<OrderResponse> getOrdersPage(String keyword, String paymentMethod, String orderStatus, Pageable pageable) {
        String normalizeKeyword = normalizeKeyword(keyword);
        String normalizePaymentMethod = normalizePaymentMethod(paymentMethod);
        String normalizeOrderStatus = normalizeOrderStatus(orderStatus);

        return orderRepository
                .searchOrders(normalizeKeyword, normalizePaymentMethod, normalizeOrderStatus, pageable)
                .map(this::toResponse);
    }
 // hàm gọi tổng đơn hàng và số lượng trạng thái đơn hàng
    @Override
    public OrderStatsResponse getOrderStats(String keyword, String paymentMethod, String orderStatus) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
        String normalizedOrderStatus = normalizeOrderStatus(orderStatus);

        return OrderStatsResponse.builder()
                .total(orderRepository.countOrders(
                        normalizedKeyword,
                        normalizedPaymentMethod,
                        normalizedOrderStatus
                ))
                .pending(orderRepository.countPendingOrders(
                        normalizedKeyword,
                        normalizedPaymentMethod,
                        normalizedOrderStatus
                ))
                .confirmed(orderRepository.countConfirmedOrders(
                        normalizedKeyword,
                        normalizedPaymentMethod,
                        normalizedOrderStatus
                ))
                .shipping(orderRepository.countShippingOrders(
                        normalizedKeyword,
                        normalizedPaymentMethod,
                        normalizedOrderStatus
                ))
                .completed(orderRepository.countCompletedOrders(
                        normalizedKeyword,
                        normalizedPaymentMethod,
                        normalizedOrderStatus
                ))
                .cancelled(orderRepository.countCancelledOrders(
                        normalizedKeyword,
                        normalizedPaymentMethod,
                        normalizedOrderStatus
                ))
                .build();
    }
    @Override
    public List<String> getPaymentMethods() {
        return orderRepository.findDistinctPaymentMethods();
    }
@Override
    public List<String> getOrderStatuses() {
        return orderRepository.findDistinctOrderStatuses();
    }
}
