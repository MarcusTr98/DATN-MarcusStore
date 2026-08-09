package com.fpoly.marcusstore.service.chat;

import com.fpoly.marcusstore.dto.chat.ChatMessageDTO;
import com.fpoly.marcusstore.dto.chat.ChatSessionDTO;
import com.fpoly.marcusstore.repository.contact.ChatSessionMetricRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ChatSessionServiceTest {

        private ChatSessionService chatSessionService;

        @BeforeEach
        void setUp() {
                // Marcus sửa: Live Chat chỉ lưu metadata vận hành, tuyệt đối không lưu nội
                // dung.
                chatSessionService = new ChatSessionService(
                                mock(SimpMessagingTemplate.class),
                                mock(ChatSessionMetricRepository.class));
        }

        @Test
        void createsOpaqueRoomAndResumesSameCustomerSession() {
                ChatSessionDTO firstSession = chatSessionService.startOrResumeCustomerSession("customer-a");
                ChatSessionDTO resumedSession = chatSessionService.startOrResumeCustomerSession("customer-a");
                ChatSessionDTO anotherCustomer = chatSessionService.startOrResumeCustomerSession("customer-b");

                assertEquals(firstSession.getRoomId(), resumedSession.getRoomId());
                assertNotEquals("customer-a", firstSession.getRoomId());
                assertNotEquals(firstSession.getRoomId(), anotherCustomer.getRoomId());
        }

        @Test
        void preventsAnotherAdminFromTakingOrReplyingToClaimedRoom() {
                ChatSessionDTO session = chatSessionService.startOrResumeCustomerSession("customer");
                chatSessionService.sendCustomerMessage("customer", "Tôi cần hỗ trợ");
                chatSessionService.claimRoom(session.getRoomId(), "admin-a");

                assertThrows(IllegalStateException.class,
                                () -> chatSessionService.claimRoom(session.getRoomId(), "admin-b"));
                assertThrows(IllegalStateException.class,
                                () -> chatSessionService.sendAdminMessage(session.getRoomId(), "admin-b", "Xin chào"));

                ChatMessageDTO reply = chatSessionService.sendAdminMessage(
                                session.getRoomId(), "admin-a", "Marcus đang hỗ trợ bạn");
                assertEquals("ADMIN", reply.getSenderRole());
        }

        @Test
        void validatesMessageAndRemovesSessionWhenCustomerEndsIt() {
                chatSessionService.startOrResumeCustomerSession("customer");

                assertThrows(IllegalArgumentException.class,
                                () -> chatSessionService.sendCustomerMessage("customer", "   "));
                assertThrows(IllegalArgumentException.class,
                                () -> chatSessionService.sendCustomerMessage("customer", "a".repeat(1001)));

                chatSessionService.endCustomerSession("customer");
                assertThrows(IllegalStateException.class,
                                () -> chatSessionService.getCustomerHistory("customer"));
        }
}
