package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

//DTO trả dữ liệu audit log về cho client
 
@Data
@Builder
public class AuditLogResponseDTO {

    private Integer logId;
    private String actionType;
    private String tableName;
    private String description;
    private String ipAddress;
    private LocalDateTime createdAt;

    // Thông tin người thực hiện
    private Integer userId;
    private String username;
    private String fullName;
}