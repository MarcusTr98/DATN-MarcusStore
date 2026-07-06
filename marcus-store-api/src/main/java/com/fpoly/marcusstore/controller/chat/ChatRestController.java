package com.fpoly.marcusstore.controller.chat;

import com.fpoly.marcusstore.dto.chat.ChatMessageDTO;
import com.fpoly.marcusstore.dto.chat.ChatRoomSummaryDTO;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.security.CustomUserDetails;
import com.fpoly.marcusstore.service.chat.AdminPresenceService;
import com.fpoly.marcusstore.service.chat.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final AdminPresenceService adminPresenceService;
    private final ChatSessionService chatSessionService;

    // 1. Client hỏi xem có Admin nào đang Online không
    @GetMapping("/presence")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getPresence() {
        boolean isOnline = adminPresenceService.hasAnyAdminOnline();
        return ResponseEntity.ok(ApiResponse.success(Map.of("isAdminOnline", isOnline)));
    }

    // 2. Client lấy lại lịch sử chat nếu bị load lại trang
    @GetMapping("/rooms/{roomId}/history")
    public ResponseEntity<ApiResponse<List<ChatMessageDTO>>> getHistory(@PathVariable String roomId) {
        return ResponseEntity.ok(ApiResponse.success(chatSessionService.getRoomHistory(roomId)));
    }

    // 3. Admin bấm nhận phòng
    @PutMapping("/rooms/{roomId}/claim")
    public ResponseEntity<ApiResponse<String>> claimRoom(
            @PathVariable String roomId,
            @AuthenticationPrincipal CustomUserDetails currentAdmin) {

        chatSessionService.claimRoom(roomId, currentAdmin.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Đã nhận hỗ trợ phòng " + roomId));
    }

    // 4. Kết thúc chat Xóa RAM
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<String>> endChat(@PathVariable String roomId) {
        chatSessionService.endSession(roomId);
        return ResponseEntity.ok(ApiResponse.success("Phiên chat đã kết thúc"));
    }
}