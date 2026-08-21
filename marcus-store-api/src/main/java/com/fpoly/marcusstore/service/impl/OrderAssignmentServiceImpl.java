package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.response.OrderAssignmentResponse;
import com.fpoly.marcusstore.dto.response.OrderAssignmentDashboardResponse;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderAssignment;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.shopping.OrderAssignmentRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import com.fpoly.marcusstore.service.OrderAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderAssignmentServiceImpl implements OrderAssignmentService {
    private static final Set<String> ACTIVE_ORDER_STATUSES = Set.of(
            "PENDING", "CONFIRMED", "PROCESSING", "READY_FOR_PICKUP", "PACKED", "SHIPPING", "DELIVERED", "FAILED");

    private final OrderAssignmentRepository assignmentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void assignAutomatically(Order order) {
        if (order == null || order.getOrderId() == null) {
            return;
        }
        Order lockedOrder = orderRepository.findByIdForUpdate(order.getOrderId()).orElse(null);
        if (lockedOrder == null || !isEligibleForAutoAssignment(lockedOrder)
                || assignmentRepository.findCurrentByOrderId(lockedOrder.getOrderId()).isPresent())
            return;

        User selectedStaff = selectLeastLoadedStaff(true);

        if (selectedStaff != null) {
            createAssignment(lockedOrder, selectedStaff, null, "AUTO", "Hệ thống tự phân theo số đơn đang xử lý");
        }
    }

    @Override
    @Transactional
    public OrderAssignmentResponse assignManually(String orderCode, Integer staffId, String reason) {
        Order order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        User staff = userRepository.findActiveStaffWithOrderUpdatePermissionById(staffId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Nhân viên không hoạt động hoặc không có quyền xử lý đơn"));

        String status = normalize(order.getOrderStatus());
        if (!ACTIVE_ORDER_STATUSES.contains(status)) {
            throw new IllegalStateException("Không thể phân công đơn ở trạng thái " + status);
        }

        assignmentRepository.findCurrentByOrderIdForUpdate(order.getOrderId()).ifPresent(current -> {
            current.setIsCurrent(false);
            assignmentRepository.save(current);
            assignmentRepository.flush();
        });

        Integer currentUserId = SecurityUtils.getCurrentUserId();
        User assignedBy = currentUserId == null ? null : userRepository.getReferenceById(currentUserId);
        OrderAssignment assignment = createAssignment(order, staff, assignedBy, "MANUAL", reason);
        return toResponse(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderAssignmentResponse getCurrentAssignment(Integer orderId) {
        return assignmentRepository.findCurrentByOrderId(orderId).map(this::toResponse).orElse(null);
    }

    @Override
    @Transactional
    public void assignDueOrders() {
        orderRepository.findOrdersDueForAutoAssignment(java.time.LocalDateTime.now(), PageRequest.of(0, 100))
                .forEach(this::assignAutomatically);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderAssignmentDashboardResponse getDashboard() {
        List<User> staffs = userRepository.findActiveStaffWithOrderUpdatePermission();
        Map<Integer, Long> activeLoadByStaffId = new HashMap<>();
        Map<Integer, Long> completedByStaffId = new HashMap<>();
        for (User staff : staffs) {
            activeLoadByStaffId.put(staff.getUserId(),
                    assignmentRepository.countCurrentActiveOrders(staff.getUserId(), ACTIVE_ORDER_STATUSES));
            completedByStaffId.put(staff.getUserId(),
                    assignmentRepository.countCurrentCompletedOrders(staff.getUserId()));
        }
        long maxLoad = activeLoadByStaffId.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<OrderAssignmentDashboardResponse.StaffLoad> staffLoads = staffs.stream().map(staff -> {
            long count = activeLoadByStaffId.get(staff.getUserId());
            long completed = completedByStaffId.get(staff.getUserId());
            double completionRate = count + completed == 0 ? 0
                    : Math.round((completed * 1000.0 / (count + completed))) / 10.0;
            return OrderAssignmentDashboardResponse.StaffLoad.builder()
                    .staffId(staff.getUserId()).staffName(displayName(staff)).activeOrderCount(count)
                    .workloadRate(maxLoad == 0 ? 0 : Math.round((count * 1000.0 / maxLoad)) / 10.0)
                    .completedOrderCount(completed).completionRate(completionRate).build();
        }).toList();
        Map<Integer, Long> projectedLoadByStaffId = new HashMap<>(activeLoadByStaffId);
        List<OrderAssignmentDashboardResponse.PendingOrder> pendingOrders = orderRepository
                .findPendingUnassignedOrders(PageRequest.of(0, 100)).stream().map(order -> {
                    User planned = selectLeastLoadedStaff(staffs, projectedLoadByStaffId);
                    if (planned != null)
                        projectedLoadByStaffId.merge(planned.getUserId(), 1L, Long::sum);
                    return OrderAssignmentDashboardResponse.PendingOrder.builder()
                            .orderCode(order.getOrderCode()).recipientName(order.getRecipientName())
                            .finalAmount(order.getFinalAmount()).autoAssignAt(order.getAutoAssignAt())
                            .plannedStaffId(planned == null ? null : planned.getUserId())
                            .plannedStaffName(planned == null ? null : displayName(planned)).build();
                }).toList();
        return OrderAssignmentDashboardResponse.builder().staffLoads(staffLoads).pendingOrders(pendingOrders).build();
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCurrentStaffCanAccess(Integer orderId) {
        if (!SecurityUtils.hasAnyRole("STAFF")) {
            return;
        }
        Integer staffId = SecurityUtils.getCurrentUserId();
        if (!assignmentRepository.existsByOrderOrderIdAndStaffUserIdAndIsCurrentTrue(orderId, staffId)) {
            throw new AccessDeniedException("Đơn hàng chưa được phân công cho bạn");
        }
    }

    private boolean isEligibleForAutoAssignment(Order order) {
        if (!"PENDING".equals(normalize(order.getOrderStatus()))) {
            return false;
        }
        return !"VNPAY".equals(normalize(order.getPaymentMethod()))
                || "PAID".equals(normalize(order.getPaymentStatus()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private User selectLeastLoadedStaff(boolean lockForAssignment) {
        List<User> staffs = lockForAssignment
                ? userRepository.findActiveStaffWithOrderUpdatePermissionForAssignment()
                : userRepository.findActiveStaffWithOrderUpdatePermission();
        return staffs.stream().min(Comparator
                .comparingLong((User staff) -> assignmentRepository.countCurrentActiveOrders(staff.getUserId(),
                        ACTIVE_ORDER_STATUSES))
                .thenComparing(User::getUserId)).orElse(null);
    }

    private User selectLeastLoadedStaff(List<User> staffs, Map<Integer, Long> loadByStaffId) {
        return staffs.stream().min(Comparator
                .comparingLong((User staff) -> loadByStaffId.getOrDefault(staff.getUserId(), 0L))
                .thenComparing(User::getUserId)).orElse(null);
    }

    private OrderAssignment createAssignment(Order order, User staff, User assignedBy, String type, String reason) {
        OrderAssignment assignment = new OrderAssignment();
        assignment.setOrder(order);
        assignment.setStaff(staff);
        assignment.setAssignedBy(assignedBy);
        assignment.setAssignmentType(type);
        assignment.setReason(reason == null || reason.isBlank() ? null : reason.trim());
        assignment.setIsCurrent(true);
        OrderAssignment savedAssignment = assignmentRepository.save(assignment);
        assignmentRepository.flush();
        return savedAssignment;
    }

    private OrderAssignmentResponse toResponse(OrderAssignment assignment) {
        return OrderAssignmentResponse.builder()
                .staffId(assignment.getStaff().getUserId())
                .staffName(displayName(assignment.getStaff()))
                .assignmentType(assignment.getAssignmentType())
                .assignedByName(displayName(assignment.getAssignedBy()))
                .assignedAt(assignment.getAssignedAt())
                .build();
    }

    private String displayName(User user) {
        if (user == null)
            return null;
        return user.getFullName() == null || user.getFullName().isBlank() ? user.getUsername() : user.getFullName();
    }
}
