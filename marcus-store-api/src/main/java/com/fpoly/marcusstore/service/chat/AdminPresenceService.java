package com.fpoly.marcusstore.service.chat;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPresenceService {

    // Key: SessionID của WebSocket, Value: Username của Admin
    private final Map<String, String> activeAdminSessions = new ConcurrentHashMap<>();
    private final Set<String> availableAdmins = ConcurrentHashMap.newKeySet();

    // Đánh dấu các Username đang trong trạng thái chờ rớt mạng
    private final Map<String, ScheduledFuture<?>> pendingOfflineTasks = new ConcurrentHashMap<>();

    private final TaskScheduler taskScheduler;
    private final SimpMessagingTemplate messagingTemplate;

    private static final long GRACE_PERIOD_SECONDS = 5;

    public void registerAdminOnline(String sessionId, String username) {
        boolean wasOnline = hasAnyAdminOnline();
        activeAdminSessions.put(sessionId, username);
        // Marcus thêm: khi mở Admin Panel, nhân viên mặc định sẵn sàng và có thể chủ
        // động tạm dừng.
        availableAdmins.add(username);

        // Hủy lệnh set Offline nếu Admin reconnect kịp thời
        ScheduledFuture<?> pendingTask = pendingOfflineTasks.remove(username);
        if (pendingTask != null) {
            pendingTask.cancel(false);
            log.info("Admin {} reconnect kịp thời, hủy lệnh báo offline", username);
        }

        // Nếu hệ thống vừa chuyển từ 0 => 1 Admin Online, báo cho toàn bộ KH biết
        if (!wasOnline && hasAnyAdminOnline()) {
            broadcastPresence();
        }
    }

    public void handleAdminDisconnect(String sessionId) {
        String username = activeAdminSessions.remove(sessionId);
        if (username == null)
            return;

        // Admin đang mở nhiều tab, tắt 1 tab vẫn còn tab khác => Kệ
        if (activeAdminSessions.containsValue(username))
            return;

        // đếm ngược 5s trước khi thực sự báo Offline
        ScheduledFuture<?> task = taskScheduler.schedule(() -> {
            pendingOfflineTasks.remove(username);
            if (!activeAdminSessions.containsValue(username)) {
                boolean wasAvailable = availableAdmins.remove(username);
                if (wasAvailable) {
                    broadcastPresence();
                }
            }
        }, Instant.now().plusSeconds(GRACE_PERIOD_SECONDS));

        pendingOfflineTasks.put(username, task);
    }

    public boolean hasAnyAdminOnline() {
        return activeAdminSessions.values().stream().anyMatch(availableAdmins::contains);
    }

    public boolean isAdminAvailable(String username) {
        return availableAdmins.contains(username) && activeAdminSessions.containsValue(username);
    }

    public boolean setAvailability(String username, boolean available) {
        if (!activeAdminSessions.containsValue(username)) {
            throw new IllegalStateException("Kết nối Live Chat chưa sẵn sàng.");
        }
        boolean before = hasAnyAdminOnline();
        if (available) {
            availableAdmins.add(username);
        } else {
            availableAdmins.remove(username);
        }
        if (before != hasAnyAdminOnline()) {
            broadcastPresence();
        }
        return isAdminAvailable(username);
    }

    private void broadcastPresence() {
        messagingTemplate.convertAndSend("/topic/chat.presence",
                ApiResponse.success(Map.of("isAdminOnline", hasAnyAdminOnline())));
    }
}
