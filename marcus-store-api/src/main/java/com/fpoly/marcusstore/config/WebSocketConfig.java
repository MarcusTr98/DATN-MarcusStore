package com.fpoly.marcusstore.config;

import com.fpoly.marcusstore.security.CustomUserDetailsService;
import com.fpoly.marcusstore.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-endpoint")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // Chỉ chặn kiểm tra ở bước CONNECT đầu tiên
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);

                        if (jwtUtils.validateJwtToken(token)) {
                            String username = jwtUtils.getUserNameFromJwtToken(token);
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                            accessor.setUser(authentication);
                        } else {
                            throw new AccessDeniedException("Token không hợp lệ hoặc đã hết hạn!");
                        }
                    } else {
                        // Khách Guest => Chặn ngay lập tức
                        throw new AccessDeniedException(
                                "Truy cập bị từ chối! Chưa đăng nhập không được sử dụng Live Chat của hệ thống!");
                    }
                }

                if (accessor != null && accessor.getDestination() != null
                        && (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                                || StompCommand.SEND.equals(accessor.getCommand()))) {
                    Authentication authentication = (Authentication) accessor.getUser();
                    boolean isAdminOrStaff = authentication != null
                            && authentication.getAuthorities().stream()
                                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                                            || "ROLE_STAFF".equals(authority.getAuthority()));

                    String destination = accessor.getDestination();
                    // Marcus thêm: topic quản trị và từng phòng chat chỉ dành cho Admin/Staff.
                    boolean adminOnlyDestination = destination.startsWith("/topic/admin/")
                            || destination.startsWith("/topic/chat.incoming")
                            || destination.startsWith("/topic/chat.room.")
                            || destination.equals("/app/chat.admin.send");
                    if (adminOnlyDestination && !isAdminOrStaff) {
                        throw new AccessDeniedException("Bạn không có quyền truy cập kênh quản trị.");
                    }

                    // Marcus thêm: Admin không được giả lập tin nhắn khách hàng.
                    if (destination.equals("/app/chat.customer.send") && isAdminOrStaff) {
                        throw new AccessDeniedException("Tài khoản quản trị không được gửi qua kênh khách hàng.");
                    }
                }
                return message;
            }
        });
    }
}
