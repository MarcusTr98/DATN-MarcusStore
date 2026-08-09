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
import com.fpoly.marcusstore.entity.interaction.ChatSessionMetric;
import com.fpoly.marcusstore.repository.contact.ChatSessionMetricRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private static final int MAX_MESSAGES_PER_ROOM = 200;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    private static final Duration EXPIRY_WARNING_BEFORE = Duration.ofMinutes(5);
    private static final int CUSTOMER_BURST_LIMIT = 8;
    private static final Duration CUSTOMER_BURST_WINDOW = Duration.ofSeconds(10);

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatSessionMetricRepository metricRepository;
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
            ChatRoomSession session = new ChatRoomSession(newRoomId, customerUsername);
            sessionsByRoom.put(newRoomId, session);
            createMetric(session);
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
        session.checkCustomerRateLimit();
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
        if (!session.answered) {
            session.answered = true;
            session.status = "ACTIVE";
            updateMetricFirstResponse(session);
        }
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
            session.claimedAt = LocalDateTime.now();
            session.status = "CLAIMED";
            session.touch();
            updateMetricClaimed(session);
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
        removeAndNotify(session, "CUSTOMER");
    }

    public void endAdminSession(String roomId, String adminUsername) {
        ChatRoomSession session = requireSession(roomId);
        if (!adminUsername.equals(session.claimedBy)) {
            throw new IllegalStateException("Chỉ nhân viên đang phụ trách mới được kết thúc phiên.");
        }
        removeAndNotify(session, "ADMIN");
    }

    // Marcus thêm: phiên chat không lưu database và tự hủy sau 30 phút không hoạt
    // động.
    @Scheduled(fixedDelay = 60_000)
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        sessionsByRoom.values().stream()
                .filter(session -> Duration.between(session.lastActivity, now).compareTo(SESSION_TTL) > 0)
                .toList()
                .forEach(session -> removeAndNotify(session, "TIMEOUT"));

        // Marcus thêm: cảnh báo trước 5 phút, chỉ gửi một lần và không kéo dài TTL.
        sessionsByRoom.values().stream()
                .filter(session -> !session.expiryWarned)
                .filter(session -> Duration.between(session.lastActivity, now)
                        .compareTo(SESSION_TTL.minus(EXPIRY_WARNING_BEFORE)) >= 0)
                .forEach(this::warnBeforeExpiry);
    }

    private void appendAndBroadcast(ChatRoomSession session, ChatMessageDTO message) {
        session.append(message);

        // Marcus sửa: khách chỉ nhận tin qua user queue do Spring gắn với Principal.
        messagingTemplate.convertAndSendToUser(session.customerUsername, "/queue/live-chat", message);
        messagingTemplate.convertAndSend("/topic/chat.room." + session.roomId, message);
        messagingTemplate.convertAndSend("/topic/chat.incoming", toRoomSummary(session));
    }

    private void removeAndNotify(ChatRoomSession session, String closedBy) {
        if (!sessionsByRoom.remove(session.roomId, session)) {
            return;
        }
        roomByCustomer.remove(session.customerUsername, session.roomId);
        session.status = "ENDED";
        finishMetric(session, closedBy);
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
        if (content.matches("(?is).*<\\s*/?\\s*(script|iframe|img|a|style|object|svg)[^>]*>.*")) {
            throw new IllegalArgumentException("Live Chat chỉ chấp nhận nội dung văn bản, không nhận HTML.");
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
                .status(session.status)
                .expiresAt(session.lastActivity.plus(SESSION_TTL))
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
                .status(session.status)
                .waitingSeconds(Math.max(0, Duration.between(session.startedAt,
                        session.claimedAt == null ? LocalDateTime.now() : session.claimedAt).toSeconds()))
                .build();
    }

    private void warnBeforeExpiry(ChatRoomSession session) {
        session.expiryWarned = true;
        ChatMessageDTO warning = createMessage(session.roomId, "SYSTEM", "SYSTEM",
                "Phiên hỗ trợ sẽ tự kết thúc sau 5 phút nếu không có tin nhắn mới.");
        session.appendWithoutTouch(warning);
        messagingTemplate.convertAndSendToUser(session.customerUsername, "/queue/live-chat", warning);
        messagingTemplate.convertAndSend("/topic/chat.room." + session.roomId, warning);
    }

    private void createMetric(ChatRoomSession session) {
        ChatSessionMetric metric = new ChatSessionMetric();
        metric.setSessionId(session.roomId);
        metric.setCustomerHash(sha256(session.customerUsername));
        metric.setStartedAt(session.startedAt);
        metric.setStatus("WAITING_ADMIN");
        metricRepository.save(metric);
    }

    private void updateMetricClaimed(ChatRoomSession session) {
        metricRepository.findById(session.roomId).ifPresent(metric -> {
            metric.setClaimedAt(session.claimedAt);
            metric.setStatus("CLAIMED");
            metricRepository.save(metric);
        });
    }

    private void updateMetricFirstResponse(ChatRoomSession session) {
        metricRepository.findById(session.roomId).ifPresent(metric -> {
            metric.setFirstResponseAt(LocalDateTime.now());
            metric.setAnswered(true);
            metric.setStatus("ACTIVE");
            metricRepository.save(metric);
        });
    }

    private void finishMetric(ChatRoomSession session, String closedBy) {
        metricRepository.findById(session.roomId).ifPresent(metric -> {
            metric.setEndedAt(LocalDateTime.now());
            metric.setAnswered(session.answered);
            metric.setStatus("ENDED");
            metric.setClosedBy(closedBy);
            metricRepository.save(metric);
        });
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể ẩn danh phiên chat.", exception);
        }
    }

    private static final class ChatRoomSession {
        private final String roomId;
        private final String customerUsername;
        private final List<ChatMessageDTO> messages = new ArrayList<>();
        private volatile String claimedBy;
        private final LocalDateTime startedAt = LocalDateTime.now();
        private volatile LocalDateTime claimedAt;
        private volatile String status = "WAITING_ADMIN";
        private volatile boolean answered;
        private volatile boolean expiryWarned;
        private final ArrayDeque<LocalDateTime> customerMessageTimes = new ArrayDeque<>();
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

        private synchronized void appendWithoutTouch(ChatMessageDTO message) {
            messages.add(message);
            if (messages.size() > MAX_MESSAGES_PER_ROOM)
                messages.remove(0);
        }

        private synchronized void checkCustomerRateLimit() {
            LocalDateTime cutoff = LocalDateTime.now().minus(CUSTOMER_BURST_WINDOW);
            while (!customerMessageTimes.isEmpty() && customerMessageTimes.peekFirst().isBefore(cutoff)) {
                customerMessageTimes.removeFirst();
            }
            if (customerMessageTimes.size() >= CUSTOMER_BURST_LIMIT) {
                throw new IllegalStateException("Bạn gửi tin quá nhanh. Vui lòng chờ vài giây.");
            }
            customerMessageTimes.addLast(LocalDateTime.now());
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
            expiryWarned = false;
        }
    }
}
