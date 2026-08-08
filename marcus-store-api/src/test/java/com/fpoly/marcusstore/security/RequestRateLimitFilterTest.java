package com.fpoly.marcusstore.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

// Marcus thêm: bảo vệ API đăng nhập khỏi spam nhưng không ảnh hưởng request hợp lệ.
class RequestRateLimitFilterTest {

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksEleventhLoginRequestWithinOneMinute() throws Exception {
        RequestRateLimitFilter filter = new RequestRateLimitFilter();

        for (int attempt = 1; attempt <= 10; attempt++) {
            MockHttpServletResponse response = execute(filter, "127.0.0.8");
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blocked = execute(filter, "127.0.0.8");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotBlank();
        assertThat(blocked.getContentAsString()).contains("thao tác quá nhanh");
    }

    private MockHttpServletResponse execute(RequestRateLimitFilter filter, String address) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(address);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
