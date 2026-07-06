package com.fpoly.marcusstore.service.chat;

import com.fpoly.marcusstore.dto.chat.ChatMessageDTO;
import com.fpoly.marcusstore.dto.chat.ChatRoomSummaryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionService {

    private final SimpMessagingTemplate messagingTemplate;

    // Lưu trữ tin nhắn tạm thời của mỗi phòng
    private final Map<String, List<ChatMessageDTO>> roomMessages = new ConcurrentHashMap<>();

    // Lưu trữ trạng thái phòng (roomId => username của Admin đang hỗ trợ)
    private final Map<String, String> claimedRooms = new ConcurrentHashMap<>();

    // Thêm tin nhắn vào RAM và Broadcast
    public ChatMessageDTO saveAndBroadcastMessage(ChatMessageDTO message) {
        // Đảm bảo list tin nhắn được khởi tạo an toàn
        roomMessages.computeIfAbsent(message.getRoomId(), k -> new ArrayList<>()).add(message);

        // Broadcast vào topic riêng của phòng
        messagingTemplate.convertAndSend("/topic/chat.room." + message.getRoomId(), message);

        // Nếu phòng chưa có Admin nào nhận, bắn thông báo tổng đài cho tất cả Admin
        // đang rảnh
        if (!claimedRooms.containsKey(message.getRoomId())) {
            messagingTemplate.convertAndSend("/topic/chat.incoming", message);
        }

        return message;
    }

    // Lấy lịch sử tin nhắn (Dùng khi Client vô tình F5)
    public List<ChatMessageDTO> getRoomHistory(String roomId) {
        return roomMessages.getOrDefault(roomId, new ArrayList<>());
    }

    // Admin bấm nhận hỗ trợ khách hàng này
    public void claimRoom(String roomId, String adminUsername) {
        claimedRooms.put(roomId, adminUsername);
        log.info("Admin {} đã nhận phòng chat {}", adminUsername, roomId);

        // Báo cho các Admin khác biết phòng này đã có chủ
        messagingTemplate.convertAndSend("/topic/chat.incoming.claimed",
                Map.of("roomId", roomId, "claimedBy", adminUsername));

        // Báo cho chính Khách hàng trong phòng biết đã có nhân viên tiếp nhận
        ChatMessageDTO systemMsg = ChatMessageDTO.builder()
                .roomId(roomId)
                .sender("system")
                .senderRole("SYSTEM")
                .content(adminUsername + " đã tham gia hỗ trợ bạn")
                .build();
        messagingTemplate.convertAndSend("/topic/chat.room." + roomId, systemMsg);
    }

    // list toàn bộ phòng chat đang có dữ liệu (dùng khi Admin vừa đăng nhập,
    // để k bỏ lỡ các phòng đã có tin nhắn trước khi Admin online)
    public List<ChatRoomSummaryDTO> getActiveRooms() {
        List<ChatRoomSummaryDTO> result = new ArrayList<>();

        roomMessages.forEach((roomId, messages) -> {
            if (messages.isEmpty())
                return;

            ChatMessageDTO lastMsg = messages.get(messages.size() - 1);
            result.add(ChatRoomSummaryDTO.builder()
                    .roomId(roomId)
                    .lastMessage(lastMsg.getContent())
                    .lastTimestamp(lastMsg.getTimestamp()) // Có thể null
                    .claimedBy(claimedRooms.get(roomId))
                    .unclaimed(!claimedRooms.containsKey(roomId))
                    .build());
        });

        // TẠM THỜI TẮT HÀM SORT CŨ GÂY LỖI 500 VÀ DÙNG HÀM NÀY
        result.sort((r1, r2) -> {
            // Ưu tiên phòng chưa ai nhận lên đầu
            if (r1.isUnclaimed() && !r2.isUnclaimed())
                return -1;
            if (!r1.isUnclaimed() && r2.isUnclaimed())
                return 1;
            // Nếu cùng trạng thái, cái nào mới hơn xếp trên
            if (r1.getLastTimestamp() == null)
                return 1;
            if (r2.getLastTimestamp() == null)
                return -1;
            return r2.getLastTimestamp().compareTo(r1.getLastTimestamp());
        });

        return result;
    }

    // Dọn dẹp sạch sẽ RAM khi kết thúc phiên chat
    public void endSession(String roomId) {
        roomMessages.remove(roomId);
        claimedRooms.remove(roomId);
        log.info("Đã xóa hoàn toàn dữ liệu phòng chat {} khỏi RAM", roomId);

        // Thông báo đóng khung chat
        messagingTemplate.convertAndSend("/topic/chat.room." + roomId + ".ended",
                Map.of("status", "CLOSED"));
    }
}