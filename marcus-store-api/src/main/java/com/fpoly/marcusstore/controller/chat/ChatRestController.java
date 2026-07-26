package com.fpoly.marcusstore.controller.chat;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.chat.AdminPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final AdminPresenceService adminPresenceService;

    // Marcus giữ public duy nhất trạng thái tổng quát, không trả dữ liệu phòng
    // chat.
    @GetMapping("/presence")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getPresence() {
        boolean isOnline = adminPresenceService.hasAnyAdminOnline();
        return ResponseEntity.ok(ApiResponse.success(Map.of("isAdminOnline", isOnline)));
    }

}
