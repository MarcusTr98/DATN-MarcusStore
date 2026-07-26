package com.fpoly.marcusstore.service.chat;

import com.fpoly.marcusstore.dto.chat.ChatMessageDTO;
import com.fpoly.marcusstore.dto.chat.ChatRoomSummaryDTO;
import com.fpoly.marcusstore.dto.chat.ChatSessionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private static final int MAX_MESSAGES_PER_ROOM = 200;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, ChatRoomSession> sessionsByRoom = new ConcurrentHashMap<>();
    private final Map<String, String> roomByCustomer = new ConcurrentHashMap<>();
    private final AtomicInteger messageSequence = new AtomicInteger();

    // Marcus thêm: chỉ tạo một phòng ngẫu nhiên cho mỗi khách trong suốt phiên đang
    // hoạt động.
    public ChatSessionDTO startOrResumeCustomerSession(String customerUsername) {
        String roomId = roomByCustomer.compute(customerUsername, (username, currentRoomId) -> {
            if (currentRoomId != null && sessionsByRoom.containsKey(currentRoomId)) {
                return currentRoomId;
            }

            String newRoomId = UUID.randomUUID().toString();
            sessionsByRoom.put(newRoomId, new ChatRoomSession(newRoomId, customerUsername));
            return newRoomId;
        });
        return toSessionDTO(requireSession(roomId));
    }

    public ChatSessionDTO getCustomerSession(String customerUsername) {
        String roomId = roomByCustomer.get(customerUsername);
        ChatRoomSession session = roomId == null ? null : sessionsByRoom.get(roomId);
        return session == null ? null : toSessionDTO(session);
    }

    public List<ChatMessageDTO> getCustomerHistory(String customerUsername) {
        ChatRoomSession session = requireCustomerSession(customerUsername);
        return session.snapshotMessages();
    }

    public List<ChatMessageDTO> getAdminRoomHistory(String roomId) {
        return requireSession(roomId).snapshotMessages();
    }

    public ChatMessageDTO sendCustomerMessage(String customerUsername, String content) {
        ChatRoomSession session = requireCustomerSession(customerUsername);
        ChatMessageDTO message = createMessage(session.roomId, customerUsername, "CUSTOMER", content);
        appendAndBroadcast(session, message);
        return message;
    }

    public ChatMessageDTO sendAdminMessage(String roomId, String adminUsername, String content) {
        ChatRoomSession session = requireSession(roomId);
        if (!adminUsername.equals(session.claimedBy)) {
            throw new IllegalStateException("Chỉ nhân viên đã nhận phiên mới được phản hồi khách hàng.");
        }

        ChatMessageDTO message = createMessage(roomId, adminUsername, "ADMIN", content);
        appendAndBroadcast(session, message);
        return message;
    }

    // Marcus sửa: claim là thao tác nguyên tử, Admin khác không thể giành phòng đã
    // được nhận.
    public ChatSessionDTO claimRoom(String roomId, String adminUsername) {
        ChatRoomSession session = requireSession(roomId);
        synchronized (session) {
            if (session.claimedBy != null && !session.claimedBy.equals(adminUsername)) {
                throw new IllegalStateException("Phiên chat đã được " + session.claimedBy + " tiếp nhận.");
            }
            if (adminUsername.equals(session.claimedBy)) {
                return toSessionDTO(session);
            }
            session.claimedBy = adminUsername;
            session.touch();
        }

        messagingTemplate.convertAndSend("/topic/chat.incoming.claimed",
                Map.of("roomId", roomId, "claimedBy", adminUsername));

        ChatMessageDTO joinMessage = createMessage(roomId, adminUsername, "SYSTEM",
                adminUsername + " đã tiếp nhận và đang hỗ trợ bạn.");
        appendAndBroadcast(session, joinMessage);
        return toSessionDTO(session);
    }

    public List<ChatRoomSummaryDTO> getActiveRooms() {
        return sessionsByRoom.values().stream()
                .filter(session -> !session.messages.isEmpty())
                .map(this::toRoomSummary)
                .sorted(Comparator
                        .comparing(ChatRoomSummaryDTO::isUnclaimed).reversed()
                        .thenComparing(ChatRoomSummaryDTO::getLastTimestamp,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public void endCustomerSession(String customerUsername) {
        ChatRoomSession session = requireCustomerSession(customerUsername);
        removeAndNotify(session);
    }

    public void endAdminSession(String roomId, String adminUsername) {
        ChatRoomSession session = requireSession(roomId);
        if (!adminUsername.equals(session.claimedBy)) {
            throw new IllegalStateException("Chỉ nhân viên đang phụ trách mới được kết thúc phiên.");
        }
        removeAndNotify(session);
    }

    // Marcus thêm: phiên chat không lưu database và tự hủy sau 30 phút không hoạt
    // động.
    @Scheduled(fixedDelay = 60_000)
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        sessionsByRoom.values().stream()
                .filter(session -> Duration.between(session.lastActivity, now).compareTo(SESSION_TTL) > 0)
                .toList()
                .forEach(this::removeAndNotify);
    }

    private void appendAndBroadcast(ChatRoomSession session, ChatMessageDTO message) {
        session.append(message);

        // Marcus sửa: khách chỉ nhận tin qua user queue do Spring gắn với Principal.
        messagingTemplate.convertAndSendToUser(session.customerUsername, "/queue/live-chat", message);
        messagingTemplate.convertAndSend("/topic/chat.room." + session.roomId, message);
        messagingTemplate.convertAndSend("/topic/chat.incoming", toRoomSummary(session));
    }

    private void removeAndNotify(ChatRoomSession session) {
        if (!sessionsByRoom.remove(session.roomId, session)) {
            return;
        }
        roomByCustomer.remove(session.customerUsername, session.roomId);
        Map<String, String> event = Map.of("roomId", session.roomId, "status", "CLOSED");
        messagingTemplate.convertAndSendToUser(session.customerUsername, "/queue/live-chat-ended", event);
        messagingTemplate.convertAndSend("/topic/chat.room." + session.roomId + ".ended", event);
        messagingTemplate.convertAndSend("/topic/chat.incoming.ended", event);
    }

    private ChatMessageDTO createMessage(String roomId, String sender, String role, String rawContent) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống.");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Tin nhắn không được vượt quá 1000 ký tự.");
        }
        return ChatMessageDTO.builder()
                .id(messageSequence.incrementAndGet())
                .roomId(roomId)
                .sender(sender)
                .senderRole(role)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private ChatRoomSession requireCustomerSession(String customerUsername) {
        String roomId = roomByCustomer.get(customerUsername);
        if (roomId == null) {
            throw new IllegalStateException("Phiên chat chưa được khởi tạo hoặc đã hết hạn.");
        }
        ChatRoomSession session = sessionsByRoom.get(roomId);
        if (session == null || !session.customerUsername.equals(customerUsername)) {
            roomByCustomer.remove(customerUsername, roomId);
            throw new IllegalStateException("Phiên chat không còn tồn tại.");
        }
        session.touch();
        return session;
    }

    private ChatRoomSession requireSession(String roomId) {
        ChatRoomSession session = sessionsByRoom.get(roomId);
        if (session == null) {
            throw new IllegalArgumentException("Không tìm thấy phiên chat.");
        }
        session.touch();
        return session;
    }

    private ChatSessionDTO toSessionDTO(ChatRoomSession session) {
        return ChatSessionDTO.builder()
                .roomId(session.roomId)
                .claimedBy(session.claimedBy)
                .active(true)
                .build();
    }

    private ChatRoomSummaryDTO toRoomSummary(ChatRoomSession session) {
        ChatMessageDTO lastMessage = session.lastMessage();
        return ChatRoomSummaryDTO.builder()
                .roomId(session.roomId)
                .customerUsername(session.customerUsername)
                .lastMessage(lastMessage == null ? "" : lastMessage.getContent())
                .lastTimestamp(lastMessage == null ? session.lastActivity : lastMessage.getTimestamp())
                .claimedBy(session.claimedBy)
                .unclaimed(session.claimedBy == null)
                .build();
    }

    private static final class ChatRoomSession {
        private final String roomId;
        private final String customerUsername;
        private final List<ChatMessageDTO> messages = new ArrayList<>();
        private volatile String claimedBy;
        private volatile LocalDateTime lastActivity = LocalDateTime.now();

        private ChatRoomSession(String roomId, String customerUsername) {
            this.roomId = roomId;
            this.customerUsername = customerUsername;
        }

        private synchronized void append(ChatMessageDTO message) {
            messages.add(message);
            if (messages.size() > MAX_MESSAGES_PER_ROOM) {
                messages.remove(0);
            }
            touch();
        }

        private synchronized List<ChatMessageDTO> snapshotMessages() {
            touch();
            return List.copyOf(messages);
        }

        private synchronized ChatMessageDTO lastMessage() {
            return messages.isEmpty() ? null : messages.get(messages.size() - 1);
        }

        private void touch() {
            lastActivity = LocalDateTime.now();
        }
    }
}
