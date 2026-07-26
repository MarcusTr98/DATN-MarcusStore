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
    private Integer id;
    private String roomId; // Marcus sửa: mã UUID ngẫu nhiên, không dùng username làm mã phòng.
    private String sender; // Người gửi (username)
    private String senderRole; // CUSTOMER hoặc ADMIN
    private String content; // Nội dung tin nhắn, tối đa 1000 ký tự.
    private LocalDateTime timestamp;
}
