package com.fpoly.marcusstore.controller.chat;

import com.fpoly.marcusstore.dto.chat.ChatMessageDTO;
import com.fpoly.marcusstore.dto.chat.ChatSessionDTO;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.chat.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/live-chat")
@RequiredArgsConstructor
public class ClientChatController {

    private final ChatSessionService chatSessionService;

    // Marcus thêm: server tự gắn phiên với tài khoản đăng nhập, client không truyền
    // roomId.
    @PostMapping("/session")
    public ResponseEntity<ApiResponse<ChatSessionDTO>> startSession(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                chatSessionService.startOrResumeCustomerSession(authentication.getName())));
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<ChatSessionDTO>> getCurrentSession(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                chatSessionService.getCustomerSession(authentication.getName())));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ChatMessageDTO>>> getHistory(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                chatSessionService.getCustomerHistory(authentication.getName())));
    }

    @DeleteMapping("/session")
    public ResponseEntity<ApiResponse<String>> endSession(Authentication authentication) {
        chatSessionService.endCustomerSession(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Phiên chat đã kết thúc."));
    }
}
