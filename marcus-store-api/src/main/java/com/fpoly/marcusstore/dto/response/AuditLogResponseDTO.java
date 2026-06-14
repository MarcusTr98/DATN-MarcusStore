package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

//DTO trả dữ liệu audit log về cho client
 
@Data
@Builder
public class AuditLogResponseDTO {

    private Integer logId;
    private String actionType;
    private String tableName;
    private String description;
    private String ipAddress;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime createdAt;
    
    // Thông tin người thực hiện
    private Integer userId;
    private String username;
    private String fullName;
}