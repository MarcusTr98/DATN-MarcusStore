package com.fpoly.marcusstore.controller.chat;

import com.fpoly.marcusstore.dto.chat.ChatMessageDTO;
import com.fpoly.marcusstore.dto.chat.ChatRoomSummaryDTO;
import com.fpoly.marcusstore.dto.chat.ChatSessionDTO;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.chat.AdminPresenceService;
import com.fpoly.marcusstore.service.chat.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import com.fpoly.marcusstore.dto.request.AdminAvailabilityRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/admin/live-chat")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
@Validated
public class AdminChatController {

    private final ChatSessionService chatSessionService;
    private final AdminPresenceService adminPresenceService;

    @GetMapping("/active-rooms")
    public ApiResponse<List<ChatRoomSummaryDTO>> getActiveRooms() {
        return ApiResponse.success(chatSessionService.getActiveRooms());
    }

    @GetMapping("/rooms/{roomId}/history")
    public ApiResponse<List<ChatMessageDTO>> getHistory(
            @PathVariable @Pattern(regexp = "^[A-Za-z0-9_-]{1,100}$") String roomId) {
        return ApiResponse.success(chatSessionService.getAdminRoomHistory(roomId));
    }

    @PutMapping("/rooms/{roomId}/claim")
    public ApiResponse<ChatSessionDTO> claimRoom(
            @PathVariable @Pattern(regexp = "^[A-Za-z0-9_-]{1,100}$") String roomId,
            Authentication authentication) {
        return ApiResponse.success(chatSessionService.claimRoom(roomId, authentication.getName()));
    }

    @DeleteMapping("/rooms/{roomId}")
    public ApiResponse<String> endRoom(
            @PathVariable @Pattern(regexp = "^[A-Za-z0-9_-]{1,100}$") String roomId,
            Authentication authentication) {
        chatSessionService.endAdminSession(roomId, authentication.getName());
        return ApiResponse.success("Phiên chat đã kết thúc.");
    }

    @GetMapping("/availability")
    public ApiResponse<Map<String, Boolean>> getAvailability(Authentication authentication) {
        return ApiResponse.success(Map.of(
                "available", adminPresenceService.isAdminAvailable(authentication.getName())));
    }

    @PutMapping("/availability")
    public ApiResponse<Map<String, Boolean>> updateAvailability(
            @Valid @RequestBody AdminAvailabilityRequest request, Authentication authentication) {
        boolean available = request.getAvailable();
        return ApiResponse.success(Map.of(
                "available", adminPresenceService.setAvailability(authentication.getName(), available)));
    }
}
