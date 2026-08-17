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
        when(assignmentRepository.save(any(OrderAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderAssignmentServiceImpl service = new OrderAssignmentServiceImpl(
                assignmentRepository, mock(OrderRepository.class), userRepository);

        service.assignAutomatically(order);

        ArgumentCaptor<OrderAssignment> assignmentCaptor = ArgumentCaptor.forClass(OrderAssignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getStaff()).isSameAs(availableStaff);
        assertThat(assignmentCaptor.getValue().getAssignmentType()).isEqualTo("AUTO");
        assertThat(assignmentCaptor.getValue().getIsCurrent()).isTrue();
    }

    @Test
    void skipsAutoAssignmentWhenOrderAlreadyHasCurrentAssignee() {
        OrderAssignmentRepository assignmentRepository = mock(OrderAssignmentRepository.class);
        Order order = new Order();
        order.setOrderId(102);
        when(assignmentRepository.findCurrentByOrderId(102)).thenReturn(Optional.of(new OrderAssignment()));

        OrderAssignmentServiceImpl service = new OrderAssignmentServiceImpl(
                assignmentRepository, mock(OrderRepository.class), mock(UserRepository.class));

        service.assignAutomatically(order);

        verify(assignmentRepository, never()).save(any(OrderAssignment.class));
    }

    private User staff(int id, String name) {
        User user = new User();
        user.setUserId(id);
        user.setFullName(name);
        user.setIsActive(true);
        return user;
    }
}
