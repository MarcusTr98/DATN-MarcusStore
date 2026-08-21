package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderAssignment;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.shopping.OrderAssignmentRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.service.impl.OrderAssignmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import com.fpoly.marcusstore.security.CustomUserDetails;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderAssignmentServiceImplTest {

    @Test
    void automaticallyAssignsOrderToStaffWithLowestActiveLoad() {
        OrderAssignmentRepository assignmentRepository = mock(OrderAssignmentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        Order order = new Order();
        order.setOrderId(101);

        User busyStaff = staff(2, "Nhân viên bận");
        User availableStaff = staff(3, "Nhân viên rảnh");
        when(assignmentRepository.findCurrentByOrderId(101)).thenReturn(Optional.empty());
        when(userRepository.findActiveStaffWithOrderUpdatePermissionForAssignment())
                .thenReturn(List.of(busyStaff, availableStaff));
        when(assignmentRepository.countCurrentActiveOrders(eq(2), any())).thenReturn(4L);
        when(assignmentRepository.countCurrentActiveOrders(eq(3), any())).thenReturn(1L);
        when(assignmentRepository.findCurrentActiveStatuses(eq(2), any()))
                .thenReturn(List.of("PROCESSING", "PROCESSING", "PENDING", "PENDING"));
        when(assignmentRepository.findCurrentActiveStatuses(eq(3), any())).thenReturn(List.of("PENDING"));
        when(assignmentRepository.save(any(OrderAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderRepository orderRepository = mock(OrderRepository.class);
        UserNotificationService notificationService = mock(UserNotificationService.class);
        when(orderRepository.findByIdForUpdate(101)).thenReturn(Optional.of(order));
        OrderAssignmentServiceImpl service = new OrderAssignmentServiceImpl(
                assignmentRepository, orderRepository, userRepository, notificationService);

        service.assignAutomatically(order);

        ArgumentCaptor<OrderAssignment> assignmentCaptor = ArgumentCaptor.forClass(OrderAssignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getStaff()).isSameAs(availableStaff);
        assertThat(assignmentCaptor.getValue().getAssignmentType()).isEqualTo("AUTO");
        assertThat(assignmentCaptor.getValue().getIsCurrent()).isTrue();
        verify(notificationService).notifyOrderAssigned(assignmentCaptor.getValue());
    }

    @Test
    void skipsAutoAssignmentWhenOrderAlreadyHasCurrentAssignee() {
        OrderAssignmentRepository assignmentRepository = mock(OrderAssignmentRepository.class);
        Order order = new Order();
        order.setOrderId(102);
        when(assignmentRepository.findCurrentByOrderId(102)).thenReturn(Optional.of(new OrderAssignment()));

        OrderRepository orderRepository = mock(OrderRepository.class);
        when(orderRepository.findByIdForUpdate(102)).thenReturn(Optional.of(order));
        OrderAssignmentServiceImpl service = new OrderAssignmentServiceImpl(
                assignmentRepository, orderRepository, mock(UserRepository.class), mock(UserNotificationService.class));

        service.assignAutomatically(order);

        verify(assignmentRepository, never()).save(any(OrderAssignment.class));
    }

    @Test
    void dashboardDistributesPlannedOrdersAcrossStaffInsteadOfSuggestingOneStaffForAll() {
        OrderAssignmentRepository assignmentRepository = mock(OrderAssignmentRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User staffOne = staff(1, "Staff 1");
        User staffTwo = staff(2, "Staff 2");
        when(userRepository.findActiveStaffWithOrderUpdatePermission()).thenReturn(List.of(staffOne, staffTwo));
        when(orderRepository.findPendingUnassignedOrders(any())).thenReturn(List.of(
                pendingOrder("ORD-1"), pendingOrder("ORD-2"), pendingOrder("ORD-3")));

        OrderAssignmentServiceImpl service = new OrderAssignmentServiceImpl(
                assignmentRepository, orderRepository, userRepository, mock(UserNotificationService.class));

        var dashboard = service.getDashboard();

        assertThat(dashboard.getPendingOrders())
                .extracting("plannedStaffId")
                .containsExactly(1, 2, 1);
    }

    @Test
    void skipsUnpaidVnPayOrderDuringAutomaticAssignment() {
        OrderAssignmentRepository assignmentRepository = mock(OrderAssignmentRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        Order order = pendingOrder("ORD-VNPAY");
        order.setOrderId(103);
        order.setPaymentMethod("VNPAY");
        order.setPaymentStatus("UNPAID");
        when(orderRepository.findByIdForUpdate(103)).thenReturn(Optional.of(order));

        OrderAssignmentServiceImpl service = new OrderAssignmentServiceImpl(
                assignmentRepository, orderRepository, mock(UserRepository.class), mock(UserNotificationService.class));

        service.assignAutomatically(order);

        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void rejectsManualAssignmentForCompletedOrder() {
        OrderAssignmentRepository assignmentRepository = mock(OrderAssignmentRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        Order order = pendingOrder("ORD-DONE");
        order.setOrderId(104);
        order.setOrderStatus("COMPLETED");
        when(orderRepository.findByOrderCodeForUpdate("ORD-DONE")).thenReturn(Optional.of(order));
        when(userRepository.findActiveStaffWithOrderUpdatePermissionById(2)).thenReturn(Optional.of(staff(2, "Staff")));

        OrderAssignmentServiceImpl service = new OrderAssignmentServiceImpl(
                assignmentRepository, orderRepository, userRepository, mock(UserNotificationService.class));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.assignManually("ORD-DONE", 2, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED");
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void rejectsStaffWhenOrderIsAssignedToSomeoneElse() {
        OrderAssignmentRepository assignmentRepository = mock(OrderAssignmentRepository.class);
        authenticate(7, "ROLE_STAFF");
        when(assignmentRepository.existsByOrderOrderIdAndStaffUserIdAndIsCurrentTrue(105, 7)).thenReturn(false);
        OrderAssignmentServiceImpl service = new OrderAssignmentServiceImpl(
                assignmentRepository, mock(OrderRepository.class), mock(UserRepository.class),
                mock(UserNotificationService.class));

        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.assertCurrentStaffCanAccess(105))
                    .isInstanceOf(AccessDeniedException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void allowsAdminToWorkOnAnyAssignedOrder() {
        OrderAssignmentRepository assignmentRepository = mock(OrderAssignmentRepository.class);
        authenticate(1, "ROLE_ADMIN");
        OrderAssignmentServiceImpl service = new OrderAssignmentServiceImpl(
                assignmentRepository, mock(OrderRepository.class), mock(UserRepository.class),
                mock(UserNotificationService.class));

        try {
            service.assertCurrentStaffCanAccess(106);
            verify(assignmentRepository, never())
                    .existsByOrderOrderIdAndStaffUserIdAndIsCurrentTrue(anyInt(), anyInt());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void staffClaimsOldestEligibleOrderAsSelfAssignment() {
        OrderAssignmentRepository assignmentRepository = mock(OrderAssignmentRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserNotificationService notificationService = mock(UserNotificationService.class);
        User staff = staff(8, "Staff 8");
        staff.setAcceptingOrders(true);
        staff.setMaxActiveOrders(5);
        Order order = pendingOrder("ORD-CLAIM");
        order.setOrderId(107);
        authenticate(8, "ROLE_STAFF");
        when(userRepository.findEligibleStaffByIdForAssignment(8)).thenReturn(Optional.of(staff));
        when(assignmentRepository.findCurrentActiveStatuses(eq(8), any())).thenReturn(List.of());
        when(orderRepository.findNextClaimableOrderForUpdate(any())).thenReturn(List.of(order));
        when(assignmentRepository.save(any(OrderAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        OrderAssignmentServiceImpl service = new OrderAssignmentServiceImpl(
                assignmentRepository, orderRepository, userRepository, notificationService);

        try {
            assertThat(service.claimNextOrder()).isEqualTo("ORD-CLAIM");
            ArgumentCaptor<OrderAssignment> captor = ArgumentCaptor.forClass(OrderAssignment.class);
            verify(assignmentRepository).save(captor.capture());
            assertThat(captor.getValue().getAssignmentType()).isEqualTo("SELF");
            assertThat(captor.getValue().getStaff()).isSameAs(staff);
            verify(notificationService).notifyOrderAssigned(captor.getValue());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(int userId, String role) {
        CustomUserDetails principal = new CustomUserDetails(
                userId, "user" + userId, "user@example.com", "User", "password",
                List.of(new SimpleGrantedAuthority(role)), true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private User staff(int id, String name) {
        User user = new User();
        user.setUserId(id);
        user.setFullName(name);
        user.setIsActive(true);
        return user;
    }

    private Order pendingOrder(String orderCode) {
        Order order = new Order();
        order.setOrderCode(orderCode);
        order.setOrderStatus("PENDING");
        order.setPaymentMethod("COD");
        order.setPaymentStatus("UNPAID");
        return order;
    }
}
