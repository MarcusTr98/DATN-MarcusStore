package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.contact.AdminNotification;
import com.fpoly.marcusstore.repository.contact.AdminNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Marcus thêm test hồi quy cho lịch sử, badge và payload realtime của chuông.
class AdminNotificationServiceTest {

    private AdminNotificationRepository repository;
    private SimpMessagingTemplate messagingTemplate;
    private AdminNotificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(AdminNotificationRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        service = new AdminNotificationService(repository, messagingTemplate);
    }

    @Test
    void returnsPagedHistoryAndUnreadCount() {
        AdminNotification notification = notification(12, false);
        when(repository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));
        when(repository.countByIsReadFalse()).thenReturn(3L);

        Map<String, Object> result = service.getNotificationsForAdmin(0, 10, false);

        assertThat((List<?>) result.get("list")).hasSize(1);
        assertThat(result.get("unreadCount")).isEqualTo(3L);
        assertThat(result.get("hasMore")).isEqualTo(false);
    }

    @Test
    void publishesDtoForNewNotification() {
        when(repository.saveAndFlush(any(AdminNotification.class))).thenAnswer(invocation -> {
            AdminNotification saved = invocation.getArgument(0);
            saved.setId(15);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        service.createAndSendNotification("ORDER", "Đơn mới", "ORD-15", "ORD-15");

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/admin/notifications"), payload.capture());
        assertThat(((Map<?, ?>) payload.getValue()).get("event")).isEqualTo("NEW");
    }

    @Test
    void rejectsMissingNotificationInsteadOfSilentlySucceeding() {
        when(repository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(404))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    private AdminNotification notification(int id, boolean isRead) {
        AdminNotification notification = new AdminNotification();
        notification.setId(id);
        notification.setType("ORDER");
        notification.setTitle("Đơn mới");
        notification.setMessage("Có đơn hàng mới");
        notification.setReferenceId("ORD-" + id);
        notification.setIsRead(isRead);
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }
}
