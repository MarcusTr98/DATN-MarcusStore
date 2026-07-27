package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.BackupOverviewResponse;
import com.fpoly.marcusstore.service.BackupJobService;
import com.fpoly.marcusstore.service.BackupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminBackupControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BackupService backupService;

    @MockBean
    private BackupJobService backupJobService;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanReadBackupOverview() throws Exception {
        when(backupService.getOverview()).thenReturn(BackupOverviewResponse.builder()
                .databaseName("MarcusStoreDB")
                .tableCount(42)
                .totalRecords(4781)
                .tables(List.of())
                .build());

        mockMvc.perform(get("/api/admin/backups/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.databaseName").value("MarcusStoreDB"));
    }

    @Test
    @WithMockUser(username = "staff1", roles = "STAFF")
    void staffCannotReadBackupOverview() throws Exception {
        mockMvc.perform(get("/api/admin/backups/overview"))
                .andExpect(status().isForbidden());
    }
}
