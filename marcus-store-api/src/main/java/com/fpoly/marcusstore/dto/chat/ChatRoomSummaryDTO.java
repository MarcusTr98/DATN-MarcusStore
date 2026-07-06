package com.fpoly.marcusstore.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tóm tắt 1 phòng chat để hiển thị trong danh sách Inbox bên Admin.
 * Dùng cho API GET /admin/chat/active-rooms.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomSummaryDTO {
    private String roomId; // Username của khách hàng trong phòng
    private String lastMessage; // Nội dung tin nhắn gần nhất
    private LocalDateTime lastTimestamp; // Thời gian tin nhắn gần nhất
    private String claimedBy; // Username Admin đang phụ trách (null nếu chưa ai nhận)
    private boolean unclaimed; // true nếu phòng đang chờ Admin nhận
}