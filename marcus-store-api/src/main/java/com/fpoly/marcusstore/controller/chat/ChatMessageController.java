package com.fpoly.marcusstore.controller.chat;

import com.fpoly.marcusstore.dto.chat.ChatMessageDTO;
import com.fpoly.marcusstore.service.chat.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatSessionService chatSessionService;

    // Marcus sửa: khách không được tự khai roomId hay vai trò trong payload.
    @MessageMapping("/chat.customer.send")
    public void sendCustomerMessage(@Payload ChatMessageDTO message, Authentication authentication) {
        if (authentication != null) {
            chatSessionService.sendCustomerMessage(authentication.getName(), message.getContent());
        }
    }

    // Marcus sửa: backend kiểm tra Admin có đúng là người đã claim phòng trước khi
    // gửi.
    @MessageMapping("/chat.admin.send")
    public void sendAdminMessage(@Payload ChatMessageDTO message, Authentication authentication) {
        if (authentication != null) {
            chatSessionService.sendAdminMessage(
                    message.getRoomId(), authentication.getName(), message.getContent());
        }
    }
}
