package com.fpoly.marcusstore.controller.chat;

import com.fpoly.marcusstore.dto.chat.ChatMessageDTO;
import com.fpoly.marcusstore.security.CustomUserDetails;
import com.fpoly.marcusstore.service.chat.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatSessionService chatSessionService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDTO message, StompHeaderAccessor accessor) {
        Authentication auth = (Authentication) accessor.getUser();
        if (auth == null)
            return;

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        // Gắn thông tin bảo mật vào tin nhắn trước khi lưu
        message.setSender(userDetails.getUsername());
        message.setSenderRole(determineRole(auth));
        message.setTimestamp(LocalDateTime.now());

        chatSessionService.saveAndBroadcastMessage(message);
    }

    private String determineRole(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
        return isAdmin ? "ADMIN" : "CUSTOMER";
    }
}