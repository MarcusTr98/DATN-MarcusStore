package com.fpoly.marcusstore.service.chat;

import com.fpoly.marcusstore.dto.chat.ChatMessageDTO;
import com.fpoly.marcusstore.dto.chat.ChatRoomSummaryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionService {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, List<ChatMessageDTO>> roomMessages = new ConcurrentHashMap<>();
    private final Map<String, String> claimedRooms = new ConcurrentHashMap<>();

    public ChatMessageDTO saveAndBroadcastMessage(ChatMessageDTO message) {
        if (message.getTimestamp() == null)
            message.setTimestamp(LocalDateTime.now());

        roomMessages.computeIfAbsent(message.getRoomId(), k -> new ArrayList<>()).add(message);
        messagingTemplate.convertAndSend("/topic/chat.room." + message.getRoomId(), message);
        if ("CUSTOMER".equals(message.getSenderRole())) {
            messagingTemplate.convertAndSend("/topic/chat.incoming", message);
        }
        return message;
    }

    public void claimRoom(String roomId, String adminUsername) {
        claimedRooms.put(roomId, adminUsername);

        // Thông báo cho các Admin khác biết phòng này đã có người nhận (để bỏ trạng
        // thái "chưa nhận")
        messagingTemplate.convertAndSend("/topic/chat.incoming.claimed",
                Map.of("roomId", roomId, "claimedBy", adminUsername));
        ChatMessageDTO joinMsg = ChatMessageDTO.builder()
                .roomId(roomId)
                .sender(adminUsername)
                .senderRole("ADMIN")
                .content(adminUsername + " đã tham gia hỗ trợ bạn")
                .timestamp(LocalDateTime.now())
                .build();
        saveAndBroadcastMessage(joinMsg);
    }

    public List<ChatRoomSummaryDTO> getActiveRooms() {
        List<ChatRoomSummaryDTO> result = new ArrayList<>();
        roomMessages.forEach((roomId, messages) -> {
            if (messages.isEmpty())
                return;
            ChatMessageDTO lastMsg = messages.get(messages.size() - 1);
            result.add(ChatRoomSummaryDTO.builder()
                    .roomId(roomId)
                    .lastMessage(lastMsg.getContent())
                    .lastTimestamp(lastMsg.getTimestamp())
                    .claimedBy(claimedRooms.get(roomId))
                    .unclaimed(!claimedRooms.containsKey(roomId))
                    .build());
        });

        result.sort((r1, r2) -> {
            if (r1.isUnclaimed() != r2.isUnclaimed())
                return r1.isUnclaimed() ? -1 : 1;
            return r2.getLastTimestamp().compareTo(r1.getLastTimestamp());
        });
        return result;
    }

    public List<ChatMessageDTO> getRoomHistory(String roomId) {
        return roomMessages.getOrDefault(roomId, Collections.emptyList());
    }

    public void endSession(String roomId) {
        roomMessages.remove(roomId);
        claimedRooms.remove(roomId);
        messagingTemplate.convertAndSend("/topic/chat.room." + roomId + ".ended", Map.of("status", "CLOSED"));
    }
}