package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.GhnWebhookRequest;
import com.fpoly.marcusstore.service.GhnStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GhnWebhookControllerTest {

    private final GhnStatusService ghnStatusService = mock(GhnStatusService.class);
    private final GhnWebhookController controller = new GhnWebhookController(ghnStatusService);

    @Test
    void rejectsWebhookWhenVerificationTokenIsNotConfigured() {
        ReflectionTestUtils.setField(controller, "ghnWebhookToken", "");

        ResponseEntity<String> response = controller.handleWebhook(null, payload());

        assertEquals(503, response.getStatusCode().value());
        verify(ghnStatusService, never()).applyStatus("GHN-001", "delivered", "WEBHOOK");
    }

    @Test
    void rejectsWebhookWhenVerificationTokenIsInvalid() {
        ReflectionTestUtils.setField(controller, "ghnWebhookToken", "expected-token");

        ResponseEntity<String> response = controller.handleWebhook("wrong-token", payload());

        assertEquals(401, response.getStatusCode().value());
        verify(ghnStatusService, never()).applyStatus("GHN-001", "delivered", "WEBHOOK");
    }

    @Test
    void acceptsWebhookOnlyWithConfiguredVerificationToken() {
        ReflectionTestUtils.setField(controller, "ghnWebhookToken", "expected-token");
        when(ghnStatusService.applyStatus("GHN-001", "delivered", "WEBHOOK"))
                .thenReturn(GhnStatusService.SyncResult.UPDATED);

        ResponseEntity<String> response = controller.handleWebhook("expected-token", payload());

        assertEquals(200, response.getStatusCode().value());
        assertEquals("OK", response.getBody());
        verify(ghnStatusService).applyStatus("GHN-001", "delivered", "WEBHOOK");
    }

    private GhnWebhookRequest payload() {
        GhnWebhookRequest payload = new GhnWebhookRequest();
        payload.setOrderCode("GHN-001");
        payload.setStatus("delivered");
        return payload;
    }
}
