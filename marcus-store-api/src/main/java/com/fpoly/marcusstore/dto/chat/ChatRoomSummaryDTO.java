package com.fpoly.marcusstore.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomSummaryDTO {
    private String roomId; // Marcus sửa: mã UUID nội bộ của phiên chat.
    private String customerUsername; // Tên khách để Admin nhận diện trong Inbox.
    private String lastMessage; // Nội dung tin nhắn gần nhất
    private LocalDateTime lastTimestamp; // Thời gian tin nhắn gần nhất
    private String claimedBy; // Username Admin đang phụ trách (null nếu chưa ai nhận)
    private boolean unclaimed; // true nếu phòng đang chờ Admin nhận
}
