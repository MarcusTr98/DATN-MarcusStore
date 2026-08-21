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
import com.fpoly.marcusstore.service.UserNotificationService;
import com.fpoly.marcusstore.dto.response.StaffAssignmentStatusResponse;
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
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderAssignmentServiceImpl implements OrderAssignmentService {
    private static final int SELF_CLAIM_COOLDOWN_SECONDS = 15;
    private static final Set<String> ACTIVE_ORDER_STATUSES = Set.of(
            "PENDING", "CONFIRMED", "PROCESSING", "READY_FOR_PICKUP", "PACKED", "SHIPPING", "DELIVERED", "FAILED");

    private final OrderAssignmentRepository assignmentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final UserNotificationService userNotificationService;

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
        LocalDateTime kpiSince = LocalDateTime.now().minusDays(30);
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
            long selfAssigned = assignmentRepository.countAssignmentsByTypeSince(staff.getUserId(), "SELF", kpiSince);
            long autoAssigned = assignmentRepository.countAssignmentsByTypeSince(staff.getUserId(), "AUTO", kpiSince);
            long manualAssigned = assignmentRepository.countAssignmentsByTypeSince(staff.getUserId(), "MANUAL",
                    kpiSince);
            long totalAssigned = selfAssigned + autoAssigned + manualAssigned;
            long completedInPeriod = assignmentRepository.countCompletedAssignmentsSince(staff.getUserId(), kpiSince);
            return OrderAssignmentDashboardResponse.StaffLoad.builder()
                    .staffId(staff.getUserId()).staffName(displayName(staff)).activeOrderCount(count)
                    .workloadRate(maxLoad == 0 ? 0 : Math.round((count * 1000.0 / maxLoad)) / 10.0)
                    .completedOrderCount(completed).completionRate(completionRate)
                    .workloadScore(workloadScore(staff))
                    .acceptingOrders(Boolean.TRUE.equals(staff.getAcceptingOrders()))
                    .maxActiveOrders(maxActiveOrders(staff))
                    .eligibleForAssignment(Boolean.TRUE.equals(staff.getAcceptingOrders())
                            && count < maxActiveOrders(staff))
                    .workloadBreakdown(workloadBreakdown(staff))
                    .selfAssignedCount(selfAssigned)
                    .autoAssignedCount(autoAssigned)
                    .manualAssignedCount(manualAssigned)
                    .totalAssignedCount(totalAssigned)
                    .selfAssignmentRate(totalAssigned == 0 ? 0
                            : Math.round(selfAssigned * 1000.0 / totalAssigned) / 10.0)
                    .completedInPeriodCount(completedInPeriod)
                    .periodCompletionRate(totalAssigned == 0 ? 0
                            : Math.round(Math.min(completedInPeriod, totalAssigned) * 1000.0 / totalAssigned) / 10.0)
                    .build();
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

    @Override
    @Transactional(readOnly = true)
    public StaffAssignmentStatusResponse getCurrentStaffStatus() {
        return buildStaffStatus(currentStaff());
    }

    @Override
    @Transactional
    public StaffAssignmentStatusResponse setCurrentStaffAvailability(boolean acceptingOrders) {
        User staff = currentStaff();
        staff.setAcceptingOrders(acceptingOrders);
        return buildStaffStatus(userRepository.save(staff));
    }

    @Override
    @Transactional
    public String claimNextOrder() {
        User staff = userRepository.findEligibleStaffByIdForAssignment(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new IllegalStateException("Bạn không đủ quyền nhận đơn"));
        if (!Boolean.TRUE.equals(staff.getAcceptingOrders())) {
            staff.setAcceptingOrders(true);
        }
        StaffAssignmentStatusResponse status = buildStaffStatus(staff);
        if (!status.isCanClaim()) {
            throw new IllegalStateException(status.getUnavailableReason());
        }
        Order order = orderRepository.findNextClaimableOrderForUpdate(PageRequest.of(0, 1)).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Hiện không có đơn phù hợp để nhận"));
        createAssignment(order, staff, staff, "SELF", "Nhân viên chủ động nhận đơn tiếp theo");
        return order.getOrderCode();
    }

    @Override
    @Transactional
    public StaffAssignmentStatusResponse updateStaffSettings(Integer staffId, boolean acceptingOrders,
            int maxActiveOrders) {
        User staff = userRepository.findEligibleStaffByIdForAssignment(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Nhân viên không đủ điều kiện xử lý đơn"));
        staff.setAcceptingOrders(acceptingOrders);
        staff.setMaxActiveOrders(Math.max(1, Math.min(maxActiveOrders, 50)));
        return buildStaffStatus(userRepository.save(staff));
    }

    private User currentStaff() {
        return userRepository.findActiveStaffWithOrderUpdatePermissionById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new IllegalStateException("Bạn không phải nhân viên xử lý đơn đang hoạt động"));
    }

    private StaffAssignmentStatusResponse buildStaffStatus(User staff) {
        long activeCount = activeCount(staff);
        LocalDateTime kpiSince = LocalDateTime.now().minusDays(30);
        long selfAssigned = assignmentRepository.countAssignmentsByTypeSince(staff.getUserId(), "SELF", kpiSince);
        long autoAssigned = assignmentRepository.countAssignmentsByTypeSince(staff.getUserId(), "AUTO", kpiSince);
        long manualAssigned = assignmentRepository.countAssignmentsByTypeSince(staff.getUserId(), "MANUAL", kpiSince);
        long totalAssigned = selfAssigned + autoAssigned + manualAssigned;
        long completedInPeriod = assignmentRepository.countCompletedAssignmentsSince(staff.getUserId(), kpiSince);
        boolean accepting = Boolean.TRUE.equals(staff.getAcceptingOrders());
        boolean belowLimit = activeCount < maxActiveOrders(staff);
        boolean cooldownReady = staff.getLastAssignedAt() == null
                || !staff.getLastAssignedAt().plusSeconds(SELF_CLAIM_COOLDOWN_SECONDS).isAfter(LocalDateTime.now());
        long cooldownRemaining = cooldownReady || staff.getLastAssignedAt() == null ? 0
                : Math.max(1, Duration.between(LocalDateTime.now(),
                        staff.getLastAssignedAt().plusSeconds(SELF_CLAIM_COOLDOWN_SECONDS)).toSeconds());
        String reason = !belowLimit ? "Bạn đã đạt giới hạn đơn đang phụ trách"
                : !cooldownReady ? "Vui lòng chờ trước khi nhận đơn tiếp theo" : null;
        return StaffAssignmentStatusResponse.builder()
                .acceptingOrders(accepting)
                .maxActiveOrders(maxActiveOrders(staff))
                .activeOrderCount(activeCount)
                .workloadScore(workloadScore(staff))
                .canClaim(reason == null)
                .lastAssignedAt(staff.getLastAssignedAt())
                .unavailableReason(reason)
                .pendingOrderCount(orderRepository.countClaimableOrders())
                .cooldownRemainingSeconds(cooldownRemaining)
                .assignedInPeriodCount(totalAssigned)
                .selfAssignedInPeriodCount(selfAssigned)
                .selfAssignmentRate(totalAssigned == 0 ? 0
                        : Math.round(selfAssigned * 1000.0 / totalAssigned) / 10.0)
                .completedInPeriodCount(completedInPeriod)
                .periodCompletionRate(totalAssigned == 0 ? 0
                        : Math.round(Math.min(completedInPeriod, totalAssigned) * 1000.0 / totalAssigned) / 10.0)
                .build();
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
        return staffs.stream()
                .filter(staff -> Boolean.TRUE.equals(staff.getAcceptingOrders()))
                .filter(staff -> activeCount(staff) < maxActiveOrders(staff))
                .min(Comparator.comparingDouble(this::workloadScore)
                        .thenComparing(staff -> staff.getLastAssignedAt(),
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(User::getUserId))
                .orElse(null);
    }

    private User selectLeastLoadedStaff(List<User> staffs, Map<Integer, Long> loadByStaffId) {
        return staffs.stream()
                .filter(staff -> Boolean.TRUE.equals(staff.getAcceptingOrders()))
                .filter(staff -> loadByStaffId.getOrDefault(staff.getUserId(), 0L) < maxActiveOrders(staff))
                .min(Comparator
                        .comparingLong((User staff) -> loadByStaffId.getOrDefault(staff.getUserId(), 0L))
                        .thenComparing(User::getUserId))
                .orElse(null);
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
        staff.setLastAssignedAt(LocalDateTime.now());
        userRepository.save(staff);
        userNotificationService.notifyOrderAssigned(savedAssignment);
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

    private long activeCount(User staff) {
        return assignmentRepository.countCurrentActiveOrders(staff.getUserId(), ACTIVE_ORDER_STATUSES);
    }

    private int maxActiveOrders(User staff) {
        return staff.getMaxActiveOrders() == null || staff.getMaxActiveOrders() < 1 ? 5 : staff.getMaxActiveOrders();
    }

    private double workloadScore(User staff) {
        return assignmentRepository.findCurrentActiveStatuses(staff.getUserId(), ACTIVE_ORDER_STATUSES).stream()
                .mapToDouble(this::statusWeight).sum();
    }

    private Map<String, Long> workloadBreakdown(User staff) {
        return assignmentRepository.findCurrentActiveStatuses(staff.getUserId(), ACTIVE_ORDER_STATUSES).stream()
                .collect(Collectors.groupingBy(this::normalize, Collectors.counting()));
    }

    private double statusWeight(String status) {
        return switch (normalize(status)) {
            case "CONFIRMED" -> 1.2;
            case "PROCESSING" -> 2.0;
            case "READY_FOR_PICKUP" -> 0.7;
            case "PACKED" -> 0.8;
            case "SHIPPING" -> 0.4;
            case "DELIVERED" -> 0.2;
            case "FAILED" -> 1.5;
            default -> 1.0;
        };
    }
}
