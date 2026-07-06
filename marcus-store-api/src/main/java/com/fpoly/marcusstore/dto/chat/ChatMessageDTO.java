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
public class ChatMessageDTO {
    private String roomId; // Tên phòng (Ví dụ: username của khách)
    private String sender; // Người gửi (username)
    private String senderRole; // CUSTOMER hoặc ADMIN
    private String content; // Nội dung tin nhắn
    private LocalDateTime timestamp;
}