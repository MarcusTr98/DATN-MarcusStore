package com.fpoly.marcusstore.service.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketPresenceListener {

    private final AdminPresenceService adminPresenceService;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication auth = (Authentication) accessor.getUser();

        if (auth != null && isAdminRole(auth)) {
            // Marcus sửa: dùng Principal chuẩn, không phụ thuộc kiểu CustomUserDetails cụ
            // thể.
            adminPresenceService.registerAdminOnline(accessor.getSessionId(), auth.getName());
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        adminPresenceService.handleAdminDisconnect(accessor.getSessionId());
    }

    // giám sát trạng thái của ADMIN và STAFF
    private boolean isAdminRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
    }
}
