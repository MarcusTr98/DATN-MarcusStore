package com.fpoly.marcusstore.service.chat;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPresenceService {

    // Key: SessionID của WebSocket, Value: Username của Admin
    private final Map<String, String> activeAdminSessions = new ConcurrentHashMap<>();

    // Đánh dấu các Username đang trong trạng thái chờ rớt mạng
    private final Map<String, ScheduledFuture<?>> pendingOfflineTasks = new ConcurrentHashMap<>();

    private final TaskScheduler taskScheduler;
    private final SimpMessagingTemplate messagingTemplate;

    private static final long GRACE_PERIOD_SECONDS = 5;

    public void registerAdminOnline(String sessionId, String username) {
        activeAdminSessions.put(sessionId, username);

        // Hủy lệnh set Offline nếu Admin reconnect kịp thời
        ScheduledFuture<?> pendingTask = pendingOfflineTasks.remove(username);
        if (pendingTask != null) {
            pendingTask.cancel(false);
            log.info("Admin {} reconnect kịp thời, hủy lệnh báo offline", username);
        }

        // Nếu hệ thống vừa chuyển từ 0 => 1 Admin Online, báo cho toàn bộ KH biết
        if (countDistinctOnlineAdmins() == 1) {
            broadcastPresence(true);
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
            // Sau 5s, nếu không còn ai online, mới báo tắt Chat UI
            if (countDistinctOnlineAdmins() == 0) {
                broadcastPresence(false);
                log.info("Toàn bộ Admin đã offline, đóng kênh Chat nội bộ.");
            }
        }, Instant.now().plusSeconds(GRACE_PERIOD_SECONDS));

        pendingOfflineTasks.put(username, task);
    }

    public boolean hasAnyAdminOnline() {
        return countDistinctOnlineAdmins() > 0;
    }

    private long countDistinctOnlineAdmins() {
        return activeAdminSessions.values().stream().distinct().count();
    }

    private void broadcastPresence(boolean isOnline) {
        messagingTemplate.convertAndSend("/topic/chat.presence",
                ApiResponse.success(Map.of("isAdminOnline", isOnline)));
    }
}