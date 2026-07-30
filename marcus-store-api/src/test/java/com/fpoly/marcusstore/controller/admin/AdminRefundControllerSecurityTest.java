package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.RefundResponse;
import com.fpoly.marcusstore.service.OrderService;
import com.fpoly.marcusstore.service.RefundProcessor;
import com.fpoly.marcusstore.service.RefundService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminRefundControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private RefundService refundService;

    @MockBean
    private RefundProcessor refundProcessor;

    @Test
    @WithMockUser(authorities = { "ROLE_STAFF", "ORDER_VIEW", "ORDER_UPDATE" })
    void staffCanViewRefundList() throws Exception {
        when(refundService.getRefunds(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/admin/refunds"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = { "ROLE_STAFF", "ORDER_VIEW", "ORDER_UPDATE" })
    void staffCannotApproveRefundEvenWithOrderUpdatePermission() throws Exception {
        mockMvc.perform(post("/api/admin/refunds/1/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = { "ROLE_ADMIN", "ORDER_VIEW" })
    void adminCanApproveRefund() throws Exception {
        when(refundProcessor.approve(1L)).thenReturn(RefundResponse.builder()
                .refundId(1L)
                .status("SUBMITTING")
                .build());

        mockMvc.perform(post("/api/admin/refunds/1/approve"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = { "ROLE_ADMIN", "ORDER_VIEW" })
    void rejectsNonPositiveRefundIdBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/admin/refunds/0/approve"))
                .andExpect(status().isBadRequest());
    }
}
