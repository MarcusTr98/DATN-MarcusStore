package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder

public class AdminNotificationResponse {
    private Integer id;
    private String type;
    private String title;
    private String message;
    private String referenceId;
    private String category;
    private String icon;
    private String deepLink;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
