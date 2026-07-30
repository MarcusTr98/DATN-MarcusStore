package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserNotificationResponse {
    private Integer id;
    private String type;
    private String title;
    private String message;
    private String referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
